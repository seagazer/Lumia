package com.seagazer.aiimage.data.comfy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.min

class ComfyApiException(val statusCode: Int, message: String) : Exception(message)

class ComfyApiClient(baseUrl: String) {

    private val apiRoot: HttpUrl = baseUrl.trimEnd('/').toHttpUrlOrNull()
        ?: throw IllegalArgumentException("Invalid ComfyUI base URL")

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(300, TimeUnit.SECONDS)
        .build()

    private fun urlSegments(vararg segments: String): HttpUrl {
        val b = apiRoot.newBuilder()
        segments.forEach { b.addPathSegment(it) }
        return b.build()
    }

    /**
     * Stops the current prompt on the ComfyUI server (same as UI "Interrupt").
     * POST `/interrupt` — see ComfyUI built-in HTTP routes.
     */
    fun interrupt() {
        val req = Request.Builder()
            .url(urlSegments("interrupt"))
            .post(ByteArray(0).toRequestBody(null))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val t = resp.body?.string().orEmpty()
                throw parseComfyError(resp.code, t)
            }
        }
    }

    fun submitPrompt(promptGraph: JSONObject, clientId: String): String {
        val body = JSONObject()
            .put("prompt", promptGraph)
            .put("client_id", clientId)
            .toString()
            .toRequestBody(JSON_MEDIA)

        val req = Request.Builder().url(urlSegments("prompt")).post(body).build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw parseComfyError(resp.code, text)
            }
            val json = runCatching { JSONObject(text) }.getOrNull()
                ?: throw ComfyApiException(resp.code, text.ifBlank { "Empty response" })
            if (json.has("error")) {
                throw comfyErrorFromJson(json, resp.code)
            }
            return json.optString("prompt_id").ifBlank {
                throw ComfyApiException(resp.code, "Missing prompt_id: $text")
            }
        }
    }

    fun fetchHistorySnapshot(promptId: String): JSONObject? {
        val req = Request.Builder().url(urlSegments("history", promptId)).get().build()
        client.newCall(req).execute().use { resp ->
            if (resp.code == 404) return null
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw parseComfyError(resp.code, text)
            }
            if (text.isBlank()) return null
            return JSONObject(text)
        }
    }

    private fun webSocketRequest(clientId: String): Request {
        // OkHttp [HttpUrl] only allows http/https; ws/wss must be a string for [Request.Builder.url].
        val httpPath = apiRoot.newBuilder()
            .addPathSegment("ws")
            .addQueryParameter("clientId", clientId)
            .build()
        val urlString = when (apiRoot.scheme) {
            "https" -> "wss://" + httpPath.toString().removePrefix("https://")
            else -> "ws://" + httpPath.toString().removePrefix("http://")
        }
        return Request.Builder().url(urlString).build()
    }

    /**
     * Subscribes to ComfyUI `/ws` until the coroutine is cancelled.
     * Reconnects after each close or failure so progress resumes when returning from background
     * (Android often tears down idle sockets while the app is not in the foreground).
     */
    suspend fun runProgressWebSocket(
        clientId: String,
        promptId: String,
        onProgress: (Int) -> Unit,
    ) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val finished = AtomicBoolean(false)
            suspendCancellableCoroutine<Unit> { cont ->
                val endConnection: () -> Unit = {
                    if (finished.compareAndSet(false, true) && cont.isActive) {
                        cont.resume(Unit)
                    }
                }
                val listener = object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        parseProgressJsonPercent(text, promptId)?.let(onProgress)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        endConnection()
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        endConnection()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        endConnection()
                    }
                }
                val ws = client.newWebSocket(webSocketRequest(clientId), listener)
                cont.invokeOnCancellation { ws.cancel() }
            }
            delay(280L)
        }
    }

    fun downloadView(filename: String, subfolder: String, type: String): ByteArray {
        val b = apiRoot.newBuilder()
            .addPathSegment("view")
            .addQueryParameter("filename", filename)
            .addQueryParameter("subfolder", subfolder)
            .addQueryParameter("type", type)
            .build()
        val req = Request.Builder().url(b).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val t = resp.body?.string().orEmpty()
                throw parseComfyError(resp.code, t)
            }
            return resp.body?.bytes() ?: ByteArray(0)
        }
    }

    /**
     * Poll history until an image appears or [maxWaitMs] elapsed.
     */
    suspend fun waitForFirstOutput(
        promptId: String,
        pollIntervalMs: Long = 800L,
        maxWaitMs: Long = 360_000L,
    ): ComfyOutputImage = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + maxWaitMs
        var last: JSONObject? = null
        while (System.currentTimeMillis() < deadline) {
            last = runCatching { fetchHistorySnapshot(promptId) }.getOrNull()
            if (last != null) {
                val img = extractFirstImage(last)
                if (img != null) return@withContext img
            }
            delay(pollIntervalMs)
        }
        throw ComfyApiException(
            504,
            "Timeout waiting for ComfyUI output. Last: ${last?.toString()?.let { it.take(min(it.length, 500)) }}",
        )
    }

    private fun parseProgressJsonPercent(jsonText: String, promptId: String): Int? {
        val o = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
        if (o.optString("type") != "progress") return null
        val data = o.optJSONObject("data") ?: return null
        val pid = data.optString("prompt_id", "")
        if (pid.isNotEmpty() && pid != promptId) return null
        val max = data.optInt("max", 0)
        if (max <= 0) return null
        val value = data.optInt("value", 0)
        return ((value * 100L / max).toInt().coerceIn(0, 100))
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        /**
         * ComfyUI `GET /history/{prompt_id}` returns `{ "<prompt_id>": { "outputs": { ... }, ... } }`,
         * not the inner object at the root. Unwrap so [outputs] is found.
         */
        fun historyEntryFromResponse(json: JSONObject): JSONObject? {
            if (json.has("outputs")) return json
            val it = json.keys()
            while (it.hasNext()) {
                val child = json.optJSONObject(it.next()) ?: continue
                if (child.has("outputs")) return child
            }
            return null
        }

        fun extractFirstImage(historyResponse: JSONObject): ComfyOutputImage? {
            val entry = historyEntryFromResponse(historyResponse) ?: return null
            val outputs = entry.optJSONObject("outputs") ?: return null
            val keys = outputs.keys()
            while (keys.hasNext()) {
                val nodeId = keys.next()
                val nodeOut = outputs.optJSONObject(nodeId) ?: continue
                val images = nodeOut.optJSONArray("images") ?: continue
                if (images.length() == 0) continue
                val first = images.getJSONObject(0)
                val fn = first.optString("filename")
                if (fn.isBlank()) continue
                return ComfyOutputImage(
                    filename = fn,
                    subfolder = first.optString("subfolder"),
                    type = first.optString("type", "output"),
                )
            }
            return null
        }

        private fun parseComfyError(code: Int, body: String): Exception {
            val j = runCatching { JSONObject(body) }.getOrNull()
            if (j != null && j.has("error")) return comfyErrorFromJson(j, code)
            return ComfyApiException(code, body.ifBlank { "HTTP $code" })
        }

        private fun comfyErrorFromJson(json: JSONObject, httpCode: Int): ComfyApiException {
            val err = json.optJSONObject("error")
            val msg = err?.optString("message")?.trim()?.ifEmpty { null }
                ?: err?.optString("type")?.trim()?.ifEmpty { null }
                ?: "ComfyUI error"
            val details = err?.optString("details")?.trim().orEmpty()
            val parts = ArrayList<String>(3)
            parts.add(msg)
            if (details.isNotEmpty()) parts.add(details)
            formatNodeErrors(json.optJSONObject("node_errors"))?.let { parts.add(it) }
            return ComfyApiException(httpCode, parts.joinToString("\n"))
        }

        private fun formatNodeErrors(nodeErrors: JSONObject?): String? {
            if (nodeErrors == null || nodeErrors.length() == 0) return null
            val lines = mutableListOf<String>()
            val keys = nodeErrors_keys(nodeErrors)
            for (nodeId in keys) {
                val obj = nodeErrors.optJSONObject(nodeId) ?: continue
                val classType = obj.optString("class_type")
                val label = if (classType.isNotEmpty()) "Node $nodeId ($classType)" else "Node $nodeId"
                val errs = obj.optJSONArray("errors") ?: continue
                for (i in 0 until errs.length()) {
                    val e = errs.optJSONObject(i) ?: continue
                    val m = e.optString("message")
                    val d = e.optString("details").trim()
                    lines += buildString {
                        append(label).append(": ").append(m)
                        if (d.isNotEmpty()) append(" — ").append(d)
                    }
                }
            }
            return if (lines.isEmpty()) null else lines.joinToString("\n")
        }

        /** [JSONObject.keys] iterator order is not guaranteed stable across Android versions */
        private fun nodeErrors_keys(o: JSONObject): List<String> {
            val it = o.keys()
            val out = ArrayList<String>(o.length())
            while (it.hasNext()) out.add(it.next())
            return out
        }
    }
}

data class ComfyOutputImage(
    val filename: String,
    val subfolder: String,
    val type: String,
)
