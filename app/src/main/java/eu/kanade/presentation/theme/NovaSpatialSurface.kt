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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.novaSpatialBackdrop(
    background: Color,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    wireColor: Color,
    wireAlpha: Float = 0.12f,
): Modifier = drawWithCache {
    val mesh = Brush.linearGradient(
        colorStops = arrayOf(
            0f to background,
            0.28f to primary.copy(alpha = 0.18f),
            0.54f to background,
            0.76f to secondary.copy(alpha = 0.12f),
            1f to tertiary.copy(alpha = 0.1f),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val wirePath = Path()
    val horizonY = size.height * 0.38f
    val vanishingX = size.width * 0.56f
    val verticalLines = 10
    val horizontalLines = 7

    for (index in 0..verticalLines) {
        val endX = size.width * index / verticalLines
        wirePath.moveTo(vanishingX, horizonY)
        wirePath.lineTo(endX, size.height)
    }
    for (index in 0..horizontalLines) {
        val fraction = index.toFloat() / horizontalLines
        val eased = fraction * fraction
        val y = horizonY + (size.height - horizonY) * eased
        wirePath.moveTo(0f, y)
        wirePath.lineTo(size.width, y)
    }

    onDrawBehind {
        drawRect(mesh)
        drawPath(
            path = wirePath,
            color = wireColor.copy(alpha = wireAlpha),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

fun Modifier.novaGlassSurface(
    color: Color,
    outline: Color,
    corner: Dp = NovaSignalDimensions.ControlCorner,
    outlineWidth: Dp = 1.dp,
): Modifier {
    val shape: Shape = RoundedCornerShape(corner)
    return clip(shape)
        .background(color.copy(alpha = 0.9f))
        .border(outlineWidth, outline.copy(alpha = 0.78f), shape)
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
