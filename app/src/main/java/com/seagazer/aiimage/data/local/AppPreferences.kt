package com.seagazer.aiimage.data.local

import android.content.Context
import com.seagazer.aiimage.domain.GalleryItem
import com.seagazer.aiimage.domain.GenerationSettings
import com.seagazer.aiimage.domain.ImageQuality
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object AppPreferences {

    private const val PREFS_NAME = "ethereal_prefs"
    private const val KEY_GALLERY = "gallery_json_v2"
    private const val KEY_RECYCLE_BIN = "recycle_bin_json_v1"
    private const val KEY_THEME_DARK = "theme_dark"
    private const val KEY_COMFY_URL = "comfy_base_url"
    private const val KEY_COMFY_CKPT = "comfy_checkpoint"
    private const val KEY_COMFY_CLIP = "comfy_clip"
    private const val KEY_COMFY_VAE = "comfy_vae"
    private const val KEY_SIMULATE_FAIL = "simulate_failure"
    private const val KEY_GENERATION_SETTINGS = "generation_settings_v1"
    private const val KEY_CREATE_PROMPT = "create_prompt_v1"
    private const val KEY_SELECTED_STYLES = "selected_styles_v1"
    private const val KEY_DAILY_GEN_DATE = "daily_gen_date_v1"
    private const val KEY_DAILY_GEN_COUNT = "daily_gen_count_v1"
    private const val KEY_PRIVATE_SPACE = "private_space_json_v1"
    private const val KEY_PRIVATE_PASSWORD = "private_space_password"

    /** TODO: Max ComfyUI generation requests per calendar day (each request counts once). */
    const val MAX_DAILY_GENERATIONS = 500

    private val dailyGenLock = Any()

    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadGallery(ctx: Context): List<GalleryItem> {
        val raw = sp(ctx).getString(KEY_GALLERY, null) ?: return emptyList()
        return runCatching { galleryFromJson(raw) }.getOrDefault(emptyList())
    }

    fun saveGallery(ctx: Context, items: List<GalleryItem>) {
        sp(ctx).edit().putString(KEY_GALLERY, galleryToJson(items)).apply()
    }

    fun loadRecycleBin(ctx: Context): List<GalleryItem> {
        val raw = sp(ctx).getString(KEY_RECYCLE_BIN, null) ?: return emptyList()
        return runCatching { galleryFromJson(raw) }.getOrDefault(emptyList())
    }

    fun saveRecycleBin(ctx: Context, items: List<GalleryItem>) {
        sp(ctx).edit().putString(KEY_RECYCLE_BIN, galleryToJson(items)).apply()
    }

    /** Default true: UX targets dark Lumia canvas (#10131a); user can switch to light in Settings. */
    fun loadThemeDark(ctx: Context) = sp(ctx).getBoolean(KEY_THEME_DARK, true)

    fun saveThemeDark(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean(KEY_THEME_DARK, value).apply()
    }

    fun loadComfyUrl(ctx: Context, defaultUrl: String) =
        sp(ctx).getString(KEY_COMFY_URL, null) ?: defaultUrl

    fun saveComfyUrl(ctx: Context, value: String) {
        sp(ctx).edit().putString(KEY_COMFY_URL, value).apply()
    }

    fun saveComfyModels(ctx: Context, ckpt: String, clip: String, vae: String) {
        sp(ctx).edit()
            .putString(KEY_COMFY_CKPT, ckpt)
            .putString(KEY_COMFY_CLIP, clip)
            .putString(KEY_COMFY_VAE, vae)
            .apply()
    }

    fun loadComfyCheckpoint(ctx: Context) = sp(ctx).getString(KEY_COMFY_CKPT, "") ?: ""
    fun loadComfyClip(ctx: Context) = sp(ctx).getString(KEY_COMFY_CLIP, "") ?: ""
    fun loadComfyVae(ctx: Context) = sp(ctx).getString(KEY_COMFY_VAE, "") ?: ""

    fun loadSimulateFailure(ctx: Context) = sp(ctx).getBoolean(KEY_SIMULATE_FAIL, false)

    fun saveSimulateFailure(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean(KEY_SIMULATE_FAIL, value).apply()
    }

    fun saveGenerationSettings(ctx: Context, s: GenerationSettings) {
        val o = JSONObject().apply {
            put("width", s.width)
            put("height", s.height)
            put("quality", s.quality.name)
            put("sampler", s.sampler)
            put("useRandomSeed", s.useRandomSeed)
            put("fixedSeed", s.fixedSeed)
        }
        sp(ctx).edit().putString(KEY_GENERATION_SETTINGS, o.toString()).apply()
    }

    fun loadGenerationSettings(ctx: Context): GenerationSettings? {
        val raw = sp(ctx).getString(KEY_GENERATION_SETTINGS, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val qName = o.optString("quality", ImageQuality.Med.name)
            val q = ImageQuality.entries.find { it.name == qName } ?: ImageQuality.Med
            GenerationSettings(
                width = o.optInt("width", 512),
                height = o.optInt("height", 512),
                quality = q,
                sampler = o.optString("sampler", "Euler Ancestral"),
                useRandomSeed = o.optBoolean("useRandomSeed", false),
                fixedSeed = o.optLong("fixedSeed", GenerationSettings.DEFAULT_PROFILE_SEED),
            )
        }.getOrNull()
    }

    fun saveCreatePrompt(ctx: Context, prompt: String) {
        sp(ctx).edit().putString(KEY_CREATE_PROMPT, prompt).apply()
    }

    fun loadCreatePrompt(ctx: Context) = sp(ctx).getString(KEY_CREATE_PROMPT, "") ?: ""

    fun saveSelectedStyles(ctx: Context, styles: Set<String>) {
        val arr = JSONArray()
        styles.sorted().forEach { arr.put(it) }
        sp(ctx).edit().putString(KEY_SELECTED_STYLES, arr.toString()).apply()
    }

    fun loadSelectedStyles(ctx: Context): Set<String> {
        val raw = sp(ctx).getString(KEY_SELECTED_STYLES, null) ?: return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        }.getOrDefault(emptySet())
    }

    fun getTodayGenerationCount(ctx: Context): Int {
        val prefs = sp(ctx)
        val today = LocalDate.now().toString()
        val storedDate = prefs.getString(KEY_DAILY_GEN_DATE, null)
        return if (storedDate == today) prefs.getInt(KEY_DAILY_GEN_COUNT, 0) else 0
    }

    /**
     * Reserves one generation slot for today if under [MAX_DAILY_GENERATIONS].
     * @return false if today’s limit is already reached (no increment).
     */
    fun tryBeginGeneration(ctx: Context): Boolean = synchronized(dailyGenLock) {
        val prefs = sp(ctx)
        val today = LocalDate.now().toString()
        val storedDate = prefs.getString(KEY_DAILY_GEN_DATE, null)
        val count = if (storedDate == today) prefs.getInt(KEY_DAILY_GEN_COUNT, 0) else 0
        if (count >= MAX_DAILY_GENERATIONS) return false
        prefs.edit()
            .putString(KEY_DAILY_GEN_DATE, today)
            .putInt(KEY_DAILY_GEN_COUNT, count + 1)
            .apply()
        true
    }

    fun loadPrivateSpace(ctx: Context): List<GalleryItem> {
        val raw = sp(ctx).getString(KEY_PRIVATE_SPACE, null) ?: return emptyList()
        return runCatching { galleryFromJson(raw) }.getOrDefault(emptyList())
    }

    fun savePrivateSpace(ctx: Context, items: List<GalleryItem>) {
        sp(ctx).edit().putString(KEY_PRIVATE_SPACE, galleryToJson(items)).apply()
    }

    fun loadPrivatePassword(ctx: Context): String? =
        sp(ctx).getString(KEY_PRIVATE_PASSWORD, null)

    fun savePrivatePassword(ctx: Context, password: String) {
        sp(ctx).edit().putString(KEY_PRIVATE_PASSWORD, password).apply()
    }

    fun isPrivatePasswordSet(ctx: Context): Boolean =
        !sp(ctx).getString(KEY_PRIVATE_PASSWORD, null).isNullOrEmpty()

    private fun galleryToJson(items: List<GalleryItem>): String {
        val arr = JSONArray()
        items.forEach { arr.put(galleryItemToJson(it)) }
        return arr.toString()
    }

    private fun galleryFromJson(raw: String): List<GalleryItem> {
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(parseGalleryItem(o))
            }
        }
    }

    private fun galleryItemToJson(g: GalleryItem): JSONObject = JSONObject().apply {
        put("id", g.id)
        put("imageUrl", g.imageUrl)
        if (g.caption != null) put("caption", g.caption) else put("caption", JSONObject.NULL)
        put("spanLarge", g.spanLarge)
        put("spanWide", g.spanWide)
        put("modelLabel", g.modelLabel)
        put("seed", g.seed)
        put("sampling", g.sampling)
        put("steps", g.steps)
        put("prompt", g.prompt)
        put("width", g.width)
        put("height", g.height)
    }

    private fun parseGalleryItem(o: JSONObject): GalleryItem {
        val cap = when {
            !o.has("caption") || o.isNull("caption") -> null
            else -> o.optString("caption").ifBlank { null }
        }
        return GalleryItem(
            id = o.optString("id"),
            imageUrl = o.optString("imageUrl"),
            caption = cap,
            spanLarge = o.optBoolean("spanLarge", true),
            spanWide = o.optBoolean("spanWide", false),
            modelLabel = o.optString("modelLabel", ""),
            seed = o.optString("seed", ""),
            sampling = o.optString("sampling", ""),
            steps = o.optString("steps", ""),
            prompt = o.optString("prompt", ""),
            width = o.optInt("width", 0),
            height = o.optInt("height", 0),
        )
    }
}
