package eu.kanade.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.home.resolveNovaLiquidIndicatorMetrics
import eu.kanade.presentation.theme.NovaSignalDimensions
import eu.kanade.presentation.theme.NovaSignalMotion
import kotlin.math.max

fun Modifier.novaHollowPacket(
    background: Color,
    outline: Color,
    accent: Color,
    secondary: Color,
    corner: Dp = NovaSignalDimensions.ControlCorner,
    particleCount: Int = 10,
): Modifier {
    val shape = RoundedCornerShape(corner)
    return clip(shape).drawWithCache {
        val strokeWidth = 1.dp.toPx()
        val accentStrokeWidth = 1.5.dp.toPx()
        val radius = corner.toPx()
        val boundedParticleCount = particleCount.coerceIn(0, 18)
        val particleX = FloatArray(boundedParticleCount)
        val particleY = FloatArray(boundedParticleCount)
        val particleRadius = FloatArray(boundedParticleCount)
        val frameBrush = Brush.horizontalGradient(listOf(accent, secondary))
        val framePath = Path().apply {
            moveTo(radius * 0.78f, strokeWidth / 2f)
            lineTo(size.width * 0.28f, strokeWidth / 2f)
            moveTo(size.width * 0.72f, strokeWidth / 2f)
            lineTo(size.width - radius * 0.78f, strokeWidth / 2f)
            moveTo(radius * 0.78f, size.height - strokeWidth / 2f)
            lineTo(size.width * 0.2f, size.height - strokeWidth / 2f)
            moveTo(size.width * 0.8f, size.height - strokeWidth / 2f)
            lineTo(size.width - radius * 0.78f, size.height - strokeWidth / 2f)
        }
        for (index in 0 until boundedParticleCount) {
            particleX[index] = size.width * (((index * 67 + 19) % 101) / 101f)
            particleY[index] = size.height * (0.18f + (((index * 43 + 11) % 67) / 67f) * 0.64f)
            particleRadius[index] = (0.55f + (index % 3) * 0.28f).dp.toPx()
        }

        onDrawBehind {
            drawRoundRect(
                color = background.copy(alpha = 0.82f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            )
            drawRoundRect(
                color = outline.copy(alpha = 0.62f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                style = Stroke(strokeWidth),
            )
            drawPath(
                path = framePath,
                brush = frameBrush,
                style = Stroke(accentStrokeWidth),
            )
            for (index in 0 until boundedParticleCount) {
                drawCircle(
                    color = if (index % 2 == 0) {
                        accent.copy(alpha = 0.2f)
                    } else {
                        secondary.copy(alpha = 0.14f)
                    },
                    radius = particleRadius[index],
                    center = Offset(particleX[index], particleY[index]),
                )
            }
        }
    }
}

@Composable
fun NovaLiquidSelectionIndicator(
    selectedIndex: Int,
    itemCount: Int,
    accent: Color,
    secondary: Color,
    outline: Color,
    modifier: Modifier = Modifier,
) {
    val animatedPosition = animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, max(0, itemCount - 1)).toFloat(),
        animationSpec = tween(
            durationMillis = NovaSignalMotion.Control,
            easing = FastOutSlowInEasing,
        ),
        label = "novaLiquidSelection",
    )

    Box(
        modifier = modifier.drawWithCache {
            val liquidPath = Path()
            val liquidBrush = Brush.horizontalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.24f),
                    secondary.copy(alpha = 0.16f),
                ),
            )
            val strokeWidth = 1.dp.toPx()

            onDrawBehind {
                val metrics = resolveNovaLiquidIndicatorMetrics(
                    position = animatedPosition.value,
                    itemCount = itemCount,
                )
                if (metrics.widthFraction <= 0f) return@onDrawBehind

                val centerX = size.width * metrics.centerFraction
                val width = size.width * metrics.widthFraction
                val top = size.height * 0.12f
                val bottom = size.height * 0.88f
                val height = bottom - top
                val left = centerX - width / 2f
                val right = centerX + width / 2f
                val middleY = (top + bottom) / 2f
                val cap = height * 0.46f
                val lift = height * 0.08f * metrics.transitionPhase

                liquidPath.rewind()
                liquidPath.moveTo(left + cap, top)
                liquidPath.cubicTo(
                    left + width * 0.34f,
                    top - lift,
                    right - width * 0.34f,
                    top - lift,
                    right - cap,
                    top,
                )
                liquidPath.cubicTo(right - cap * 0.18f, top, right, middleY - cap * 0.58f, right, middleY)
                liquidPath.cubicTo(right, middleY + cap * 0.58f, right - cap * 0.18f, bottom, right - cap, bottom)
                liquidPath.cubicTo(
                    right - width * 0.34f,
                    bottom + lift,
                    left + width * 0.34f,
                    bottom + lift,
                    left + cap,
                    bottom,
                )
                liquidPath.cubicTo(left + cap * 0.18f, bottom, left, middleY + cap * 0.58f, left, middleY)
                liquidPath.cubicTo(left, middleY - cap * 0.58f, left + cap * 0.18f, top, left + cap, top)
                liquidPath.close()

                drawPath(path = liquidPath, brush = liquidBrush)
                drawPath(
                    path = liquidPath,
                    color = outline.copy(alpha = 0.72f),
                    style = Stroke(strokeWidth),
                )

                val particleAlpha = 0.22f + metrics.transitionPhase * 0.3f
                drawCircle(
                    color = accent.copy(alpha = particleAlpha),
                    radius = 1.05.dp.toPx(),
                    center = Offset(right - cap * 0.2f, top + height * 0.22f),
                )
                drawCircle(
                    color = secondary.copy(alpha = particleAlpha * 0.78f),
                    radius = 0.8.dp.toPx(),
                    center = Offset(left + cap * 0.38f, bottom - height * 0.2f),
                )
                drawCircle(
                    color = accent.copy(alpha = particleAlpha * 0.62f),
                    radius = 0.62.dp.toPx(),
                    center = Offset(centerX + width * 0.18f, bottom - height * 0.12f),
                )
            }
        },
        content = {},
    )
}

@Composable
fun NovaFluidFixedTabRow(
    selectedIndex: Int,
    itemCount: Int,
    onSelect: (Int) -> Unit,
    background: Color,
    outline: Color,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    itemContent: @Composable BoxScope.(index: Int, selected: Boolean) -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = NovaSignalDimensions.MinimumTouchTarget)
            .novaHollowPacket(
                background = background,
                outline = outline,
                accent = accent,
                secondary = secondary,
            )
            .selectableGroup(),
    ) {
        NovaLiquidSelectionIndicator(
            selectedIndex = selectedIndex,
            itemCount = itemCount,
            accent = accent,
            secondary = secondary,
            outline = outline,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 5.dp),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(itemCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .selectable(
                            selected = selectedIndex == index,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                    content = { itemContent(index, selectedIndex == index) },
                )
            }
        }
    }
}

@Composable
fun NovaLiquidTabLabel(
    selected: Boolean,
    accent: Color,
    outline: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val selection = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(
            durationMillis = NovaSignalMotion.Control,
            easing = FastOutSlowInEasing,
        ),
        label = "novaLiquidTabLabel",
    )
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .drawWithCache {
                val radius = 8.dp.toPx()
                val strokeWidth = 1.dp.toPx()
                onDrawBehind {
                    val progress = selection.value
                    if (progress <= 0f) return@onDrawBehind
                    drawRoundRect(
                        color = accent.copy(alpha = 0.13f * progress),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                    )
                    drawRoundRect(
                        color = outline.copy(alpha = 0.5f * progress),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        style = Stroke(strokeWidth),
                    )
                    drawCircle(
                        color = accent.copy(alpha = 0.52f * progress),
                        radius = 0.85.dp.toPx(),
                        center = Offset(size.width * 0.84f, size.height * 0.24f),
                    )
                }
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
