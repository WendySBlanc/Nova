package eu.kanade.presentation.home

enum class NovaDestination {
    Library,
    Discover,
    Activity,
    Me,
}

enum class NovaNavigationLayout {
    BottomBar,
    Rail,
}

enum class NovaScreenMotion {
    SharedAxis,
    TitleForward,
    TitleBackward,
}

fun resolveNovaNavigationLayout(windowWidthDp: Int): NovaNavigationLayout =
    if (windowWidthDp >= 600) NovaNavigationLayout.Rail else NovaNavigationLayout.BottomBar

fun resolveNovaScreenMotion(
    initialIsTitleDetail: Boolean,
    targetIsTitleDetail: Boolean,
    isPop: Boolean,
): NovaScreenMotion = when {
    !initialIsTitleDetail && targetIsTitleDetail && !isPop -> NovaScreenMotion.TitleForward
    initialIsTitleDetail && !targetIsTitleDetail && isPop -> NovaScreenMotion.TitleBackward
    else -> NovaScreenMotion.SharedAxis
}
