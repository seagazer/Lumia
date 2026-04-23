package com.seagazer.aiimage.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.first
import com.seagazer.aiimage.R
import com.seagazer.aiimage.domain.GalleryItem
import com.seagazer.aiimage.ui.components.LumiaAmbientBackground
import com.seagazer.aiimage.ui.components.LumiaTopAppBar
import com.seagazer.aiimage.ui.components.cloudShadow
import com.seagazer.aiimage.ui.theme.LumiaDesign

private sealed class GalleryResolutionFilter {
    data object All : GalleryResolutionFilter()
    data object Unknown : GalleryResolutionFilter()
    data class Exact(val width: Int, val height: Int) : GalleryResolutionFilter()
}

private fun buildResolutionFilterOptions(items: List<GalleryItem>): List<GalleryResolutionFilter> {
    if (items.isEmpty()) return listOf(GalleryResolutionFilter.All)
    val hasInvalid = items.any { it.width <= 0 || it.height <= 0 }
    val sizes = items.asSequence()
        .map { it.width to it.height }
        .filter { (w, h) -> w > 0 && h > 0 }
        .distinct()
        .sortedWith(compareBy({ it.first }, { it.second }))
        .map { (w, h) -> GalleryResolutionFilter.Exact(w, h) }
        .toList()
    return buildList {
        add(GalleryResolutionFilter.All)
        addAll(sizes)
        if (hasInvalid) add(GalleryResolutionFilter.Unknown)
    }
}

private fun GalleryItem.matches(filter: GalleryResolutionFilter): Boolean = when (filter) {
    GalleryResolutionFilter.All -> true
    GalleryResolutionFilter.Unknown -> width <= 0 || height <= 0
    is GalleryResolutionFilter.Exact -> width == filter.width && height == filter.height
}

@Composable
fun GalleryScreen(
    items: List<GalleryItem>,
    onOpenItem: (GalleryItem) -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenPrivateSpace: () -> Unit = {},
    lazyGridState: LazyGridState = rememberLazyGridState(),
    scrollToItemId: String? = null,
    onScrollToItemConsumed: () -> Unit = {},
) {
    val options = remember(items) { buildResolutionFilterOptions(items) }
    var filter by remember { mutableStateOf<GalleryResolutionFilter>(GalleryResolutionFilter.All) }
    LaunchedEffect(options) {
        if (filter !in options) filter = GalleryResolutionFilter.All
    }
    val filtered = remember(items, filter) { items.filter { it.matches(filter) } }

    LaunchedEffect(scrollToItemId, items) {
        val id = scrollToItemId ?: return@LaunchedEffect
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return@LaunchedEffect
        filter = GalleryResolutionFilter.All
        snapshotFlow { lazyGridState.layoutInfo.totalItemsCount }
            .first { it > idx }
        lazyGridState.animateScrollToItem(idx)
        onScrollToItemConsumed()
    }

    Box(Modifier.fillMaxSize()) {
        LumiaAmbientBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            LumiaTopAppBar(
                trailing = {
                    IconButton(onClick = onOpenPrivateSpace) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = stringResource(R.string.cd_private_space),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onOpenRecycleBin) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.cd_recycle_bin),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
            if (items.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxSize()) { EmptyGalleryState() }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.gallery_page_title),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            lineHeight = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .height(4.dp)
                            .fillMaxWidth(0.2f)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary,
                                    ),
                                ),
                            ),
                    )
                    Spacer(Modifier.height(28.dp))

                    TotalArtifactsCard(count = items.size, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    ResolutionFilterDropdown(
                        options = options,
                        selected = filter,
                        onSelect = { filter = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))

                    LazyVerticalGrid(
                        state = lazyGridState,
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            GalleryGridCard(item = item, onClick = { onOpenItem(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalArtifactsCard(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                stringResource(R.string.total_artifacts).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.GridView, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ResolutionFilterDropdown(
    options: List<GalleryResolutionFilter>,
    selected: GalleryResolutionFilter,
    onSelect: (GalleryResolutionFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var anchorWidth by remember { mutableStateOf(0.dp) }
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    anchorWidth = with(density) { coords.size.width.toDp() }
                }
                .clip(RoundedCornerShape(LumiaDesign.radiusLg))
                .background(LumiaDesign.GlassCard)
                .cloudShadow(RoundedCornerShape(LumiaDesign.radiusLg), 10.dp)
                .borderGhost()
                .clickable { expanded = true }
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.DisplaySettings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.filter_resolution),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(Icons.Outlined.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(anchorWidth)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    RoundedCornerShape(LumiaDesign.radiusLg),
                )
                .border(BorderStroke(1.dp, LumiaDesign.GhostBorderMedium), RoundedCornerShape(LumiaDesign.radiusLg)),
        ) {
            options.forEach { f ->
                val t = when (f) {
                    GalleryResolutionFilter.All -> stringResource(R.string.resolution_preset_all)
                    GalleryResolutionFilter.Unknown -> stringResource(R.string.gallery_resolution_unknown)
                    is GalleryResolutionFilter.Exact -> "${f.width}×${f.height}"
                }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (f == selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    ),
                            )
                            Text(t, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    onClick = {
                        onSelect(f)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(),
                )
            }
        }
    }
}

private fun Modifier.borderGhost(): Modifier =
    this.border(1.dp, LumiaDesign.GhostBorderLight, RoundedCornerShape(LumiaDesign.radiusLg))

@Composable
private fun GalleryGridCard(
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
private fun EmptyGalleryState() {
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
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = stringResource(R.string.gallery_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.gallery_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
