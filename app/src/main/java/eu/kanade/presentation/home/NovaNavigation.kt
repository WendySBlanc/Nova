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

fun resolveNovaNavigationLayout(windowWidthDp: Int): NovaNavigationLayout =
    if (windowWidthDp >= 600) NovaNavigationLayout.Rail else NovaNavigationLayout.BottomBar
