package com.seagazer.aiimage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 135° gradient: primaryContainer → primary — follows current [MaterialTheme] (light/dark). */
@Composable
fun etherealPrimaryGradientBrush(): Brush = Brush.linearGradient(
    colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary),
    start = Offset.Zero,
    end = Offset(400f, 400f),
)

/** Cloud shadow: onSurface @ ~4 %, follows current [MaterialTheme]. */
@Composable
fun cloudShadowModifier(shape: Shape, elevation: Dp = 12.dp): Modifier {
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    return Modifier.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = tint,
        spotColor = tint,
    )
}

/** Non-composable overload kept for places where a static dark tint is acceptable (e.g. overlay). */
fun Modifier.cloudShadow(shape: Shape, elevation: Dp = 12.dp): Modifier =
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0xFF2C2F30).copy(alpha = 0.04f),
        spotColor = Color(0xFF2C2F30).copy(alpha = 0.04f),
    )

/**
 * Top bar container. [frosted] adds a light semi-transparent fill (blur is not supported uniformly in Compose).
 * App top bars use [frosted] = false so content shows through.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    frosted: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val fill = if (frosted) Color.White.copy(alpha = 0.92f) else Color.Transparent
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill),
        content = content,
    )
}
