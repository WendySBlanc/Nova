package eu.kanade.presentation.home

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

fun resolveNovaScreenMotion(
    initialIsTitleDetail: Boolean,
    targetIsTitleDetail: Boolean,
): NovaScreenMotion = when {
    initialIsTitleDetail != targetIsTitleDetail -> NovaScreenMotion.TitleDetail
    else -> NovaScreenMotion.SharedAxis
}
