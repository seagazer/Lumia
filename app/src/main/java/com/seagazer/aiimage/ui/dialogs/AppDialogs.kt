package com.seagazer.aiimage.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

@Composable
fun SetPrivatePasswordDialog(
    visible: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val tooShortMsg = stringResource(R.string.password_too_short)
    val mismatchMsg = stringResource(R.string.password_mismatch)
    LumiaFullScreenScrimDialog(onDismiss = onDismiss) {
        LumiaGlassEffectDialogPanel(
            Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            LumiaGlassIconCircle(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            LumiaDialogTitle(stringResource(R.string.set_password_title))
            Spacer(Modifier.height(8.dp))
            LumiaDialogMessage(stringResource(R.string.set_password_body))
            Spacer(Modifier.height(20.dp))
            PasswordField(
                value = password,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) password = it },
                placeholder = "••••••",
            )
            Spacer(Modifier.height(12.dp))
            PasswordField(
                value = confirmPassword,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPassword = it },
                placeholder = stringResource(R.string.confirm_password_hint),
            )
            if (errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    errorMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
            LumiaPrimaryDialogButton(
                text = stringResource(R.string.confirm),
                onClick = {
                    when {
                        password.length != 6 -> errorMsg = tooShortMsg
                        password != confirmPassword -> errorMsg = mismatchMsg
                        else -> onConfirm(password)
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            LumiaSecondaryDialogButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
fun EnterPrivatePasswordDialog(
    visible: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val incorrectMsg = stringResource(R.string.password_incorrect)
    val tooShortMsg = stringResource(R.string.password_too_short)
    LumiaFullScreenScrimDialog(onDismiss = onDismiss) {
        LumiaGlassEffectDialogPanel(
            Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            LumiaGlassIconCircle(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            LumiaDialogTitle(stringResource(R.string.enter_password_title))
            Spacer(Modifier.height(8.dp))
            LumiaDialogMessage(stringResource(R.string.enter_password_body))
            Spacer(Modifier.height(20.dp))
            PasswordField(
                value = password,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) password = it },
                placeholder = "••••••",
            )
            if (errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    errorMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
            LumiaPrimaryDialogButton(
                text = stringResource(R.string.confirm),
                onClick = {
                    when {
                        password.length != 6 -> errorMsg = tooShortMsg
                        else -> {
                            errorMsg = incorrectMsg
                            onConfirm(password)
                        }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            LumiaSecondaryDialogButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
