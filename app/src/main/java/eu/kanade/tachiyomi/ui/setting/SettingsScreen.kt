package eu.kanade.tachiyomi.ui.setting

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.SettingsAdvancedScreen
import eu.kanade.presentation.more.settings.screen.SettingsAppearanceScreen
import eu.kanade.presentation.more.settings.screen.SettingsBrowseScreen
import eu.kanade.presentation.more.settings.screen.SettingsConnectionScreen
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.presentation.more.settings.screen.SettingsDownloadScreen
import eu.kanade.presentation.more.settings.screen.SettingsEhScreen
import eu.kanade.presentation.more.settings.screen.SettingsLibraryScreen
import eu.kanade.presentation.more.settings.screen.SettingsMainScreen
import eu.kanade.presentation.more.settings.screen.SettingsMangadexScreen
import eu.kanade.presentation.more.settings.screen.SettingsReaderScreen
import eu.kanade.presentation.more.settings.screen.SettingsSearchScreen
import eu.kanade.presentation.more.settings.screen.SettingsSecurityScreen
import eu.kanade.presentation.more.settings.screen.SettingsTrackingScreen
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.util.DefaultNavigatorScreenTransition
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import tachiyomi.presentation.core.components.TwoPanelBox

class SettingsScreen(
    private val destination: Int? = null,
) : Screen() {

    constructor(destination: Destination) : this(destination.id)

    @Composable
    override fun Content() {
        val parentNavigator = LocalNavigator.currentOrThrow
        if (!isTabletUi()) {
            Navigator(
                screen = initialScreen(twoPane = false),
                content = {
                    val pop: () -> Unit = {
                        if (it.canPop) {
                            it.pop()
                        } else {
                            parentNavigator.pop()
                        }
                    }
                    CompositionLocalProvider(LocalBackPress provides pop) {
                        DefaultNavigatorScreenTransition(navigator = it)
                    }
                },
            )
        } else {
            Navigator(
                screen = initialScreen(twoPane = true),
            ) {
                val insets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                TwoPanelBox(
                    modifier = Modifier
                        .windowInsetsPadding(insets)
                        .consumeWindowInsets(insets),
                    startContent = {
                        CompositionLocalProvider(LocalBackPress provides parentNavigator::pop) {
                            SettingsMainScreen.Content(twoPane = true)
                        }
                    },
                    endContent = { DefaultNavigatorScreenTransition(navigator = it) },
                )
            }
        }
    }

    private fun initialScreen(twoPane: Boolean): VoyagerScreen {
        return when (destination) {
            Destination.Appearance.id -> SettingsAppearanceScreen
            Destination.Reader.id -> SettingsReaderScreen
            Destination.Library.id -> SettingsLibraryScreen
            Destination.Downloads.id -> SettingsDownloadScreen
            Destination.Browse.id -> SettingsBrowseScreen
            Destination.Tracking.id -> SettingsTrackingScreen
            Destination.Connections.id -> SettingsConnectionScreen
            Destination.DataAndStorage.id -> SettingsDataScreen
            Destination.Security.id -> SettingsSecurityScreen
            Destination.Ehentai.id -> SettingsEhScreen
            Destination.Mangadex.id -> SettingsMangadexScreen
            Destination.Advanced.id -> SettingsAdvancedScreen
            Destination.About.id -> AboutScreen()
            Destination.Search.id -> SettingsSearchScreen()
            else -> if (twoPane) SettingsAppearanceScreen else SettingsMainScreen
        }
    }

    sealed class Destination(val id: Int) {
        data object About : Destination(0)
        data object DataAndStorage : Destination(1)
        data object Tracking : Destination(2)
        data object Reader : Destination(3)
        data object Appearance : Destination(4)
        data object Library : Destination(5)
        data object Downloads : Destination(6)
        data object Browse : Destination(7)
        data object Connections : Destination(8)
        data object Security : Destination(9)
        data object Ehentai : Destination(10)
        data object Mangadex : Destination(11)
        data object Advanced : Destination(12)
        data object Search : Destination(13)
    }
}
