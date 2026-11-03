package eu.kanade.presentation.home

enum class NovaDiscoverSection {
    Sources,
    Feed,
    Extensions,
    Migration,
}

enum class NovaMeQuickRoute {
    Downloads,
    Categories,
    Statistics,
    Settings,
}

fun novaDiscoverSections(
    feedEnabled: Boolean,
    feedFirst: Boolean,
): List<NovaDiscoverSection> = buildList {
    if (feedEnabled && feedFirst) add(NovaDiscoverSection.Feed)
    add(NovaDiscoverSection.Sources)
    if (feedEnabled && !feedFirst) add(NovaDiscoverSection.Feed)
    add(NovaDiscoverSection.Extensions)
    add(NovaDiscoverSection.Migration)
}

fun novaMeQuickRoutes(): List<NovaMeQuickRoute> = NovaMeQuickRoute.entries
