package eu.kanade.tachiyomi.ui.reader.viewer.lnreader

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ReaderLensRefreshScriptTest {

    @Test
    fun `translated pages are overlaid without replacing the scrollable original`() {
        val script = buildReaderLensRefreshScript(
            mapOf(
                4 to ReaderLensPageSource(
                    url = "https://nova.local/reader-page/chapter/4?v=2",
                    translated = true,
                ),
            ),
        )

        script.shouldContain("nova-lens-overlay")
        script.shouldContain("source.translated")
        script.shouldContain("figure.appendChild(overlay)")
        script.shouldContain("\"translated\":true")
    }

    @Test
    fun `page refresh preserves the visible page anchor instead of forcing an absolute scroll position`() {
        val script = buildReaderLensRefreshScript(
            mapOf(0 to ReaderLensPageSource("https://nova.local/reader-page/chapter/0?v=1", false)),
        )

        script.shouldContain("anchorOffset")
        script.shouldContain("window.scrollBy")
        script.contains("window.scrollTo(0, scrollTop)").shouldBeFalse()
    }
}
