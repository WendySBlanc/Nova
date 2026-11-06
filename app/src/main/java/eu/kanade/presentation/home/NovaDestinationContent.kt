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
}

enum class NovaMeCategory(val label: String) {
    Overview("Aper\u00e7u"),
    Reading("Lecture"),
    Library("Biblioth\u00e8que"),
    Application("Param\u00e8tres"),
}

enum class NovaMeSettingDestination {
    Appearance,
    Reader,
    Library,
    Downloads,
    Browse,
    Tracking,
    Connections,
    DataAndStorage,
    Security,
    Ehentai,
    Mangadex,
    Advanced,
    About,
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

fun novaMeCategories(): List<NovaMeCategory> = NovaMeCategory.entries

fun novaMeSettingDestinations(): List<NovaMeSettingDestination> = NovaMeSettingDestination.entries
