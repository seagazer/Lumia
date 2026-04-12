package com.seagazer.aiimage.domain

enum class ImageQuality(val label: String) {
    Low("Low"),
    Med("Med"),
    High("High"),
    ;

    /** KSampler steps */
    fun toSamplerSteps(): Int = when (this) {
        Low -> 20
        Med -> 38
        High -> 56
    }
}

data class GenerationSettings(
    val width: Int = 512,
    val height: Int = 512,
    val quality: ImageQuality = ImageQuality.Med,
    val sampler: String = "Euler Ancestral",
    /**
     * When true, each generation uses a new random 32-bit seed.
     * When false, [fixedSeed] is sent to ComfyUI every time (reproducible runs).
     */
    val useRandomSeed: Boolean = false,
    /**
     * 32-bit unsigned seed (0…4294967295).
     * [DEFAULT_PROFILE_SEED] is derived from RTX 4070 Super + 32 GiB + Qwen-image-202512-fp8, balanced performance / quality-first.
     */
    val fixedSeed: Long = DEFAULT_PROFILE_SEED,
) {
    companion object {
        /** CRC32 of `RTX4070Super|32GiB|Qwen-image-202512-fp8|balancedPerf|qualityFirst` (UTF-8). */
        val DEFAULT_PROFILE_SEED: Long = 1_034_423_436L
    }
}

data class GalleryItem(
    val id: String,
    val imageUrl: String,
    val caption: String?,
    val spanLarge: Boolean = false,
    val spanWide: Boolean = false,
    /** Full metadata for opening [ResultDetail] after restart */
    val modelLabel: String = "",
    val seed: String = "",
    val sampling: String = "",
    val steps: String = "",
    val prompt: String = "",
    val width: Int = 0,
    val height: Int = 0,
)

data class ResultDetail(
    val id: String,
    val imageUrl: String,
    val prompt: String,
    val modelLabel: String,
    val seed: String,
    val sampling: String,
    val steps: String,
    val width: Int = 0,
    val height: Int = 0,
)
