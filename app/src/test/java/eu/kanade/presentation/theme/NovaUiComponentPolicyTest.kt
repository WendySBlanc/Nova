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
