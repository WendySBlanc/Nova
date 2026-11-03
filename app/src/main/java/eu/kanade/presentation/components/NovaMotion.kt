package eu.kanade.presentation.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import eu.kanade.presentation.theme.NovaSignalMotion

enum class NovaMotionSpeed(val durationMillis: Int) {
    Feedback(NovaSignalMotion.Feedback),
    Control(NovaSignalMotion.Control),
    Content(NovaSignalMotion.Content),
}

fun <T> novaMotionSpec(
    speed: NovaMotionSpeed = NovaMotionSpeed.Control,
    reduceMotion: Boolean = false,
): FiniteAnimationSpec<T> = tween(
    durationMillis = if (reduceMotion) 0 else speed.durationMillis,
    easing = FastOutSlowInEasing,
)
