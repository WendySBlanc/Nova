package eu.kanade.presentation.theme

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import java.io.File

class NovaUiComponentPolicyTest {

    @Test
    fun `compact surfaces avoid fragile measurement components`() {
        source("app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt")
            .shouldNotContain("ButtonGroup(")
        source("app/src/main/java/eu/kanade/presentation/manga/NovaMangaScreen.kt")
            .shouldNotContain("HorizontalFloatingToolbar(")
    }

    @Test
    fun `shared indeterminate loading uses Material 3 expressive indicator`() {
        source("presentation-core/src/main/java/tachiyomi/presentation/core/screens/LoadingScreen.kt")
            .shouldContain("LoadingIndicator(")
        source("presentation-core/src/main/java/tachiyomi/presentation/core/screens/LoadingScreen.kt")
            .shouldNotContain("CircularProgressIndicator")
    }

    @Test
    fun `me exposes settings directly instead of opening a settings hub`() {
        val moreScreen = source("app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt")
        val moreTab = source("app/src/main/java/eu/kanade/tachiyomi/ui/more/MoreTab.kt")

        moreScreen.shouldContain("NovaMeSettingDestination.Appearance")
        moreScreen.shouldContain("NovaMeSettingDestination.Reader")
        moreScreen.shouldContain("NovaMeSettingDestination.Library")
        moreScreen.shouldContain("NovaMeSettingDestination.Downloads")
        moreScreen.shouldContain("NovaMeSettingDestination.DataAndStorage")
        moreScreen.shouldContain("NovaMeSettingDestination.Advanced")
        moreTab.shouldNotContain("navigator.push(SettingsScreen())")
    }

    @Test
    fun `settings destination is named settings everywhere`() {
        val moreScreen = source("app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt")
        val moreTab = source("app/src/main/java/eu/kanade/tachiyomi/ui/more/MoreTab.kt")

        moreScreen.shouldContain("text = \"Param\\u00e8tres\"")
        moreScreen.shouldNotContain("text = \"Moi\"")
        moreTab.shouldContain("title = \"Param\\u00e8tres\"")
        moreTab.shouldNotContain("title = \"Moi\"")
    }

    private fun source(relativePath: String): String = File(repositoryRoot(), relativePath).readText()

    private fun repositoryRoot(): File {
        val workingDirectory = requireNotNull(System.getProperty("user.dir"))
        var candidate = File(workingDirectory).canonicalFile
        while (!File(candidate, "settings.gradle.kts").isFile) {
            candidate = candidate.parentFile
                ?: error("Unable to locate repository root from $workingDirectory")
        }
        return candidate
    }
}
