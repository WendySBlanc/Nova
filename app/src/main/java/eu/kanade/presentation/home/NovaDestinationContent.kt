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

enum class NovaSettingsGroup(private vararg val routes: NovaMeSettingDestination) {
    Interface(NovaMeSettingDestination.Appearance),
    Reading(NovaMeSettingDestination.Reader, NovaMeSettingDestination.Library),
    Sources(NovaMeSettingDestination.Browse, NovaMeSettingDestination.Tracking, NovaMeSettingDestination.Connections),
    Storage(NovaMeSettingDestination.Downloads, NovaMeSettingDestination.DataAndStorage, NovaMeSettingDestination.Security),
    Advanced(NovaMeSettingDestination.Ehentai, NovaMeSettingDestination.Mangadex, NovaMeSettingDestination.Advanced),
    About(NovaMeSettingDestination.About),
    ;

    fun destinations(showEh: Boolean, showMangadex: Boolean): List<NovaMeSettingDestination> = routes.filter {
        (it != NovaMeSettingDestination.Ehentai || showEh) &&
            (it != NovaMeSettingDestination.Mangadex || showMangadex)
    }
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

fun novaMeSettingDestinations(): List<NovaMeSettingDestination> = NovaMeSettingDestination.entries
