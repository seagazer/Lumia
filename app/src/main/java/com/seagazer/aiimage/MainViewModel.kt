package com.seagazer.aiimage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seagazer.aiimage.data.comfy.ComfyApiClient
import com.seagazer.aiimage.data.comfy.ComfyApiException
import com.seagazer.aiimage.data.comfy.ComfyWorkflowPatcher
import com.seagazer.aiimage.data.local.AppPreferences
import com.seagazer.aiimage.util.AppGalleryStorage
import com.seagazer.aiimage.util.PublicAlbumSaver
import com.seagazer.aiimage.domain.GalleryItem
import com.seagazer.aiimage.domain.GenerationSettings
import com.seagazer.aiimage.domain.ResultDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.UUID
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _themeDark = MutableStateFlow(true)
    val themeDark: StateFlow<Boolean> = _themeDark.asStateFlow()

    private val _generationSettings = MutableStateFlow(GenerationSettings())
    val generationSettings: StateFlow<GenerationSettings> = _generationSettings.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _selectedStyles = MutableStateFlow(setOf<String>())
    val selectedStyles: StateFlow<Set<String>> = _selectedStyles.asStateFlow()

    private val _gallery = MutableStateFlow<List<GalleryItem>>(emptyList())
    val gallery: StateFlow<List<GalleryItem>> = _gallery.asStateFlow()

    private val _recycleBin = MutableStateFlow<List<GalleryItem>>(emptyList())
    val recycleBin: StateFlow<List<GalleryItem>> = _recycleBin.asStateFlow()

    private val _privateSpace = MutableStateFlow<List<GalleryItem>>(emptyList())
    val privateSpace: StateFlow<List<GalleryItem>> = _privateSpace.asStateFlow()

    /** True when the currently displayed [ResultDetail] was opened from the private space list. */
    private val _resultFromPrivateSpace = MutableStateFlow(false)
    val resultFromPrivateSpace: StateFlow<Boolean> = _resultFromPrivateSpace.asStateFlow()

    private val _resultDetail = MutableStateFlow<ResultDetail?>(null)
    val resultDetail: StateFlow<ResultDetail?> = _resultDetail.asStateFlow()

    /** When opening result: which tab should appear selected on bottom bar (create vs gallery). */
    private val _resultBottomTabGallery = MutableStateFlow(false)
    val resultBottomTabGallery: StateFlow<Boolean> = _resultBottomTabGallery.asStateFlow()

    /** True when [ResultDetail] was opened from the gallery grid (vs fresh generation). */
    private val _resultFromGallery = MutableStateFlow(false)
    val resultFromGallery: StateFlow<Boolean> = _resultFromGallery.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    /**0–100 from ComfyUI WebSocket `progress`; null until first event or when idle. */
    private val _generationProgressPercent = MutableStateFlow<Int?>(null)
    val generationProgressPercent: StateFlow<Int?> = _generationProgressPercent.asStateFlow()

    private var generationJob: Job? = null

    private val _showNetworkError = MutableStateFlow(false)
    val showNetworkError: StateFlow<Boolean> = _showNetworkError.asStateFlow()

    private val _networkErrorDetail = MutableStateFlow<String?>(null)
    /** Set when [showNetworkError] is shown: okhttp/socket message or invalid-URL text */
    val networkErrorDetail: StateFlow<String?> = _networkErrorDetail.asStateFlow()

    private val _showGenerationFailed = MutableStateFlow(false)
    val showGenerationFailed: StateFlow<Boolean> = _showGenerationFailed.asStateFlow()

    private val _generationFailedDetail = MutableStateFlow<String?>(null)
    /** ComfyUI [ComfyApiException] message (incl. validation details) when [showGenerationFailed] is shown */
    val generationFailedDetail: StateFlow<String?> = _generationFailedDetail.asStateFlow()

    /** Demo: force failure path from settings. */
    private val _simulateFailure = MutableStateFlow(false)
    val simulateFailure: StateFlow<Boolean> = _simulateFailure.asStateFlow()

    private val _dailyGenerationsUsed = MutableStateFlow(0)
    val dailyGenerationsUsed: StateFlow<Int> = _dailyGenerationsUsed.asStateFlow()

    val dailyGenerationsCap: Int get() = AppPreferences.MAX_DAILY_GENERATIONS

    private val _dailyLimitExceeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dailyLimitExceeded: SharedFlow<Unit> = _dailyLimitExceeded.asSharedFlow()

    /** Debug: emulator → host [BuildConfig.DEFAULT_COMFY_BASE_URL]; release: change in Settings */
    private val _comfyBaseUrl = MutableStateFlow(BuildConfig.DEFAULT_COMFY_BASE_URL)
    val comfyBaseUrl: StateFlow<String> = _comfyBaseUrl.asStateFlow()

    /**
     * SD1.5 workflows: filename in `models/checkpoints`.
     * UNET / Qwen workflows (bundled fp8 template): filename in `models/diffusion_models` (UNETLoader).
     */
    private val _comfyCheckpointFile = MutableStateFlow("")
    val comfyCheckpointFile: StateFlow<String> = _comfyCheckpointFile.asStateFlow()

    private val _comfyClipFile = MutableStateFlow("")
    val comfyClipFile: StateFlow<String> = _comfyClipFile.asStateFlow()

    private val _comfyVaeFile = MutableStateFlow("")
    val comfyVaeFile: StateFlow<String> = _comfyVaeFile.asStateFlow()

    /** API JSON aligned with workspace `fp8-flow-api.json` (Qwen UNET + CLIP + VAE txt2img). */
    private val defaultWorkflowTemplate: String by lazy {
        getApplication<Application>().resources.openRawResource(R.raw.comfy_fp8_flow_default)
            .bufferedReader()
            .use { it.readText() }
    }

    init {
        val app = getApplication<Application>()
        _themeDark.value = AppPreferences.loadThemeDark(app)
        _comfyBaseUrl.value = AppPreferences.loadComfyUrl(app, BuildConfig.DEFAULT_COMFY_BASE_URL)
        _comfyCheckpointFile.value = AppPreferences.loadComfyCheckpoint(app)
        _comfyClipFile.value = AppPreferences.loadComfyClip(app)
        _comfyVaeFile.value = AppPreferences.loadComfyVae(app)
        _simulateFailure.value = AppPreferences.loadSimulateFailure(app)
        _gallery.value = filterReachableGallery(AppPreferences.loadGallery(app))
        _recycleBin.value = filterReachableGallery(AppPreferences.loadRecycleBin(app))
        _privateSpace.value = filterReachableGallery(AppPreferences.loadPrivateSpace(app))
        AppPreferences.loadGenerationSettings(app)?.let { _generationSettings.value = it }
        _prompt.value = AppPreferences.loadCreatePrompt(app)
        _selectedStyles.value = AppPreferences.loadSelectedStyles(app)
        _dailyGenerationsUsed.value = AppPreferences.getTodayGenerationCount(app)
    }

    fun setThemeDark(value: Boolean) {
        _themeDark.value = value
        AppPreferences.saveThemeDark(getApplication(), value)
    }

    fun setPrompt(value: String) {
        _prompt.value = value
        AppPreferences.saveCreatePrompt(getApplication(), value)
    }

    fun setGenerationSettings(value: GenerationSettings) {
        _generationSettings.value = value
        AppPreferences.saveGenerationSettings(getApplication(), value)
    }

    fun toggleStyle(style: String) {
        _selectedStyles.update { cur ->
            val next = if (style in cur) cur - style else cur + style
            AppPreferences.saveSelectedStyles(getApplication(), next)
            next
        }
    }

    fun setComfyBaseUrl(value: String) {
        _comfyBaseUrl.value = value
        AppPreferences.saveComfyUrl(getApplication(), value)
    }

    fun setComfyCheckpointFile(value: String) {
        _comfyCheckpointFile.value = value
        persistComfyModels()
    }

    fun setComfyClipFile(value: String) {
        _comfyClipFile.value = value
        persistComfyModels()
    }

    fun setComfyVaeFile(value: String) {
        _comfyVaeFile.value = value
        persistComfyModels()
    }

    fun setSimulateFailure(value: Boolean) {
        _simulateFailure.value = value
        AppPreferences.saveSimulateFailure(getApplication(), value)
    }

    fun refreshDailyGenerationsFromPrefs() {
        _dailyGenerationsUsed.value = AppPreferences.getTodayGenerationCount(getApplication())
    }

    private fun persistComfyModels() {
        AppPreferences.saveComfyModels(
            getApplication(),
            _comfyCheckpointFile.value,
            _comfyClipFile.value,
            _comfyVaeFile.value,
        )
    }

    private fun persistGallery() {
        AppPreferences.saveGallery(getApplication(), _gallery.value)
    }

    private fun persistRecycleBin() {
        AppPreferences.saveRecycleBin(getApplication(), _recycleBin.value)
    }

    private fun persistPrivateSpace() {
        AppPreferences.savePrivateSpace(getApplication(), _privateSpace.value)
    }

    private fun filterReachableGallery(items: List<GalleryItem>): List<GalleryItem> =
        items.filter { item ->
            when {
                item.imageUrl.startsWith("file:") ->
                    runCatching { File(URI.create(item.imageUrl)).isFile }.getOrDefault(false)
                item.imageUrl.startsWith("http://") || item.imageUrl.startsWith("https://") -> true
                else -> false
            }
        }

    /**
     * Copies the result image (local file from Comfy download) into the public Pictures album.
     */
    fun saveResultToPublicAlbum(imageUrl: String): Boolean {
        if (!imageUrl.startsWith("file:")) return false
        return runCatching {
            val file = File(URI.create(imageUrl))
            PublicAlbumSaver.savePngFromFile(getApplication(), file) != null
        }.getOrDefault(false)
    }

    /** Deletes persisted gallery images, JSON, and temp Comfy cache files */
    fun clearAppCaches() {
        val app = getApplication<Application>()
        AppGalleryStorage.directory(app).deleteRecursively()
        File(app.cacheDir, "comfy_out").deleteRecursively()
        _gallery.value = emptyList()
        _recycleBin.value = emptyList()
        _privateSpace.value = emptyList()
        AppPreferences.saveGallery(app, emptyList())
        AppPreferences.saveRecycleBin(app, emptyList())
        AppPreferences.savePrivateSpace(app, emptyList())
    }

    fun showNetworkError(value: Boolean) {
        _showNetworkError.value = value
        if (!value) _networkErrorDetail.value = null
    }

    fun showGenerationFailed(value: Boolean) {
        _showGenerationFailed.value = value
        if (!value) _generationFailedDetail.value = null
    }

    fun openResultFromGallery(item: GalleryItem) {
        _resultFromGallery.value = true
        _resultFromPrivateSpace.value = false
        _resultBottomTabGallery.value = true
        val prompt = item.prompt.ifBlank { item.caption ?: "" }
        _resultDetail.value = ResultDetail(
            id = item.id,
            imageUrl = item.imageUrl,
            prompt = prompt,
            modelLabel = item.modelLabel.ifBlank { "ComfyUI" },
            seed = item.seed.ifBlank { "—" },
            sampling = item.sampling.ifBlank { "—" },
            steps = item.steps.ifBlank { "—" },
            width = item.width,
            height = item.height,
        )
    }

    fun openResultFromGeneration(detail: ResultDetail) {
        _resultFromGallery.value = false
        _resultFromPrivateSpace.value = false
        _resultBottomTabGallery.value = true
        _resultDetail.value = detail
    }

    fun clearResult() {
        _resultDetail.value = null
    }

    fun cancelGeneration() {
        val url = _comfyBaseUrl.value.trim()
        if (url.isNotEmpty() && !_simulateFailure.value) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { ComfyApiClient(url).interrupt() }
                    .onFailure { Log.w(TAG, "ComfyUI interrupt failed", it) }
            }
        }
        generationJob?.cancel()
        generationJob = null
        _generating.value = false
        _generationProgressPercent.value = null
    }

    /**
     * Removes the item from the gallery and moves the image file into app trash (if local).
     * Metadata is stored in the recycle bin until restored or permanently deleted.
     */
    fun moveGalleryItemToRecycleBin(id: String) {
        val item = _gallery.value.find { it.id == id } ?: return
        val app = getApplication<Application>()
        val moved = moveLocalGalleryFileToTrash(app, item)
        _gallery.update { list -> list.filterNot { it.id == id } }
        persistGallery()
        _recycleBin.update { list -> listOf(moved) + list.filterNot { it.id == id } }
        persistRecycleBin()
        if (_resultDetail.value?.id == id) clearResult()
    }

    fun restoreRecycleBinItem(id: String) {
        val item = _recycleBin.value.find { it.id == id } ?: return
        val app = getApplication<Application>()
        val restored = moveLocalFileFromTrashToGallery(app, item)
        _recycleBin.update { list -> list.filterNot { it.id == id } }
        persistRecycleBin()
        _gallery.update { list -> listOf(restored) + list.filterNot { it.id == id } }
        persistGallery()
    }

    fun isPrivatePasswordSet(): Boolean =
        AppPreferences.isPrivatePasswordSet(getApplication())

    fun verifyPrivatePassword(password: String): Boolean =
        AppPreferences.loadPrivatePassword(getApplication()) == password

    fun setPrivatePassword(password: String) {
        AppPreferences.savePrivatePassword(getApplication(), password)
    }

    fun openResultFromPrivateSpace(item: GalleryItem) {
        _resultFromGallery.value = true
        _resultFromPrivateSpace.value = true
        _resultBottomTabGallery.value = true
        val prompt = item.prompt.ifBlank { item.caption ?: "" }
        _resultDetail.value = ResultDetail(
            id = item.id,
            imageUrl = item.imageUrl,
            prompt = prompt,
            modelLabel = item.modelLabel.ifBlank { "ComfyUI" },
            seed = item.seed.ifBlank { "—" },
            sampling = item.sampling.ifBlank { "—" },
            steps = item.steps.ifBlank { "—" },
            width = item.width,
            height = item.height,
        )
    }

    fun moveGalleryItemToPrivateSpace(id: String) {
        val item = _gallery.value.find { it.id == id } ?: return
        val app = getApplication<Application>()
        val moved = moveLocalFileToPrivate(app, item)
        _gallery.update { list -> list.filterNot { it.id == id } }
        persistGallery()
        _privateSpace.update { list -> listOf(moved) + list.filterNot { it.id == id } }
        persistPrivateSpace()
        if (_resultDetail.value?.id == id) clearResult()
    }

    fun restorePrivateSpaceItemToGallery(id: String) {
        val item = _privateSpace.value.find { it.id == id } ?: return
        val app = getApplication<Application>()
        val restored = moveLocalFileFromPrivateToGallery(app, item)
        _privateSpace.update { list -> list.filterNot { it.id == id } }
        persistPrivateSpace()
        _gallery.update { list -> listOf(restored) + list.filterNot { it.id == id } }
        persistGallery()
        if (_resultDetail.value?.id == id) clearResult()
    }

    private fun moveLocalFileToPrivate(app: Application, item: GalleryItem): GalleryItem {
        if (!item.imageUrl.startsWith("file:")) return item
        val src = runCatching { File(URI.create(item.imageUrl)) }.getOrNull() ?: return item
        if (!src.isFile) return item
        val privateDir = AppGalleryStorage.ensurePrivateDirectory(app)
        val dest = File(privateDir, "${item.id}.png")
        if (dest.exists()) dest.delete()
        if (!src.renameTo(dest)) {
            runCatching {
                src.copyTo(dest, overwrite = true)
                src.delete()
            }
        }
        return item.copy(imageUrl = dest.toURI().toString())
    }

    private fun moveLocalFileFromPrivateToGallery(app: Application, item: GalleryItem): GalleryItem {
        if (!item.imageUrl.startsWith("file:")) return item
        val src = runCatching { File(URI.create(item.imageUrl)) }.getOrNull() ?: return item
        if (!src.isFile) return item
        val galleryDir = AppGalleryStorage.ensureDirectory(app)
        val dest = File(galleryDir, "${item.id}.png")
        if (dest.exists()) dest.delete()
        if (!src.renameTo(dest)) {
            runCatching {
                src.copyTo(dest, overwrite = true)
                src.delete()
            }
        }
        return item.copy(imageUrl = dest.toURI().toString())
    }

    fun permanentlyDeleteRecycleBinItem(id: String) {
        val item = _recycleBin.value.find { it.id == id } ?: return
        if (item.imageUrl.startsWith("file:")) {
            runCatching {
                val f = File(URI.create(item.imageUrl))
                if (f.isFile) f.delete()
            }
        }
        _recycleBin.update { list -> list.filterNot { it.id == id } }
        persistRecycleBin()
    }

    private fun moveLocalGalleryFileToTrash(app: Application, item: GalleryItem): GalleryItem {
        if (!item.imageUrl.startsWith("file:")) return item
        val src = runCatching { File(URI.create(item.imageUrl)) }.getOrNull() ?: return item
        if (!src.isFile) return item
        val trashDir = AppGalleryStorage.ensureTrashDirectory(app)
        val dest = File(trashDir, "${item.id}.png")
        if (dest.exists()) dest.delete()
        if (!src.renameTo(dest)) {
            runCatching {
                src.copyTo(dest, overwrite = true)
                src.delete()
            }
        }
        return item.copy(imageUrl = dest.toURI().toString())
    }

    private fun moveLocalFileFromTrashToGallery(app: Application, item: GalleryItem): GalleryItem {
        if (!item.imageUrl.startsWith("file:")) return item
        val src = runCatching { File(URI.create(item.imageUrl)) }.getOrNull() ?: return item
        if (!src.isFile) return item
        val galleryDir = AppGalleryStorage.ensureDirectory(app)
        val dest = File(galleryDir, "${item.id}.png")
        if (dest.exists()) dest.delete()
        if (!src.renameTo(dest)) {
            runCatching {
                src.copyTo(dest, overwrite = true)
                src.delete()
            }
        }
        return item.copy(imageUrl = dest.toURI().toString())
    }

    fun generateArt(onDone: (ResultDetail?) -> Unit) {
        val text = _prompt.value.trim()
        if (text.isEmpty() || _generating.value) return
        val app = getApplication<Application>()
        if (!AppPreferences.tryBeginGeneration(app)) {
            _dailyLimitExceeded.tryEmit(Unit)
            return
        }
        _dailyGenerationsUsed.value = AppPreferences.getTodayGenerationCount(app)
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                _generating.value = true
                _generationProgressPercent.value = null
                when {
                    _simulateFailure.value -> {
                        delay(1600)
                        if (!isActive) return@launch
                        _generationFailedDetail.value = null
                        _showGenerationFailed.value = true
                        onDone(null)
                    }

                    else -> {
                        val detail = runCatching {
                            generateViaComfyUi(text) { pct ->
                                if (isActive) _generationProgressPercent.value = pct
                            }
                        }.fold(
                            onSuccess = { it },
                            onFailure = { e ->
                                if (!isActive) return@fold null
                                Log.e(TAG, "generateArt failed", e)
                                when (e) {
                                    is IOException -> {
                                        _networkErrorDetail.value = e.message?.ifBlank { e.javaClass.simpleName }
                                            ?: e.javaClass.simpleName
                                        _showNetworkError.value = true
                                    }
                                    is ComfyApiException -> {
                                        _generationFailedDetail.value = e.message
                                        _showGenerationFailed.value = true
                                    }
                                    is IllegalArgumentException -> {
                                        _networkErrorDetail.value = e.message ?: e.javaClass.simpleName
                                        _showNetworkError.value = true
                                    }
                                    else -> {
                                        _generationFailedDetail.value = e.message
                                        _showGenerationFailed.value = true
                                    }
                                }
                                null
                            },
                        )
                        if (!isActive) return@launch
                        if (detail != null) {
                            _gallery.update { list ->
                                val newItem = GalleryItem(
                                    id = detail.id,
                                    imageUrl = detail.imageUrl,
                                    caption = text.take(48),
                                    spanLarge = true,
                                    spanWide = false,
                                    modelLabel = detail.modelLabel,
                                    seed = detail.seed,
                                    sampling = detail.sampling,
                                    steps = detail.steps,
                                    prompt = detail.prompt,
                                    width = detail.width,
                                    height = detail.height,
                                )
                                listOf(newItem) + list
                            }
                            persistGallery()
                        }
                        onDone(detail)
                    }
                }
            } finally {
                _generating.value = false
                _generationProgressPercent.value = null
                generationJob = null
            }
        }
    }

    private suspend fun generateViaComfyUi(
        text: String,
        onProgress: (Int) -> Unit,
    ): ResultDetail {
        val app = getApplication<Application>()
        val settings = _generationSettings.value
        val finalPrompt = buildFinalPrompt(text)
        val seed = if (settings.useRandomSeed) {
            Random.nextLong(0, 1L shl 32)
        } else {
            settings.fixedSeed and 0xffff_ffffL
        }
        val primary = _comfyCheckpointFile.value.trim().ifEmpty { null }
        val clip = _comfyClipFile.value.trim().ifEmpty { null }
        val vae = _comfyVaeFile.value.trim().ifEmpty { null }
        val graph = ComfyWorkflowPatcher.buildPromptGraph(
            defaultWorkflowTemplate,
            finalPrompt,
            settings,
            seed,
            primary,
            clip,
            vae,
        )
        val baseUrl = _comfyBaseUrl.value.trim()
        return withContext(Dispatchers.IO) {
            coroutineScope {
                val client = ComfyApiClient(baseUrl)
                val clientId = UUID.randomUUID().toString()
                val promptId = client.submitPrompt(graph, clientId)
                val progressJob = launch {
                    client.runProgressWebSocket(clientId, promptId, onProgress)
                }
                try {
                    val outRef = client.waitForFirstOutput(promptId)
                    onProgress(100)
                    val bytes = client.downloadView(outRef.filename, outRef.subfolder, outRef.type)
                    val id = UUID.randomUUID().toString()
                    val galleryDir = AppGalleryStorage.ensureDirectory(app)
                    val file = File(galleryDir, "$id.png")
                    file.writeBytes(bytes)
                    val uriStr = file.toURI().toString()
                    ResultDetail(
                        id = id,
                        imageUrl = uriStr,
                        prompt = finalPrompt,
                        modelLabel = ComfyWorkflowPatcher.effectiveCheckpointLabel(defaultWorkflowTemplate, primary),
                        seed = seed.toString(),
                        sampling = settings.sampler,
                        steps = settings.quality.toSamplerSteps().toString(),
                        width = settings.width,
                        height = settings.height,
                    )
                } finally {
                    progressJob.cancel()
                    progressJob.join()
                }
            }
        }
    }

    private fun buildFinalPrompt(text: String): String {
        val suffix = _selectedStyles.value.joinToString(", ")
        return if (suffix.isNotEmpty()) "$text, $suffix" else text
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
