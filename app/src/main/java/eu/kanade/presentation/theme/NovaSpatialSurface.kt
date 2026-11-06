package eu.kanade.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Suppress("UNUSED_PARAMETER")
fun Modifier.novaSpatialBackdrop(
    background: Color,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    wireColor: Color,
    wireAlpha: Float = 0.12f,
    planeAlpha: Float = 0.1f,
    colorIntensity: Float = 1f,
    showMark: Boolean = true,
): Modifier = background(background)

fun Modifier.novaGlassSurface(
    color: Color,
    outline: Color,
    corner: Dp = NovaSignalDimensions.ControlCorner,
    outlineWidth: Dp = 1.dp,
): Modifier {
    val shape: Shape = RoundedCornerShape(corner)
    return clip(shape)
        .background(color)
        .border(outlineWidth, outline.copy(alpha = 0.68f), shape)
}

fun Modifier.novaCoverHeroGradient(
    palette: NovaCoverGradientPalette,
): Modifier = drawWithCache {
    val coverWash = Brush.horizontalGradient(
        colorStops = arrayOf(
            0f to palette.contentScrim,
            0.52f to palette.primary.copy(alpha = 0.46f),
            1f to Color.Transparent,
        ),
    )
    val readabilityScrim = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.Black.copy(alpha = 0.12f),
            0.48f to Color.Black.copy(alpha = 0.28f),
            0.82f to palette.background.copy(alpha = 0.9f),
            1f to palette.background,
        ),
    )

    onDrawBehind {
        drawRect(coverWash)
        drawRect(readabilityScrim)
    }
}

fun Modifier.novaNeonProgress(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    thickness: Dp = 3.dp,
): Modifier = drawWithCache {
    val strokeWidth = thickness.toPx()
    val y = size.height - strokeWidth / 2f
    val boundedProgress = progress.coerceIn(0f, 1f)

    onDrawWithContent {
        drawContent()
        drawLine(
            color = trackColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = progressColor,
            start = Offset(0f, y),
            end = Offset(size.width * boundedProgress, y),
            strokeWidth = strokeWidth,
        )
    }
}
