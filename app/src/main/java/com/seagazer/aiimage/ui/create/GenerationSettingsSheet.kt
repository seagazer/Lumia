@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.seagazer.aiimage.ui.create

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seagazer.aiimage.R
import com.seagazer.aiimage.domain.GenerationSettings
import com.seagazer.aiimage.domain.ImageQuality
import com.seagazer.aiimage.ui.components.LumiaSheetDragHandle
import com.seagazer.aiimage.ui.components.cloudShadow
import com.seagazer.aiimage.ui.components.etherealPrimaryGradientBrush
import com.seagazer.aiimage.ui.theme.LumiaDesign
import java.util.Locale

private const val DimMin = 64
private const val DimMax = 1024

private const val SeedMax = 4294967295L

/** @return null if the numeric value would exceed [DimMax] (caller should toast and keep the current text). */
private fun filteredResolutionDigits(proposed: String): String? {
    val digits = proposed.filter { it.isDigit() }.take(4)
    val n = digits.toIntOrNull()
    if (n != null && n > DimMax) return null
    return digits
}

private fun normalizeDimension(raw: String, fallback: Int): Int {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
        return alignDimension(fallback)
    }
    val n = trimmed.toIntOrNull() ?: fallback
    return alignDimension(n)
}

private fun alignDimension(value: Int): Int {
    val clamped = value.coerceIn(DimMin, DimMax)
    val aligned = (clamped / 8) * 8
    return aligned.coerceIn(DimMin, DimMax).let { if (it < DimMin) DimMin else it }
}

@Composable
fun GenerationSettingsSheet(
    visible: Boolean,
    settings: GenerationSettings,
    onDismiss: () -> Unit,
    onApply: (GenerationSettings) -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val resolutionMaxToast = stringResource(R.string.resolution_max_exceeded_toast)
    val seedInvalidToast = stringResource(R.string.seed_invalid_toast)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf(settings) }
    var widthText by remember { mutableStateOf(settings.width.toString()) }
    var heightText by remember { mutableStateOf(settings.height.toString()) }
    var samplerMenuExpanded by remember { mutableStateOf(false) }
    var seedText by remember { mutableStateOf(settings.fixedSeed.toString()) }
    val samplers = remember {
        listOf("Euler Ancestral", "DPM++ 2M Karras", "Heun", "LMS Discrete")
    }
    val maxSheetH = (LocalConfiguration.current.screenHeightDp * 0.75f).dp

    LaunchedEffect(visible, settings) {
        if (visible) {
            draft = settings
            widthText = settings.width.toString()
            heightText = settings.height.toString()
            seedText = settings.fixedSeed.toString()
        }
    }

    fun applyAndDismiss() {
        val wTrim = widthText.trim()
        val hTrim = heightText.trim()
        val wRaw = wTrim.toIntOrNull()
        val hRaw = hTrim.toIntOrNull()
        if (wRaw != null && wRaw > DimMax || hRaw != null && hRaw > DimMax) {
            Toast.makeText(context, resolutionMaxToast, Toast.LENGTH_SHORT).show()
            return
        }
        val w = normalizeDimension(widthText, draft.width)
        val h = normalizeDimension(heightText, draft.height)
        val fixedSeed = if (draft.useRandomSeed) {
            draft.fixedSeed
        } else {
            val trimmed = seedText.trim()
            if (trimmed.isEmpty()) {
                Toast.makeText(context, seedInvalidToast, Toast.LENGTH_SHORT).show()
                return
            }
            val n = trimmed.toLongOrNull()
            if (n == null || n < 0 || n > SeedMax) {
                Toast.makeText(context, seedInvalidToast, Toast.LENGTH_SHORT).show()
                return
            }
            n
        }
        onApply(draft.copy(width = w, height = h, fixedSeed = fixedSeed))
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = LumiaDesign.ScrimDark,
        tonalElevation = 0.dp,
        dragHandle = { LumiaSheetDragHandle() },
        shape = RoundedCornerShape(
            topStart = LumiaDesign.radiusSheetTop,
            topEnd = LumiaDesign.radiusSheetTop
        ),
    ) {
        val footerDividerColor = MaterialTheme.colorScheme.outlineVariant
        Column(
            Modifier
                .fillMaxWidth()
                .height(maxSheetH)
                .padding(horizontal = 24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.generation_settings_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.engine_configuration),
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.resolution).uppercase(Locale.US),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.resolution_pixels_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ResolutionField(
                        label = stringResource(R.string.resolution_width),
                        value = widthText,
                        onValueChange = { t ->
                            when (val next = filteredResolutionDigits(t)) {
                                null -> Toast.makeText(
                                    context,
                                    resolutionMaxToast,
                                    Toast.LENGTH_SHORT
                                ).show()

                                else -> widthText = next
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    ResolutionField(
                        label = stringResource(R.string.resolution_height),
                        value = heightText,
                        onValueChange = { t ->
                            when (val next = filteredResolutionDigits(t)) {
                                null -> Toast.makeText(
                                    context,
                                    resolutionMaxToast,
                                    Toast.LENGTH_SHORT
                                ).show()

                                else -> heightText = next
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.image_quality).uppercase(Locale.US),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ImageQuality.entries.forEach { q ->
                        QualityChip(
                            label = q.localizedLabel(),
                            selected = draft.quality == q,
                            modifier = Modifier.weight(1f),
                            onClick = { draft = draft.copy(quality = q) },
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.sampler_method).uppercase(Locale.US),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                ExposedDropdownMenuBox(
                    expanded = samplerMenuExpanded,
                    onExpandedChange = { samplerMenuExpanded = it },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = samplerDisplayName(draft.sampler),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    DropdownMenu(
                        expanded = samplerMenuExpanded,
                        onDismissRequest = { samplerMenuExpanded = false },
                        modifier = Modifier.exposedDropdownSize(true),
                    ) {
                        samplers.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(samplerDisplayName(name)) },
                                onClick = {
                                    draft = draft.copy(sampler = name)
                                    samplerMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.generation_seed).uppercase(Locale.US),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.seed_random_each_time),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                    )
                    Switch(
                        checked = draft.useRandomSeed,
                        onCheckedChange = { checked ->
                            draft = draft.copy(useRandomSeed = checked)
                            if (!checked) seedText = draft.fixedSeed.toString()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                if (!draft.useRandomSeed) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.seed_fixed_value_hint),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = seedText,
                        onValueChange = { t -> seedText = t.filter { it.isDigit() }.take(10) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.generation_seed)) },
                        placeholder = { Text(GenerationSettings.DEFAULT_PROFILE_SEED.toString()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontFeatureSettings = "tnum",
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
                Spacer(Modifier.height(40.dp))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawLine(
                            color = footerDividerColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = stroke,
                        )
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 8.dp, vertical = 24.dp),
            ) {
                val confirmBrush = etherealPrimaryGradientBrush()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(LumiaDesign.radiusLg))
                        .background(confirmBrush)
                        .cloudShadow(RoundedCornerShape(LumiaDesign.radiusLg), 16.dp)
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) { applyAndDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.confirm_settings),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.2).sp,
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.saved_to_local_profile).uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 2.4.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ImageQuality.localizedLabel(): String = stringResource(
    when (this) {
        ImageQuality.Low -> R.string.quality_low
        ImageQuality.Med -> R.string.quality_med
        ImageQuality.High -> R.string.quality_high
    },
)

@Composable
private fun samplerDisplayName(storedName: String): String = when (storedName) {
    "Euler Ancestral" -> stringResource(R.string.sampler_euler_ancestral)
    "DPM++ 2M Karras" -> stringResource(R.string.sampler_dpmpp_2m_karras)
    "Heun" -> stringResource(R.string.sampler_heun)
    "LMS Discrete" -> stringResource(R.string.sampler_lms_discrete)
    else -> storedName
}

@Composable
private fun ResolutionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 10.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontFeatureSettings = "tnum",
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

@Composable
private fun QualityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            )
            .then(
                if (selected) Modifier.cloudShadow(RoundedCornerShape(8.dp), 8.dp)
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
