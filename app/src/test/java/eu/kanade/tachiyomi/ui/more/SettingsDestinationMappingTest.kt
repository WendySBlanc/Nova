package eu.kanade.tachiyomi.ui.more

import eu.kanade.presentation.home.NovaMeSettingDestination
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SettingsDestinationMappingTest {

    @Test
    fun `every direct settings shortcut keeps the adaptive settings container`() {
        val expected = mapOf(
            NovaMeSettingDestination.Appearance to SettingsScreen.Destination.Appearance,
            NovaMeSettingDestination.Reader to SettingsScreen.Destination.Reader,
            NovaMeSettingDestination.Library to SettingsScreen.Destination.Library,
            NovaMeSettingDestination.Downloads to SettingsScreen.Destination.Downloads,
            NovaMeSettingDestination.Browse to SettingsScreen.Destination.Browse,
            NovaMeSettingDestination.Tracking to SettingsScreen.Destination.Tracking,
            NovaMeSettingDestination.Connections to SettingsScreen.Destination.Connections,
            NovaMeSettingDestination.DataAndStorage to SettingsScreen.Destination.DataAndStorage,
            NovaMeSettingDestination.Security to SettingsScreen.Destination.Security,
            NovaMeSettingDestination.Ehentai to SettingsScreen.Destination.Ehentai,
            NovaMeSettingDestination.Mangadex to SettingsScreen.Destination.Mangadex,
            NovaMeSettingDestination.Advanced to SettingsScreen.Destination.Advanced,
            NovaMeSettingDestination.About to SettingsScreen.Destination.About,
        )

        NovaMeSettingDestination.entries.associateWith { it.toSettingsDestination() } shouldBe expected
    }
}
