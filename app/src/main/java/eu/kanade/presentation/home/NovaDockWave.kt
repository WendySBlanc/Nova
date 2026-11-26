package eu.kanade.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.novaReducedMotion
import kotlin.math.abs

@Composable
@ReadOnlyComposable
internal fun novaNavigationIndicatorColor(): Color = with(MaterialTheme.colorScheme) {
    primary.copy(alpha = 0.16f).compositeOver(surface)
}

@Composable
internal fun Modifier.novaDockWave(selectedIndex: Int, tabCount: Int, withLabels: Boolean): Modifier {
    val reducedMotion = novaReducedMotion()
    val scheme = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val fill = novaNavigationIndicatorColor()
    val rim = colors.primary.copy(alpha = 0.30f)
    val head = animateFloatAsState(selectedIndex.toFloat(), scheme.fastSpatialSpec(), label = "dock-wave-head")
    val tail = animateFloatAsState(selectedIndex.toFloat(), scheme.slowSpatialSpec(), label = "dock-wave-wake")

    return drawWithCache {
        val track = size.width / tabCount.coerceAtLeast(1)
        val radius = minOf(20.dp.toPx(), track * 0.41f, size.height / 2f)
        val cy = if (withLabels) minOf(21.dp.toPx(), size.height / 2f) else size.height / 2f
        val last = (tabCount - 1).coerceAtLeast(0).toFloat()
        val mirror = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
        val rimStroke = Stroke(1.dp.toPx())
        val wave = Path()

        onDrawBehind {
            val frontIndex = (if (reducedMotion) selectedIndex.toFloat() else head.value).coerceIn(0f, last)
            val frontX = ((if (mirror < 0f) last - frontIndex else frontIndex) + 0.5f) * track
            val wake = if (reducedMotion) 0f else novaDockWake(frontIndex, tail.value.coerceIn(0f, last)) * mirror
            val length = abs(wake) * track

            if (length < 0.5f) {
                val position = Offset(frontX, cy)
                drawCircle(fill, radius, position)
                drawCircle(rim, radius, position, style = rimStroke)
                return@onDrawBehind
            }

            val facing = if (wake < 0f) -1f else 1f
            val tailX = frontX - length * facing
            val tailRadius = radius * (1f - abs(wake) * 0.59f)
            val neckX = frontX - length * facing * 0.56f
            val neckRadius = tailRadius * 0.76f
            val frontBend = radius * 0.56f
            val tailBend = tailRadius * 0.56f
            val headTip = frontX + radius * facing
            val tailTip = tailX - tailRadius * facing

            wave.reset()
            wave.moveTo(frontX, cy - radius)
            wave.cubicTo(frontX + frontBend * facing, cy - radius, headTip, cy - frontBend, headTip, cy)
            wave.cubicTo(headTip, cy + frontBend, frontX + frontBend * facing, cy + radius, frontX, cy + radius)
            wave.cubicTo(neckX, cy + radius, neckX, cy + neckRadius, tailX, cy + tailRadius)
            wave.cubicTo(tailX - tailBend * facing, cy + tailRadius, tailTip, cy + tailBend, tailTip, cy)
            wave.cubicTo(tailTip, cy - tailBend, tailX - tailBend * facing, cy - tailRadius, tailX, cy - tailRadius)
            wave.cubicTo(neckX, cy - neckRadius, neckX, cy - radius, frontX, cy - radius)
            wave.close()

            drawPath(wave, fill)
            drawPath(wave, rim, style = rimStroke)
        }
    }
}
