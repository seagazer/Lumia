package com.seagazer.aiimage.ui.theme

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Enables the platform dim behind the dialog window (typical AlertDialog behavior).
 * Compose [androidx.compose.ui.window.Dialog] content must be composed for [LocalView] to refer to the dialog.
 */
@Composable
fun ApplyDialogDimBehind(dimAmount: Float = 0.5f) {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val attrs = attributes
            attrs.dimAmount = dimAmount.coerceIn(0f, 1f)
            attributes = attrs
        }
    }
}
