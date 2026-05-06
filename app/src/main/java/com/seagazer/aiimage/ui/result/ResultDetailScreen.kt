@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.seagazer.aiimage.ui.result

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.seagazer.aiimage.R
import com.seagazer.aiimage.domain.ResultDetail
import com.seagazer.aiimage.ui.components.LumiaDestructiveGradientButton
import com.seagazer.aiimage.ui.components.ShimmerAuraOverlay
import com.seagazer.aiimage.ui.components.cloudShadow
import com.seagazer.aiimage.ui.components.etherealPrimaryGradientBrush
import com.seagazer.aiimage.ui.dialogs.DeleteArtifactConfirmDialog
import com.seagazer.aiimage.ui.components.LumiaTopAppBar
import com.seagazer.aiimage.ui.theme.LumiaDesign
import kotlinx.coroutines.delay
import java.io.File
import java.net.URI
import java.text.DateFormat
import java.util.Date

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

private const val DetailImageSharedTransitionMs = 280

private fun detailImageSharedTransitionKey(detailId: String) = "detail_image_$detailId"

private val DetailImageBoundsTransform = BoundsTransform { _, _ ->
    tween(DetailImageSharedTransitionMs, easing = FastOutSlowInEasing)
}

@Composable
fun ResultDetailScreen(
    detail: ResultDetail,
    detailItems: List<ResultDetail> = emptyList(),
    detailIndex: Int = 0,
    fromGallery: Boolean,
    loadingImage: Boolean,
    onBack: () -> Unit,
    onSaveToGallery: (ResultDetail) -> Boolean,
    onShare: () -> Unit,
    onExport: (ResultDetail) -> Boolean,
    onDeleteFromGallery: ((ResultDetail) -> Unit)? = null,
    onImageViewerVisibilityChanged: ((Boolean) -> Unit)? = null,
    onDetailIndexChange: ((Int) -> Unit)? = null,
    isFromPrivateSpace: Boolean = false,
    onTogglePrivate: ((ResultDetail) -> Unit)? = null,
) {
    val context = LocalContext.current
    val pagerItems = remember(fromGallery, detailItems, detail) {
        if (fromGallery && detailItems.isNotEmpty()) detailItems else listOf(detail)
    }
    val pagerState = rememberPagerState(
        initialPage = detailIndex.coerceIn(0, pagerItems.lastIndex),
        pageCount = { pagerItems.size },
    )
    val currentDetail = pagerItems.getOrElse(pagerState.currentPage) { detail }
    var saveBanner by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    LaunchedEffect(showFullScreen) {
        onImageViewerVisibilityChanged?.invoke(showFullScreen)
    }
    LaunchedEffect(saveBanner) {
        if (saveBanner) {
            delay(4500)
            saveBanner = false
        }
    }
    LaunchedEffect(detailIndex, pagerItems.size) {
        val targetPage = detailIndex.coerceIn(0, pagerItems.lastIndex)
        if (targetPage != pagerState.currentPage) {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState, pagerItems.size) {
        if (pagerItems.isEmpty()) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }
            .collect { page -> onDetailIndexChange?.invoke(page) }
    }

    fun shareImage(currentDetail: ResultDetail) {
        val file = runCatching { File(URI.create(currentDetail.imageUrl)) }.getOrNull() ?: return
        if (!file.isFile) return
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser_title)))
        onShare()
    }

    val fallbackTitle = stringResource(R.string.gallery_page_title)
    val artifactLabel = remember(currentDetail.prompt, fallbackTitle) {
        currentDetail.prompt.lineSequence().firstOrNull()?.trim().orEmpty().take(48)
            .ifBlank { fallbackTitle }
    }

    DeleteArtifactConfirmDialog(
        visible = deleteConfirm,
        artifactName = artifactLabel,
        onConfirm = {
            deleteConfirm = false
            onDeleteFromGallery?.invoke(currentDetail)
        },
        onDismiss = { deleteConfirm = false },
    )

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = showFullScreen,
            label = "fullscreen",
            transitionSpec = {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            },
        ) { isFullScreen ->
            if (isFullScreen) {
                FullScreenImageViewer(
                    imageUrl = currentDetail.imageUrl,
                    sharedImageTransitionKey = detailImageSharedTransitionKey(currentDetail.id),
                    onDismiss = { showFullScreen = false },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    LumiaTopAppBar(showBack = true, onBack = onBack)
                    if (fromGallery) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val pageDetail = pagerItems[page]
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp)
                                    .padding(top = 20.dp, bottom = 120.dp),
                            ) {
                                GalleryArtifactContent(
                                    detail = pageDetail,
                                    loadingImage = loadingImage && page == pagerState.currentPage,
                                    onExport = { onExport(pageDetail) },
                                    onShare = { shareImage(pageDetail) },
                                    onDelete = { deleteConfirm = true },
                                    showDelete = onDeleteFromGallery != null,
                                    onImageClick = { showFullScreen = true },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedContent,
                                    isFromPrivateSpace = isFromPrivateSpace,
                                    onTogglePrivate = onTogglePrivate?.let { toggle -> { toggle(pageDetail) } },
                                )
                            }
                        }
                    } else {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 120.dp, top = 16.dp),
                        ) {
                            GenerationResultContent(
                                detail = currentDetail,
                                loadingImage = loadingImage,
                                saveBanner = saveBanner,
                                onSaveToGallery = {
                                    if (onSaveToGallery(currentDetail)) saveBanner = true
                                },
                                onShare = { shareImage(currentDetail) },
                                onImageClick = { showFullScreen = true },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryArtifactContent(
    detail: ResultDetail,
    loadingImage: Boolean,
    onExport: () -> Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    showDelete: Boolean,
    onImageClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isFromPrivateSpace: Boolean = false,
    onTogglePrivate: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                    ),
                    start = Offset(200f, 300f),
                    end = Offset(0f, 0f),
                ),
            )
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(LumiaDesign.radiusLg))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(onClick = onImageClick),
        ) {
            with(sharedTransitionScope) {
                AsyncImage(
                    model = detail.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = detailImageSharedTransitionKey(detail.id),
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            enter = fadeIn(tween(DetailImageSharedTransitionMs)),
                            exit = fadeOut(tween(DetailImageSharedTransitionMs)),
                            boundsTransform = DetailImageBoundsTransform,
                        ),
                    contentScale = ContentScale.Crop,
                )
            }
            ShimmerAuraOverlay(visible = loadingImage)
        }
    }

    val created = formattedCreatedDate(detail.imageUrl)
    Spacer(Modifier.height(20.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.created_on),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                created ?: "—",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        val pillBrush = etherealPrimaryGradientBrush()
        val pillShape = RoundedCornerShape(percent = 50)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .background(pillBrush)
                    .cloudShadow(pillShape, 8.dp)
                    .clickable(role = Role.Button) { onExport() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Download, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.export),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .background(pillBrush)
                    .cloudShadow(pillShape, 8.dp)
                    .clickable(role = Role.Button, onClick = onShare)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Share, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.share),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(
        stringResource(R.string.generation_prompt_heading),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(LumiaDesign.radiusLg))
            .background(LumiaDesign.GlassCard)
            .border(1.dp, LumiaDesign.GhostBorderLight, RoundedCornerShape(LumiaDesign.radiusLg)),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Text(
            "\"${detail.prompt}\"",
            style = MaterialTheme.typography.titleLarge.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                lineHeight = 26.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
        )
    }

    Spacer(Modifier.height(24.dp))
    Text(
        stringResource(R.string.parameters_heading),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        GalleryParamCell(
            stringResource(R.string.resolution),
            resolutionLabel(detail),
            Modifier.weight(1f),
        )
        GalleryParamCell(
            stringResource(R.string.quality),
            qualityLabelFromSteps(detail.steps),
            Modifier.weight(1f),
            valueColor = MaterialTheme.colorScheme.tertiary,
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        GalleryParamCell(
            stringResource(R.string.sampler_method),
            detail.sampling.ifBlank { "—" },
            Modifier.weight(1f),
        )
        GalleryParamCell(
            stringResource(R.string.seed),
            detail.seed.ifBlank { "—" },
            Modifier.weight(1f),
        )
    }

    if (onTogglePrivate != null) {
        Spacer(Modifier.height(20.dp))
        val privateButtonBrush = if (isFromPrivateSpace) {
            etherealPrimaryGradientBrush()
        } else {
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
        val privateIcon = if (isFromPrivateSpace) Icons.Filled.LockOpen else Icons.Filled.Lock
        val privateText = if (isFromPrivateSpace) {
            stringResource(R.string.remove_from_private)
        } else {
            stringResource(R.string.move_to_private)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(LumiaDesign.radiusLg))
                .background(privateButtonBrush)
                .cloudShadow(RoundedCornerShape(LumiaDesign.radiusLg), 16.dp)
                .clickable(role = Role.Button, onClick = onTogglePrivate),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                privateIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = privateText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp,
                ),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    if (showDelete && !isFromPrivateSpace) {
        Spacer(Modifier.height(28.dp))
        LumiaDestructiveGradientButton(
            text = stringResource(R.string.delete_artifact),
            onClick = onDelete,
            height = 64.dp,
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

/** Caption color — onSurfaceVariant with reduced emphasis to match M3 supporting text. */

@Composable
private fun GalleryParamCell(
    caption: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
) {
    val vc = valueColor ?: MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .clip(RoundedCornerShape(LumiaDesign.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = vc,
        )
    }
}

@Composable
private fun GenerationResultContent(
    detail: ResultDetail,
    loadingImage: Boolean,
    saveBanner: Boolean,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    onImageClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(32.dp))
            .cloudShadow(RoundedCornerShape(32.dp), 24.dp)
            .clickable(onClick = onImageClick),
    ) {
        with(sharedTransitionScope) {
            AsyncImage(
                model = detail.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = detailImageSharedTransitionKey(detail.id),
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(tween(DetailImageSharedTransitionMs)),
                        exit = fadeOut(tween(DetailImageSharedTransitionMs)),
                        boundsTransform = DetailImageBoundsTransform,
                    ),
                contentScale = ContentScale.Crop,
            )
        }
        ShimmerAuraOverlay(visible = loadingImage)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(LumiaDesign.radiusLg))
                .background(LumiaDesign.GlassCard)
                .border(1.dp, LumiaDesign.GhostBorderLight, RoundedCornerShape(LumiaDesign.radiusLg))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.prompt_applied).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    detail.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(percent = 50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    if (saveBanner) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LumiaDesign.radiusLg))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f), RoundedCornerShape(LumiaDesign.radiusLg)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.tertiary),
            )
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.image_saved_banner),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    ResultGradientButton(
        text = stringResource(R.string.save_to_device_upper),
        icon = { Icon(Icons.Filled.Download, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp)) },
        onClick = onSaveToGallery,
    )
    Spacer(Modifier.height(16.dp))
    ResultGradientButton(
        text = stringResource(R.string.share_masterpiece),
        icon = { Icon(Icons.Outlined.Share, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp)) },
        onClick = onShare,
    )
    Spacer(Modifier.height(24.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MetadataTile(
            label = stringResource(R.string.resolution),
            value = resolutionLabel(detail),
            modifier = Modifier.weight(1f),
        )
        MetadataTile(
            label = stringResource(R.string.metadata_model),
            value = detail.modelLabel.ifBlank { "—" },
            modifier = Modifier.weight(1f),
        )
    }
}

private fun resolutionLabel(detail: ResultDetail): String =
    if (detail.width > 0 && detail.height > 0) "${detail.width} × ${detail.height}" else "—"

private fun formattedCreatedDate(imageUrl: String): String? {
    if (!imageUrl.startsWith("file:")) return null
    return runCatching {
        val file = File(URI.create(imageUrl))
        if (!file.isFile) return null
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(file.lastModified()))
    }.getOrNull()
}

@Composable
private fun qualityLabelFromSteps(steps: String): String {
    val n = steps.toIntOrNull() ?: return "—"
    return stringResource(
        when {
            n >= 45 -> R.string.quality_high
            n >= 28 -> R.string.quality_med
            else -> R.string.quality_low
        },
    )
}

@Composable
private fun ResultGradientButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val brush = etherealPrimaryGradientBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(LumiaDesign.radiusLg))
            .background(brush)
            .cloudShadow(RoundedCornerShape(LumiaDesign.radiusLg), 20.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
            ),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun MetadataTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(LumiaDesign.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
