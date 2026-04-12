package com.seagazer.aiimage.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Radii and scrim/glass tokens aligned with ux design HTML (Tailwind rem → dp at 1:1). */
object LumiaDesign {
    val radiusLg = 12.dp
    val radiusXl = 16.dp
    val radius2xl = 24.dp
    val radiusSheetTop = 40.dp
    val radiusModal = 40.dp
    val radiusDialog = 32.dp

    /** `text-indigo-200` for section labels in parameter sheets. */
    val LabelIndigo200 = Color(0xFFC7D2FE)

    /** Bottom sheet glass: `rgba(29, 32, 38, 0.65)` → #1d2026 @ 65% */
    val GlassSheet = Color(0xA61D2026)

    /** Same hue as [GlassSheet], fully opaque — used where readability beats glass (e.g. parameter sheet). */
    val SheetSolid = Color(0xFF1D2026)

    /** Card glass: `rgba(50, 53, 60, 0.4)` */
    val GlassCard = Color(0x6632353C)

    /** Modal overlay: `bg-black/60` (legacy; prefer [DialogScrimOpaque] for dialogs). */
    val ScrimDark = Color(0x99000000)

    /** Softer canvas scrim: `bg-background/40` (legacy; prefer opaque dialog canvas). */
    val ScrimSoft = Color(0x6610131A)

    /** Fully opaque dim behind dialogs — content behind must not show through. */
    val DialogScrimOpaque = Color(0xFF000000)

    val GhostBorderLight = Color.White.copy(alpha = 0.05f)
    val GhostBorderMedium = Color.White.copy(alpha = 0.10f)

    /** Tinted elevated shadow (~ `rgba(55,0,150,0.12)`). */
    val ShadowTint = Color(0xFF370096).copy(alpha = 0.12f)
}
