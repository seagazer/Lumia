package com.seagazer.aiimage.data.comfy

import com.seagazer.aiimage.domain.GenerationSettings

/** Maps UI sampler label to ComfyUI `KSampler.inputs.sampler_name`. */
fun GenerationSettings.comfySamplerApiName(): String {
    val label = sampler.trim()
    return SAMPLER_LABEL_TO_COMFY[label] ?: label.lowercase().replace(' ', '_')
}

private val SAMPLER_LABEL_TO_COMFY = mapOf(
    "Euler Ancestral" to "euler_ancestral",
    "DPM++ 2M Karras" to "dpmpp_2m_karras",
    "Heun" to "heun",
    "LMS Discrete" to "lms",
)
