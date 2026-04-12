@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.seagazer.aiimage.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.seagazer.aiimage.R
import com.seagazer.aiimage.domain.GalleryItem
import com.seagazer.aiimage.ui.components.LumiaAmbientBackground
import com.seagazer.aiimage.ui.components.LumiaDestructiveGradientButton
import com.seagazer.aiimage.ui.components.LumiaSheetDragHandle
import com.seagazer.aiimage.ui.components.LumiaTopAppBar
import com.seagazer.aiimage.ui.components.cloudShadow
import com.seagazer.aiimage.ui.components.etherealPrimaryGradientBrush
import com.seagazer.aiimage.ui.dialogs.PermanentDeleteConfirmDialog
import com.seagazer.aiimage.ui.theme.LumiaDesign

@Composable
fun RecycleBinScreen(
    items: List<GalleryItem>,
    onBack: () -> Unit,
    onRestore: (GalleryItem) -> Unit,
    onPermanentDelete: (GalleryItem) -> Unit,
) {
    var sheetItem by remember { mutableStateOf<GalleryItem?>(null) }
    var pendingPermanentDelete by remember { mutableStateOf<GalleryItem?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PermanentDeleteConfirmDialog(
        visible = pendingPermanentDelete != null,
        artifactName = pendingPermanentDelete?.let { del ->
            val cap = del.caption?.trim()?.take(48)?.ifBlank { null }
            val fromPrompt = del.prompt.lineSequence().firstOrNull()?.trim()?.take(48)?.ifBlank { null }
            cap ?: fromPrompt ?: stringResource(R.string.recycle_bin)
        } ?: "",
        onConfirm = {
            val p = pendingPermanentDelete
            pendingPermanentDelete = null
            if (p != null) {
                onPermanentDelete(p)
                if (sheetItem?.id == p.id) sheetItem = null
            }
        },
        onDismiss = { pendingPermanentDelete = null },
    )

    Box(Modifier.fillMaxSize()) {
        LumiaAmbientBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            LumiaTopAppBar(showBack = true, onBack = onBack, title = stringResource(R.string.recycle_bin))
            if (items.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxSize()) {
                    RecycleBinEmptyState()
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.recycle_bin),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 36.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(20.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        items(items, key = { it.id }) { item ->
                            RecycleBinGridCard(
                                item = item,
                                onClick = { sheetItem = item },
                            )
                        }
                    }
                }
            }
        }
    }

    val item = sheetItem
    if (item != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetItem = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            scrimColor = LumiaDesign.ScrimDark,
            tonalElevation = 0.dp,
            dragHandle = { LumiaSheetDragHandle() },
            shape = RoundedCornerShape(
                topStart = LumiaDesign.radiusSheetTop,
                topEnd = LumiaDesign.radiusSheetTop,
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = stringResource(R.string.recycle_bin),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .clip(RoundedCornerShape(LumiaDesign.radiusLg))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Spacer(Modifier.height(20.dp))
                val pillBrush = etherealPrimaryGradientBrush()
                val pillShape = RoundedCornerShape(percent = 50)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(pillShape)
                        .background(pillBrush)
                        .cloudShadow(pillShape, 10.dp)
                        .clickable(role = Role.Button) {
                            onRestore(item)
                            sheetItem = null
                        }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.restore_to_gallery),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                LumiaDestructiveGradientButton(
                    text = stringResource(R.string.permanently_delete),
                    onClick = {
                        pendingPermanentDelete = item
                    },
                    height = 56.dp,
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RecycleBinGridCard(
    item: GalleryItem,
    onClick: () -> Unit,
) {
    val cap = item.caption.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .cloudShadow(RoundedCornerShape(12.dp), 12.dp)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = cap,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f),
                        ),
                    ),
                )
                .padding(12.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                cap.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecycleBinEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Restore,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = stringResource(R.string.recycle_bin_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.recycle_bin_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
