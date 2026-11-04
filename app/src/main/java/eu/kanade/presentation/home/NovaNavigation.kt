package eu.kanade.presentation.home

import kotlin.math.abs
import kotlin.math.round

enum class NovaDestination {
    Home,
    Discover,
    Library,
    Activity,
    Me,
}

enum class NovaNavigationLayout {
    BottomBar,
    Rail,
}

enum class NovaScreenMotion {
    SharedAxis,
    TitleDetail,
}

fun resolveNovaNavigationLayout(windowWidthDp: Int): NovaNavigationLayout =
    if (windowWidthDp >= 600) NovaNavigationLayout.Rail else NovaNavigationLayout.BottomBar

fun defaultNovaDestination(): NovaDestination = NovaDestination.Home

data class NovaLiquidIndicatorMetrics(
    val centerFraction: Float,
    val widthFraction: Float,
    val transitionPhase: Float,
) {
    companion object {
        val Zero = NovaLiquidIndicatorMetrics(
            centerFraction = 0f,
            widthFraction = 0f,
            transitionPhase = 0f,
        )
    }
}

fun resolveNovaLiquidIndicatorMetrics(
    position: Float,
    itemCount: Int,
): NovaLiquidIndicatorMetrics {
    if (itemCount <= 0) return NovaLiquidIndicatorMetrics.Zero

    val boundedPosition = position.coerceIn(0f, itemCount - 1f)
    val transitionPhase = (abs(boundedPosition - round(boundedPosition)) * 2f).coerceIn(0f, 1f)
    val cellFraction = 1f / itemCount

    return NovaLiquidIndicatorMetrics(
        centerFraction = (boundedPosition + 0.5f) * cellFraction,
        widthFraction = cellFraction * (0.72f + 0.22f * transitionPhase),
        transitionPhase = transitionPhase,
    )
}

fun resolveNovaScreenMotion(
    initialIsTitleDetail: Boolean,
    targetIsTitleDetail: Boolean,
): NovaScreenMotion = when {
    initialIsTitleDetail != targetIsTitleDetail -> NovaScreenMotion.TitleDetail
    else -> NovaScreenMotion.SharedAxis
}
