package eu.kanade.presentation.home

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

internal val LocalNovaDockBottomInset = staticCompositionLocalOf { 0.dp }

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

fun resolveNovaNavigationDirection(previousIndex: Int, currentIndex: Int, isRtl: Boolean = false): Int =
    currentIndex.compareTo(previousIndex) * if (isRtl) -1 else 1

internal fun novaPagerMotionOffset(
    page: Int,
    currentPage: Int,
    currentPageOffset: Float,
    isRtl: Boolean,
    reducedMotion: Boolean,
): Float {
    val offset = (page - currentPage) - currentPageOffset
    if (reducedMotion || offset == 0f) return 0f
    return offset.coerceIn(-1f, 1f) * if (isRtl) -1f else 1f
}

internal fun nextNovaCarouselPage(page: Int, pageCount: Int, direction: Int): Int {
    if (pageCount < 2) return 0
    return when {
        page <= 0 -> 1
        page >= pageCount - 1 -> pageCount - 2
        direction < 0 -> page - 1
        else -> page + 1
    }
}

fun defaultNovaDestination(): NovaDestination = NovaDestination.Home

fun resolveNovaScreenMotion(
    initialIsTitleDetail: Boolean,
    targetIsTitleDetail: Boolean,
): NovaScreenMotion = when {
    initialIsTitleDetail != targetIsTitleDetail -> NovaScreenMotion.TitleDetail
    else -> NovaScreenMotion.SharedAxis
}
