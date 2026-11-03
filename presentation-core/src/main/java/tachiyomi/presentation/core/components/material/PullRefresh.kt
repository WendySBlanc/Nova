package tachiyomi.presentation.core.components.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

internal sealed interface PullRefreshMotion {
    data object Hidden : PullRefreshMotion
    data class Pulling(val progress: Float) : PullRefreshMotion
    data object Refreshing : PullRefreshMotion
}

internal fun resolvePullRefreshMotion(
    distanceFraction: Float,
    refreshing: Boolean,
): PullRefreshMotion = when {
    refreshing -> PullRefreshMotion.Refreshing
    distanceFraction > 0f -> PullRefreshMotion.Pulling(distanceFraction.coerceIn(0f, 1f))
    else -> PullRefreshMotion.Hidden
}

/**
 * @param refreshing Whether the layout is currently refreshing
 * @param onRefresh Lambda which is invoked when a swipe to refresh gesture is completed.
 * @param enabled Whether the the layout should react to swipe gestures or not.
 * @param indicatorPadding Content padding for the indicator, to inset the indicator in if required.
 * @param content The content containing a vertically scrollable composable.
 */
@Composable
fun PullRefresh(
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    val motionProvider = remember(state, refreshing) {
        { resolvePullRefreshMotion(state.distanceFraction, refreshing) }
    }
    val refreshScale by animateFloatAsState(
        targetValue = if (refreshing) 1f else 0.84f,
        animationSpec = if (refreshing) {
            keyframes {
                durationMillis = 220
                0.84f at 0
                1.08f at 120
                1f at 220
            }
        } else {
            tween(durationMillis = 90)
        },
        label = "pull_refresh_scale",
    )
    val indicatorContainerColor by animateColorAsState(
        targetValue = if (refreshing) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 160),
        label = "pull_refresh_container_color",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (refreshing) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 160),
        label = "pull_refresh_content_color",
    )
    Box(
        modifier = modifier
            .pullToRefresh(
                isRefreshing = refreshing,
                state = state,
                enabled = enabled,
                onRefresh = onRefresh,
            ),
    ) {
        content()

        PullToRefreshDefaults.Indicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(indicatorPadding)
                .graphicsLayer {
                    when (val motion = motionProvider()) {
                        PullRefreshMotion.Hidden -> {
                            alpha = 0f
                            scaleX = 0.84f
                            scaleY = 0.84f
                        }
                        is PullRefreshMotion.Pulling -> {
                            alpha = motion.progress
                            val scale = 0.84f + (0.16f * motion.progress)
                            scaleX = scale
                            scaleY = scale
                        }
                        PullRefreshMotion.Refreshing -> {
                            alpha = 1f
                            scaleX = refreshScale
                            scaleY = refreshScale
                        }
                    }
                },
            isRefreshing = refreshing,
            state = state,
            containerColor = indicatorContainerColor,
            color = indicatorColor,
        )
    }
}
