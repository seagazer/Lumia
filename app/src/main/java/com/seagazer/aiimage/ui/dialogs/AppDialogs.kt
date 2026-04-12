package com.seagazer.aiimage.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seagazer.aiimage.R
import com.seagazer.aiimage.ui.components.LumiaDestructiveGradientButton
import com.seagazer.aiimage.ui.components.LumiaDialogMessage
import com.seagazer.aiimage.ui.components.LumiaDialogTitle
import com.seagazer.aiimage.ui.components.LumiaGlassEffectDialogPanel
import com.seagazer.aiimage.ui.components.LumiaGlassIconCircle
import com.seagazer.aiimage.ui.components.LumiaGlassPanel
import com.seagazer.aiimage.ui.components.LumiaPrimaryDialogButton
import com.seagazer.aiimage.ui.components.LumiaSecondaryDialogButton
import com.seagazer.aiimage.ui.components.LumiaTextLinkButton
import com.seagazer.aiimage.ui.theme.ApplyDialogDimBehind
import com.seagazer.aiimage.ui.theme.LumiaOverlayTheme

@Composable
fun NetworkErrorDialog(
    visible: Boolean,
    detail: String? = null,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    if (!visible) return
    LumiaFullScreenScrimDialog(onDismiss = onCancel) {
        LumiaGlassPanel(
            Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            LumiaGlassIconCircle(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            LumiaDialogTitle(stringResource(R.string.network_error_title))
            Spacer(Modifier.height(8.dp))
            LumiaDialogMessage(stringResource(R.string.network_error_body))
            if (!detail.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                LumiaDialogMessage(detail)
            }
            Spacer(Modifier.height(28.dp))
            LumiaPrimaryDialogButton(
                text = stringResource(R.string.try_again_upper),
                onClick = onRetry,
            )
            Spacer(Modifier.height(8.dp))
            LumiaTextLinkButton(
                text = stringResource(R.string.dismiss_upper),
                onClick = onCancel,
            )
        }
    }
}

@Composable
fun GenerationFailedDialog(
    visible: Boolean,
    detail: String? = null,
    onTryAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    LumiaFullScreenScrimDialog(onDismiss = onDismiss) {
        LumiaGlassPanel(
            Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            LumiaGlassIconCircle(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f),
            ) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            LumiaDialogTitle(stringResource(R.string.generation_failed_title))
            Spacer(Modifier.height(8.dp))
            LumiaDialogMessage(stringResource(R.string.generation_failed_body))
            if (!detail.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                LumiaDialogMessage(detail)
            }
            Spacer(Modifier.height(28.dp))
            LumiaPrimaryDialogButton(
                text = stringResource(R.string.try_again),
                onClick = onTryAgain,
            )
            Spacer(Modifier.height(12.dp))
            LumiaSecondaryDialogButton(
                text = stringResource(R.string.dismiss),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun LumiaFullScreenScrimDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

@Composable
fun DeleteArtifactConfirmDialog(
    visible: Boolean,
    artifactName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    LumiaFullScreenScrimDialog(onDismiss = onDismiss) {
        LumiaGlassEffectDialogPanel(
            Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            LumiaGlassIconCircle(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            ) {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            LumiaDialogTitle(stringResource(R.string.confirm_move_to_recycle_title))
            Spacer(Modifier.height(8.dp))
            LumiaDialogMessage(stringResource(R.string.confirm_move_to_recycle_body))
            Spacer(Modifier.height(28.dp))
            LumiaDestructiveGradientButton(
                text = stringResource(R.string.move_to_recycle_bin).uppercase(),
                onClick = onConfirm,
            )
            Spacer(Modifier.height(12.dp))
            LumiaSecondaryDialogButton(
                text = stringResource(R.string.keep_image),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
fun PermanentDeleteConfirmDialog(
    visible: Boolean,
    artifactName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    LumiaFullScreenScrimDialog(onDismiss = onDismiss) {
        LumiaGlassEffectDialogPanel(
            Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            LumiaGlassIconCircle(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            ) {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            LumiaDialogTitle(stringResource(R.string.recycle_bin_delete_forever_title))
            Spacer(Modifier.height(8.dp))
            LumiaDialogMessage(stringResource(R.string.recycle_bin_delete_forever_body, artifactName))
            Spacer(Modifier.height(28.dp))
            LumiaDestructiveGradientButton(
                text = stringResource(R.string.permanently_delete).uppercase(),
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
