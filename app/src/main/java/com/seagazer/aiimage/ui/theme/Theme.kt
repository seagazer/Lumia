package com.seagazer.aiimage.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Lumia dark palette from ux HTML; use for full app and overlay-only theming. */
val LumiaDarkColorScheme = darkColorScheme(
    primary = Color(0xFFcdbdff),
    onPrimary = Color(0xFF370096),
    primaryContainer = Color(0xFF5d21df),
    onPrimaryContainer = Color(0xFFcebfff),
    secondary = Color(0xFFc7bfff),
    onSecondary = Color(0xFF2c148e),
    secondaryContainer = Color(0xFF4635a7),
    onSecondaryContainer = Color(0xFFb8afff),
    tertiary = Color(0xFF00daf3),
    onTertiary = Color(0xFF00363d),
    tertiaryContainer = Color(0xFF005d68),
    onTertiaryContainer = Color(0xFF00dcf5),
    error = Color(0xFFffb4ab),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000a),
    onErrorContainer = Color(0xFFffdad6),
    background = Color(0xFF10131a),
    onBackground = Color(0xFFe1e2eb),
    surface = Color(0xFF10131a),
    onSurface = Color(0xFFe1e2eb),
    surfaceVariant = Color(0xFF32353c),
    onSurfaceVariant = Color(0xFFcbc3d9),
    outline = Color(0xFF948da2),
    outlineVariant = Color(0xFF494456),
    surfaceContainerLowest = Color(0xFF0b0e14),
    surfaceContainerLow = Color(0xFF191c22),
    surfaceContainer = Color(0xFF1d2026),
    surfaceContainerHigh = Color(0xFF272a31),
    surfaceContainerHighest = Color(0xFF32353c),
    scrim = Color(0x99000000),
)

/** Light palette (same tokens as [AIImageTheme] when `darkTheme` is false). */
val LumiaLightColorScheme = lightColorScheme(
    primary = Color(0xFF6A1CF6),
    onPrimary = Color(0xFFF7F0FF),
    primaryContainer = Color(0xFFAC8EFF),
    onPrimaryContainer = Color(0xFF2A0070),
    secondary = Color(0xFF7343A9),
    onSecondary = Color(0xFFFAEFFF),
    secondaryContainer = Color(0xFFE3C6FF),
    onSecondaryContainer = Color(0xFF5E2D93),
    tertiary = Color(0xFF9D365D),
    onTertiary = Color(0xFFFFEFF1),
    tertiaryContainer = Color(0xFFFF8DB2),
    onTertiaryContainer = Color(0xFF640332),
    error = Color(0xFFB41340),
    onError = Color(0xFFFFEFEF),
    errorContainer = Color(0xFFF74B6D),
    onErrorContainer = Color(0xFF510017),
    background = EtherealBackground,
    onBackground = EtherealOnBackground,
    surface = EtherealSurface,
    onSurface = EtherealOnSurface,
    surfaceVariant = EtherealSurfaceVariant,
    onSurfaceVariant = EtherealOnSurfaceVariant,
    outline = EtherealOutline,
    outlineVariant = EtherealOutlineVariant,
    surfaceContainerLowest = EtherealSurfaceContainerLowest,
    surfaceContainerLow = EtherealSurfaceContainerLow,
    surfaceContainer = EtherealSurfaceContainer,
    surfaceContainerHigh = EtherealSurfaceContainerHigh,
    surfaceContainerHighest = EtherealSurfaceContainerHighest,
    scrim = Color(0x52000000),
)

/** App-level dark vs light toggle (Settings). Dialogs/sheets read this to stay in sync. */
val LocalAppDarkTheme = staticCompositionLocalOf { true }

@Composable
fun AIImageTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) LumiaDarkColorScheme else LumiaLightColorScheme
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    if (activity != null && !view.isInEditMode) {
        SideEffect {
            val window = activity.window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

/** Material theme for dialog/sheet content — follows [LocalAppDarkTheme] / Settings. */
@Composable
fun LumiaOverlayTheme(content: @Composable () -> Unit) {
    val darkTheme = LocalAppDarkTheme.current
    MaterialTheme(
        colorScheme = if (darkTheme) LumiaDarkColorScheme else LumiaLightColorScheme,
        typography = Typography,
        content = content,
    )
}
