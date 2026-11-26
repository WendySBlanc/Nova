package eu.kanade.presentation.home

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

internal fun AnimatedContentTransitionScope<*>.novaInkHold(): ContentTransform =
    EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished

@Composable
internal fun Modifier.novaInkEntrance(
    scope: AnimatedVisibilityScope,
    fromLeft: Boolean,
    withSurface: Boolean = true,
): Modifier {
    val scheme = MaterialTheme.motionScheme
    val amount = scope.transition.animateFloat(
        transitionSpec = { scheme.fastEffectsSpec() },
        label = "nova-ink-entrance",
    ) { state -> if (state == EnterExitState.PreEnter) 0f else 1f }
    return novaPanelReveal(
        fromLeft = fromLeft,
        edgeColor = MaterialTheme.colorScheme.primary,
        surfaceColor = if (withSurface) MaterialTheme.colorScheme.background else Color.Transparent,
        subtle = true,
    ) {
        if (scope.transition.targetState == EnterExitState.PostExit) 1f else amount.value
    }
}

internal fun Modifier.novaPanelReveal(
    fromLeft: Boolean,
    edgeColor: Color = Color.White,
    surfaceColor: Color = Color.Transparent,
    subtle: Boolean = false,
    progress: () -> Float,
): Modifier = drawWithCache {
    val mask = Path()
    val edgeX = if (fromLeft) 0f else size.width
    val gutterWidth = (if (subtle) 5.dp else 8.dp).toPx()
    val strokeWidth = (if (subtle) 1.5.dp else 2.dp).toPx()
    val shadow = Brush.verticalGradient(0.48f to Color.Black, 0.88f to Color.Transparent)
    val paperEdge = Brush.verticalGradient(0.48f to edgeColor, 0.88f to Color.Transparent)

    onDrawWithContent {
        val fraction = progress().coerceIn(0f, 1f)
        if (fraction <= 0f) return@onDrawWithContent
        if (fraction >= 1f) {
            drawContent()
            return@onDrawWithContent
        }

        val top = Offset(size.width * novaPanelEdge(fraction, 0f, fromLeft), 0f)
        val bottom = Offset(size.width * novaPanelEdge(fraction, 1f, fromLeft), size.height)
        mask.reset()
        mask.moveTo(top.x, top.y)
        mask.lineTo(edgeX, 0f)
        mask.lineTo(edgeX, size.height)
        mask.lineTo(bottom.x, bottom.y)
        mask.close()
        clipPath(mask) {
            if (surfaceColor.alpha > 0f) drawRect(surfaceColor)
            this@onDrawWithContent.drawContent()
        }

        val energy = novaPanelEnergy(fraction)
        drawLine(shadow, top, bottom, strokeWidth = gutterWidth * energy, alpha = energy * 0.59f)
        drawLine(paperEdge, top, bottom, strokeWidth = strokeWidth * energy, alpha = energy * 0.94f)
    }
}
