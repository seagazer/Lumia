package com.seagazer.aiimage.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.seagazer.aiimage.R
import com.seagazer.aiimage.domain.AppLanguageOption
import com.seagazer.aiimage.util.resolveAppVersionName
import com.seagazer.aiimage.ui.components.LumiaAmbientBackground
import com.seagazer.aiimage.ui.components.LumiaDialogMessage
import com.seagazer.aiimage.ui.components.LumiaDialogTitle
import com.seagazer.aiimage.ui.components.LumiaGlassIconCircle
import com.seagazer.aiimage.ui.components.LumiaGlassEffectDialogPanel
import com.seagazer.aiimage.ui.components.LumiaDestructiveGradientButton
import com.seagazer.aiimage.ui.components.LumiaSecondaryDialogButton
import com.seagazer.aiimage.ui.components.LumiaTopAppBar
import com.seagazer.aiimage.ui.theme.ApplyDialogDimBehind
import com.seagazer.aiimage.ui.theme.LumiaOverlayTheme
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning

@Composable
fun SettingsScreen(
    themeDark: Boolean,
    appLanguage: AppLanguageOption,
    comfyBaseUrl: String,
    dailyGenerationsUsed: Int,
    dailyGenerationsMax: Int,
    onThemeDarkChange: (Boolean) -> Unit,
    onAppLanguageChange: (AppLanguageOption) -> Unit,
    onComfyUrlChange: (String) -> Unit,
    onClearCache: () -> Unit,
) {
    val context = LocalContext.current
    var clearConfirm by remember { mutableStateOf(false) }
    var languagePicker by remember { mutableStateOf(false) }
    if (languagePicker) {
        LanguagePickerDialog(
            selected = appLanguage,
            onDismiss = { languagePicker = false },
            onSelect = {
                onAppLanguageChange(it)
                languagePicker = false
            },
        )
    }
    if (clearConfirm) {
        ClearCacheConfirmDialog(
            onDismiss = { clearConfirm = false },
            onConfirm = {
                onClearCache()
                clearConfirm = false
                Toast.makeText(context, context.getString(R.string.clear_cache), Toast.LENGTH_SHORT).show()
            },
        )
    }
    Box(Modifier.fillMaxSize()) {
        LumiaAmbientBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            LumiaTopAppBar()
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 120.dp, top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.nav_settings),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_page_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsIconRow(
                        icon = { Icon(Icons.Outlined.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        title = stringResource(R.string.theme),
                        subtitle = stringResource(R.string.theme_switch_subtitle),
                        trailing = {
                            LumiaSwitch(checked = themeDark, onCheckedChange = onThemeDarkChange)
                        },
                    )
                    LanguageSettingsRow(
                        currentLabel = appLanguageLabel(appLanguage),
                        onClick = { languagePicker = true },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp),
                            )
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.14f))
                            .clickable { clearConfirm = true }
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteSweep,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                Text(
                                    stringResource(R.string.clear_cache),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    stringResource(R.string.clear_cache_subtitle_design),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                RecessedAccountCard(
                    dailyGenerationsUsed = dailyGenerationsUsed,
                    dailyGenerationsMax = dailyGenerationsMax,
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.comfy_connection),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ComfySection(
                    baseUrl = comfyBaseUrl,
                    onBaseUrlChange = onComfyUrlChange,
                )
                LumiaFooterLinks()
            }
        }
    }
}

@Composable
private fun appLanguageLabel(option: AppLanguageOption): String = when (option) {
    AppLanguageOption.System -> stringResource(R.string.language_system)
    AppLanguageOption.English -> stringResource(R.string.language_english)
    AppLanguageOption.ChineseSimplified -> stringResource(R.string.language_chinese)
}

@Composable
private fun LanguageSettingsRow(
    currentLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    stringResource(R.string.language),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    currentLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    selected: AppLanguageOption,
    onDismiss: () -> Unit,
    onSelect: (AppLanguageOption) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ApplyDialogDimBehind(dimAmount = 0.5f)
        LumiaOverlayTheme {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                LumiaGlassEffectDialogPanel {
                    LumiaDialogTitle(stringResource(R.string.language_picker_title))
                    Spacer(Modifier.height(16.dp))
                    AppLanguageOption.entries.forEach { option ->
                        val label = appLanguageLabel(option)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelect(option) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (option == selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    LumiaSecondaryDialogButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsIconRow(
    icon: @Composable () -> Unit,
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
private fun LumiaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
private fun RecessedAccountCard(
    dailyGenerationsUsed: Int,
    dailyGenerationsMax: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)))
                        .padding(2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCYtXZSp5vQOo6H7UKrsgpXLl7mJdvSaUt65NIgp-9YpvqszFdQafg2H40eCesiIizO3R3HmYSYuJHLaiPnrmccOzx1lmhFmkq7TQJHaqA8LiWrEZjziQIfAMORVZzh5LwJKhhj13bJSNVtnOZGpAvYnac6X71E73gnxzoBe05KTtHMkY3PiWEImALL6hI0-juc8nBSqog_L1pRYgoRDJG9CDWfPJZWNoiFmqcsMCk7B5Gf4ksothLHmzy3wLezvKBK9DkJJAI07XMV",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .border(2.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onTertiary)
                }
            }
            Column(Modifier.padding(start = 20.dp)) {
                Text(
                    stringResource(R.string.premium_artisan),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    stringResource(R.string.pro_account).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 3.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.daily_generations),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.daily_generations_count, dailyGenerationsUsed, dailyGenerationsMax),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(8.dp))
        val progress = if (dailyGenerationsMax > 0) {
            (dailyGenerationsUsed.toFloat() / dailyGenerationsMax.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiary),
                        ),
                        RoundedCornerShape(percent = 50),
                    ),
            )
        }
    }
}

@Composable
private fun LumiaFooterLinks() {
    val context = LocalContext.current
    val versionName = remember {
        context.applicationContext.resolveAppVersionName()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                stringResource(R.string.privacy).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
                modifier = Modifier.clickable { },
            )
            Text(
                stringResource(R.string.legal_statement).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
                modifier = Modifier.clickable { },
            )
            Text(
                stringResource(R.string.terms).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
                modifier = Modifier.clickable { },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${stringResource(R.string.lumia_os_build)} $versionName",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            ),
        )
    }
}

@Composable
private fun ClearCacheConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ApplyDialogDimBehind(dimAmount = 0.5f)
        LumiaOverlayTheme {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LumiaGlassEffectDialogPanel {
                LumiaGlassIconCircle(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f),
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                LumiaDialogTitle(stringResource(R.string.clear_cache_confirm_title))
                Spacer(Modifier.height(8.dp))
                LumiaDialogMessage(stringResource(R.string.clear_cache_confirm_body))
                Spacer(Modifier.height(28.dp))
                LumiaDestructiveGradientButton(
                    text = stringResource(R.string.clear_everything),
                    onClick = onConfirm,
                )
                Spacer(Modifier.height(12.dp))
                LumiaSecondaryDialogButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                )
            }
        }
        }
    }
}

@Composable
private fun ComfySection(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
    ) {
        ComfyField(stringResource(R.string.base_url), baseUrl, onBaseUrlChange)
    }
}

@Composable
private fun ComfyField(label: String, value: String, onValueChange: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(14.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    )
    Spacer(Modifier.height(12.dp))
}
