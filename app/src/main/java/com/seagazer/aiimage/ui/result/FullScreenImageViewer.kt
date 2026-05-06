@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.seagazer.aiimage.ui.result

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DetailImageSharedTransitionMs = 280

private const val DoubleTapZoomScale = 2.5f

private const val DoubleTapZoomAnimMs = 320

private val FullscreenDetailBoundsTransform = BoundsTransform { _, _ ->
    tween(DetailImageSharedTransitionMs, easing = FastOutSlowInEasing)
}

@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    /** Must match the detail screen image's [rememberSharedContentState] key for this artifact. */
    sharedImageTransitionKey: String,
    onDismiss: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity

    DisposableEffect(activity, view) {
        val window = activity?.window
        if (window == null) {
            return@DisposableEffect onDispose { }
        }
        val controller = WindowCompat.getInsetsController(window, view)
        val prevStatus = window.statusBarColor
        val prevNav = window.navigationBarColor
        val prevLightStatus = controller.isAppearanceLightStatusBars
        val prevLightNav = controller.isAppearanceLightNavigationBars
        onDispose {
            window.statusBarColor = prevStatus
            window.navigationBarColor = prevNav
            controller.isAppearanceLightStatusBars = prevLightStatus
            controller.isAppearanceLightNavigationBars = prevLightNav
        }
    }

    SideEffect {
        val window = activity?.window ?: return@SideEffect
        val bar = Color.Black.toArgb()
        window.statusBarColor = bar
        window.navigationBarColor = bar
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val scaleForGestures = rememberUpdatedState(scale)
    val zoomAnimJob = remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    var gesturesEnabled by remember { mutableStateOf(false) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        if (!gesturesEnabled) return@rememberTransformableState
        zoomAnimJob.value?.cancel()
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    fun layoutCenterOrNull(): Offset? {
        if (layoutSize.width <= 0 || layoutSize.height <= 0) return null
        return Offset(layoutSize.width / 2f, layoutSize.height / 2f)
    }

    fun animateZoomTo(targetScale: Float, doubleTapFocal: Offset? = null) {
        val t = targetScale.coerceIn(1f, 5f)
        val startScale = scale
        val startOffset = offset
        val c = layoutCenterOrNull()
        val useFocal = doubleTapFocal != null && c != null && t > startScale
        if (!useFocal) {
            if (abs(startScale - t) < 0.001f && abs(startOffset.x) < 0.01f && abs(startOffset.y) < 0.01f) {
                return
            }
        } else {
            if (abs(startScale - t) < 0.001f) return
        }
        zoomAnimJob.value?.cancel()
        zoomAnimJob.value = scope.launch {
            val spec = tween<Float>(durationMillis = DoubleTapZoomAnimMs, easing = FastOutSlowInEasing)
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spec,
            ) { frac, _ ->
                scale = startScale + (t - startScale) * frac
                offset = if (useFocal) {
                    val tap = requireNotNull(doubleTapFocal)
                    val ctr = requireNotNull(c)
                    Offset(
                        x = startOffset.x + (tap.x - ctr.x) * (startScale - scale),
                        y = startOffset.y + (tap.y - ctr.y) * (startScale - scale),
                    )
                } else {
                    Offset(
                        x = startOffset.x * (1f - frac),
                        y = startOffset.y * (1f - frac),
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(DetailImageSharedTransitionMs.toLong())
        gesturesEnabled = true
    }

    fun performDismiss() {
        scale = 1f
        offset = Offset.Zero
        view.post { onDismiss() }
    }

    BackHandler(onBack = { performDismiss() })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        with(sharedTransitionScope) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { layoutSize = it }
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = sharedImageTransitionKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                        enter = fadeIn(tween(DetailImageSharedTransitionMs)),
                        exit = fadeOut(tween(DetailImageSharedTransitionMs)),
                        boundsTransform = FullscreenDetailBoundsTransform,
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .then(
                        if (gesturesEnabled) {
                            Modifier.transformable(state = transformableState)
                        } else {
                            Modifier
                        },
                    )
                    .pointerInput(gesturesEnabled) {
                        if (!gesturesEnabled) return@pointerInput
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                val s = scaleForGestures.value
                                if (s <= 1.01f) {
                                    animateZoomTo(DoubleTapZoomScale, doubleTapFocal = tapOffset)
                                } else {
                                    animateZoomTo(1f)
                                }
                            },
                            onTap = {
                                val s = scaleForGestures.value
                                if (s <= 1.01f) {
                                    performDismiss()
                                } else {
                                    animateZoomTo(1f)
                                }
                            },
                        )
                    },
            )
        }
    }
}
