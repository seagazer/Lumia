package com.seagazer.aiimage.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.seagazer.aiimage.R
import com.seagazer.aiimage.ui.theme.LumiaDesign
import com.seagazer.aiimage.ui.theme.ApplyDialogDimBehind
import com.seagazer.aiimage.ui.theme.LumiaOverlayTheme

@Composable
fun LumiaAmbientBackground(modifier: Modifier = Modifier) {
    val indigoOrb = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
    val cyanOrb = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.10f)
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val r = size.minDimension * 0.45f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(indigoOrb, Color.Transparent),
                        center = Offset(size.width * 1.05f, -size.height * 0.02f),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(size.width * 0.92f, -size.height * 0.08f),
                )
                val r2 = size.minDimension * 0.4f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cyanOrb, Color.Transparent),
                        center = Offset(-size.width * 0.05f, size.height * 1.02f),
                        radius = r2,
                    ),
                    radius = r2,
                    center = Offset(-size.width * 0.05f, size.height * 1.05f),
                )
            },
    )
}

@Composable
fun LumiaTopAppBar(
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    title: String = stringResource(R.string.lumia_ai_brand),
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val fade = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            Color.Transparent,
        ),
    )
    Box(
        modifier
            .fillMaxWidth()
            .background(fade)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .then(modifier),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showBack && onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (trailing != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    trailing.invoke(this)
                }
            }
        }
    }
}

@Composable
fun LumiaSheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 48.dp, height = 6.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        )
    }
}

/** Dialog panel: solid fill + ghost border (readability over glass translucency). */
@Composable
fun LumiaGlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(LumiaDesign.radiusDialog)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .cloudShadow(shape, 24.dp)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

/** Modal shell: solid surface (same role as former glass-effect panel). */
@Composable
fun LumiaGlassEffectDialogPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(LumiaDesign.radiusDialog)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .cloudShadow(shape, 32.dp)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

/** Destructive CTA: same red gradient as delete-confirm dialogs (error_container → error). */
@Composable
fun LumiaDestructiveGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(LumiaDesign.radiusLg)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.error,
                    ),
                    start = Offset.Zero,
                    end = Offset(400f, 400f),
                ),
            )
            .cloudShadow(shape, 12.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onError,
                fontWeight = if (height >= 56.dp) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = if (height >= 56.dp) 18.sp else 12.sp,
                letterSpacing = if (height >= 56.dp) (-0.2).sp else 1.2.sp,
            )
        }
    }
}

@Composable
fun LumiaGlassIconCircle(
    containerColor: Color,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(containerColor)
            .then(modifier),
        contentAlignment = Alignment.Center,
        content = icon,
    )
}

@Composable
fun LumiaDialogTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
fun LumiaDialogMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/** Primary dialog / sheet CTA — same purple gradient as home “Generate”. */
@Composable
fun LumiaPrimaryDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LumiaDesign.radiusLg)
    val brush = etherealPrimaryGradientBrush()
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(brush)
            .cloudShadow(shape, 12.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

/** Secondary / cancel — outlined purple, matches primary family without filled gradient. */
@Composable
fun LumiaSecondaryDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LumiaDesign.radiusLg)
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
            .clickable(role = Role.Button, onClick = onClick)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

/** Progress modal: glass card, luminous ring, cancel, M3 linear progress (indeterminate or percent). */
@Composable
fun LumiaGeneratingDialog(
    visible: Boolean,
    title: String,
    progressPercent: Int? = null,
    onCancel: (() -> Unit)? = null,
) {
    if (!visible) return
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ApplyDialogDimBehind(dimAmount = 0.5f)
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
            }
        }
        LumiaOverlayTheme {
        val glowPrimary = MaterialTheme.colorScheme.primary
        val glowTertiary = MaterialTheme.colorScheme.tertiary
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val cardShape = RoundedCornerShape(LumiaDesign.radiusModal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                    .cloudShadow(cardShape, 40.dp),
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .drawBehind {
                            val c1 = Offset(size.width * 0.15f, size.height * 0.12f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(glowPrimary.copy(alpha = 0.2f), Color.Transparent),
                                    center = c1,
                                    radius = size.minDimension * 0.45f,
                                ),
                                center = c1,
                                radius = size.minDimension * 0.35f,
                            )
                            val c2 = Offset(size.width * 0.92f, size.height * 0.88f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(glowTertiary.copy(alpha = 0.1f), Color.Transparent),
                                    center = c2,
                                    radius = size.minDimension * 0.4f,
                                ),
                                center = c2,
                                radius = size.minDimension * 0.35f,
                            )
                        },
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 40.dp, end = 40.dp, top = 48.dp, bottom = 8.dp),
                ) {
                    Box(
                        Modifier.size(112.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                                            glowPrimary.copy(alpha = 0.4f),
                                        ),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (progressPercent == null) {
                            stringResource(R.string.generating_subtitle)
                        } else {
                            stringResource(R.string.generating_progress_percent, progressPercent)
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.6.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (onCancel != null) {
                        Spacer(Modifier.height(24.dp))
                        LumiaSecondaryDialogButton(
                            text = stringResource(R.string.cancel),
                            onClick = onCancel,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    Spacer(Modifier.height(40.dp))
                }
                val barModifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(bottomStart = LumiaDesign.radiusModal, bottomEnd = LumiaDesign.radiusModal))
                if (progressPercent == null) {
                    LinearProgressIndicator(
                        modifier = barModifier,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                } else {
                    val p = (progressPercent.coerceIn(0, 100)) / 100f
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = barModifier,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }
        }
    }
}

@Composable
fun LumiaTextLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
