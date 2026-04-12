package com.seagazer.aiimage.ui.theme

import androidx.compose.ui.graphics.Color

/** Lumia palette from ux design HTML exports (dark-first). Prefer [MaterialTheme.colorScheme] in UI. */
val EtherealPrimary = Color(0xFFcdbdff)
val EtherealOnPrimary = Color(0xFF370096)
val EtherealPrimaryContainer = Color(0xFF5d21df)
val EtherealOnPrimaryContainer = Color(0xFFcebfff)

val EtherealSecondary = Color(0xFFc7bfff)
val EtherealOnSecondary = Color(0xFF2c148e)
val EtherealSecondaryContainer = Color(0xFF4635a7)
val EtherealOnSecondaryContainer = Color(0xFFb8afff)

val EtherealTertiary = Color(0xFF00daf3)
val EtherealOnTertiary = Color(0xFF00363d)
val EtherealTertiaryContainer = Color(0xFF005d68)
val EtherealOnTertiaryContainer = Color(0xFF00dcf5)

val EtherealError = Color(0xFFffb4ab)
val EtherealOnError = Color(0xFF690005)
val EtherealErrorContainer = Color(0xFF93000a)
val EtherealOnErrorContainer = Color(0xFFffdad6)

/** Legacy bottom-nav active (cyan); bar now uses [MaterialTheme.colorScheme.primary] to match the top bar. */
val EtherealNavActive = Color(0xFF00daf3)
val EtherealNavInactive = Color(0xFF64748b)

/** @deprecated No longer used by bottom bar; kept for binary compat in external references. */
val EtherealNavSelectedBg = Color(0x00000000)
val EtherealNavSelectedContent = EtherealPrimary
val EtherealNavUnselected = EtherealNavInactive

/** Legacy light-theme surfaces (only referenced where light UI is explicit). */
val EtherealBackground = Color(0xFFF5F6F7)
val EtherealOnBackground = Color(0xFF2C2F30)
val EtherealSurface = Color(0xFFF5F6F7)
val EtherealOnSurface = Color(0xFF2C2F30)
val EtherealSurfaceVariant = Color(0xFFDADDDF)
val EtherealOnSurfaceVariant = Color(0xFF595C5D)
val EtherealOutline = Color(0xFF757778)
val EtherealOutlineVariant = Color(0xFFABADAE)
val EtherealSurfaceContainerLowest = Color(0xFFFFFFFF)
val EtherealSurfaceContainerLow = Color(0xFFEFF1F2)
val EtherealSurfaceContainer = Color(0xFFE6E8EA)
val EtherealSurfaceContainerHigh = Color(0xFFE0E3E4)
val EtherealSurfaceContainerHighest = Color(0xFFDADDDF)
