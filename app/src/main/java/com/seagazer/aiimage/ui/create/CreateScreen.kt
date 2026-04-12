package com.seagazer.aiimage.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seagazer.aiimage.MainViewModel
import com.seagazer.aiimage.R
import com.seagazer.aiimage.domain.GenerationSettings
import com.seagazer.aiimage.ui.components.LumiaAmbientBackground
import com.seagazer.aiimage.ui.components.LumiaTopAppBar
import com.seagazer.aiimage.ui.components.ShimmerAuraOverlay
import com.seagazer.aiimage.ui.components.cloudShadow
import com.seagazer.aiimage.ui.components.etherealPrimaryGradientBrush
import com.seagazer.aiimage.ui.theme.LumiaDesign
import java.util.Locale

@Composable
fun CreateScreen(
    viewModel: MainViewModel,
    prompt: String,
    generating: Boolean,
    onPromptChange: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    var showTuneSheet by remember { mutableStateOf(false) }
    val settings: GenerationSettings by viewModel.generationSettings.collectAsStateWithLifecycle()

    GenerationSettingsSheet(
        visible = showTuneSheet,
        settings = settings,
        onDismiss = { showTuneSheet = false },
        onApply = { viewModel.setGenerationSettings(it) },
    )

    Box(Modifier.fillMaxSize()) {
        LumiaAmbientBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            LumiaTopAppBar()
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 120.dp, top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.creation_page_title),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.creation_page_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(0.95f),
                )
                Spacer(Modifier.height(32.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                ),
                                start = Offset(0f, 80f),
                                end = Offset(500f, 0f),
                            ),
                        )
                        .padding(4.dp),
                ) {
                    PromptComposer(
                        prompt = prompt,
                        onPromptChange = onPromptChange,
                        onOpenParameters = { showTuneSheet = true },
                    )
                }
                Spacer(Modifier.height(56.dp))

                val generateBrush = etherealPrimaryGradientBrush()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(LumiaDesign.radiusLg))
                        .background(generateBrush)
                        .cloudShadow(RoundedCornerShape(LumiaDesign.radiusLg), 20.dp)
                        .clickable(role = Role.Button, enabled = !generating && prompt.isNotBlank(), onClick = onGenerate),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Text(
                            text = stringResource(R.string.generate_masterpiece).uppercase(Locale.US),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    ShimmerAuraOverlay(visible = generating)
                }
            }
        }
    }
}

@Composable
private fun PromptComposer(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onOpenParameters: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LumiaDesign.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        BasicTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 72.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp,
                lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box {
                    if (prompt.isEmpty()) {
                        Text(
                            text = stringResource(R.string.prompt_placeholder),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                }
            },
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable(onClick = onOpenParameters)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.cd_tune),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.parameters).uppercase(Locale.US),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
