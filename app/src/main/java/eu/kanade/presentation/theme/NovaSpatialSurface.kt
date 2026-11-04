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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
): Modifier = drawWithCache {
    val boundedColorIntensity = colorIntensity.coerceIn(0f, 4f)
    val mesh = Brush.linearGradient(
        colorStops = arrayOf(
            0f to background,
            0.28f to primary.copy(alpha = 0.11f * boundedColorIntensity),
            0.54f to background,
            0.76f to secondary.copy(alpha = 0.08f * boundedColorIntensity),
            1f to tertiary.copy(alpha = 0.07f * boundedColorIntensity),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val wirePath = Path()
    val orthographicPath = Path()
    val markPath = Path()
    val horizonY = size.height * 0.38f
    val vanishingX = size.width * 0.56f
    val verticalLines = 10
    val horizontalLines = 7
    val gridStep = 56.dp.toPx()

    var gridX = 0f
    while (gridX <= size.width) {
        orthographicPath.moveTo(gridX, 0f)
        orthographicPath.lineTo(gridX, horizonY)
        gridX += gridStep
    }
    var gridY = 0f
    while (gridY <= horizonY) {
        orthographicPath.moveTo(0f, gridY)
        orthographicPath.lineTo(size.width, gridY)
        gridY += gridStep
    }

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

    val markWidth = size.minDimension * 0.46f
    val markCenterX = size.width * 0.68f
    val markTop = size.height * 0.09f
    val markBottom = markTop + markWidth * 0.92f
    markPath.moveTo(markCenterX, markTop)
    markPath.lineTo(markCenterX - markWidth / 2f, markBottom)
    markPath.lineTo(markCenterX + markWidth / 2f, markBottom)
    markPath.close()

    onDrawBehind {
        drawRect(mesh)
        drawPath(
            path = orthographicPath,
            color = wireColor.copy(alpha = wireAlpha * 0.55f),
            style = Stroke(width = 1.dp.toPx()),
        )
        drawPath(
            path = wirePath,
            color = wireColor.copy(alpha = wireAlpha),
            style = Stroke(width = 1.dp.toPx()),
        )
        rotate(degrees = -12f, pivot = Offset(size.width * 0.2f, size.height * 0.3f)) {
            drawRect(
                color = secondary.copy(alpha = planeAlpha),
                topLeft = Offset(-size.width * 0.12f, size.height * 0.2f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.55f, size.height * 0.12f),
            )
        }
        rotate(degrees = 9f, pivot = Offset(size.width * 0.82f, size.height * 0.64f)) {
            drawRect(
                color = tertiary.copy(alpha = planeAlpha * 0.8f),
                topLeft = Offset(size.width * 0.54f, size.height * 0.58f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.58f, size.height * 0.09f),
            )
        }
        if (showMark) {
            drawPath(
                path = markPath,
                color = primary.copy(alpha = wireAlpha * 1.45f),
                style = Stroke(width = 1.25.dp.toPx()),
            )
            drawLine(
                color = secondary.copy(alpha = wireAlpha),
                start = Offset(markCenterX, markTop),
                end = Offset(markCenterX, markBottom),
                strokeWidth = 1.dp.toPx(),
            )
        }
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
