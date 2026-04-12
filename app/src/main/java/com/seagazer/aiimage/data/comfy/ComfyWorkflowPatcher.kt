package com.seagazer.aiimage.data.comfy

import com.seagazer.aiimage.domain.GenerationSettings
import org.json.JSONObject

/**
 * Patches API-format workflow JSON. Supports:
 * - Legacy SD1.5 txt2img ([comfy_txt2img_default]): CheckpointLoaderSimple + EmptyLatentImage + KSampler on nodes 3–6,4.
 * - Qwen / UNET API flow (see [comfy_fp8_flow_default]): UNETLoader + CLIPLoader + VAELoader + EmptySD3LatentImage + KSampler on nodes 1,3,4,5,6,8.
 */
object ComfyWorkflowPatcher {

    private const val LEGACY_NODE_KSAMPLER = "3"
    private const val LEGACY_NODE_LATENT = "5"
    private const val LEGACY_NODE_POSITIVE = "6"
    private const val LEGACY_NODE_CHECKPOINT = "4"

    private const val FP8_NODE_UNET = "1"
    private const val FP8_NODE_CLIP = "3"
    private const val FP8_NODE_VAE = "4"
    private const val FP8_NODE_LATENT = "5"
    private const val FP8_NODE_POSITIVE = "6"
    private const val FP8_NODE_KSAMPLER = "8"

    fun isUnetLoaderWorkflow(templateJson: String): Boolean {
        return runCatching {
            val n1 = JSONObject(templateJson).optJSONObject(FP8_NODE_UNET)
            n1 != null && n1.optString("class_type") == "UNETLoader"
        }.getOrDefault(false)
    }

    fun buildPromptGraph(
        templateJson: String,
        positivePrompt: String,
        settings: GenerationSettings,
        seed: Long,
        primaryModelFileOverride: String? = null,
        clipFileOverride: String? = null,
        vaeFileOverride: String? = null,
    ): JSONObject {
        val root = JSONObject(templateJson)
        if (isUnetLoaderWorkflow(templateJson)) {
            patchFp8Style(root, positivePrompt, settings, seed, primaryModelFileOverride, clipFileOverride, vaeFileOverride)
        } else {
            patchLegacySd15(root, positivePrompt, settings, seed, primaryModelFileOverride)
        }
        return root
    }

    private fun patchLegacySd15(
        root: JSONObject,
        positivePrompt: String,
        settings: GenerationSettings,
        seed: Long,
        checkpointOverride: String?,
    ) {
        root.getJSONObject(LEGACY_NODE_POSITIVE).getJSONObject("inputs").put("text", positivePrompt)
        root.getJSONObject(LEGACY_NODE_LATENT).getJSONObject("inputs").apply {
            put("width", settings.width)
            put("height", settings.height)
        }
        root.getJSONObject(LEGACY_NODE_KSAMPLER).getJSONObject("inputs").apply {
            put("seed", seed)
            put("steps", settings.quality.toSamplerSteps())
            put("sampler_name", settings.comfySamplerApiName())
        }
        val ckpt = checkpointOverride?.trim().orEmpty()
        if (ckpt.isNotEmpty()) {
            root.getJSONObject(LEGACY_NODE_CHECKPOINT).getJSONObject("inputs").put("ckpt_name", ckpt)
        }
    }

    private fun patchFp8Style(
        root: JSONObject,
        positivePrompt: String,
        settings: GenerationSettings,
        seed: Long,
        unetOverride: String?,
        clipOverride: String?,
        vaeOverride: String?,
    ) {
        root.getJSONObject(FP8_NODE_POSITIVE).getJSONObject("inputs").put("text", positivePrompt)
        root.getJSONObject(FP8_NODE_LATENT).getJSONObject("inputs").apply {
            put("width", settings.width)
            put("height", settings.height)
        }
        root.getJSONObject(FP8_NODE_KSAMPLER).getJSONObject("inputs").apply {
            put("seed", seed)
            put("steps", settings.quality.toSamplerSteps())
            put("sampler_name", settings.comfySamplerApiName())
        }
        unetOverride?.trim()?.takeIf { it.isNotEmpty() }?.let {
            root.getJSONObject(FP8_NODE_UNET).getJSONObject("inputs").put("unet_name", it)
        }
        clipOverride?.trim()?.takeIf { it.isNotEmpty() }?.let {
            root.getJSONObject(FP8_NODE_CLIP).getJSONObject("inputs").put("clip_name", it)
        }
        vaeOverride?.trim()?.takeIf { it.isNotEmpty() }?.let {
            root.getJSONObject(FP8_NODE_VAE).getJSONObject("inputs").put("vae_name", it)
        }
    }

    fun checkpointNameFromTemplate(templateJson: String): String {
        return runCatching {
            JSONObject(templateJson)
                .getJSONObject(LEGACY_NODE_CHECKPOINT)
                .getJSONObject("inputs")
                .getString("ckpt_name")
        }.getOrDefault("ComfyUI")
    }

    private fun unetNameFromTemplate(templateJson: String): String {
        return runCatching {
            JSONObject(templateJson)
                .getJSONObject(FP8_NODE_UNET)
                .getJSONObject("inputs")
                .getString("unet_name")
        }.getOrDefault("UNET")
    }

    fun effectiveCheckpointLabel(templateJson: String, primaryOverride: String?): String {
        val o = primaryOverride?.trim().orEmpty()
        if (o.isNotEmpty()) return o
        return if (isUnetLoaderWorkflow(templateJson)) {
            unetNameFromTemplate(templateJson)
        } else {
            checkpointNameFromTemplate(templateJson)
        }
    }
}
