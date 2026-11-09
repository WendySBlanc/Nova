package eu.kanade.tachiyomi.ui.reader.viewer.lnreader

import eu.kanade.tachiyomi.data.lens.ReaderLensRegion
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class ReaderLensOverlayScriptTest {

    @Test
    fun `overlay text is encoded as JSON instead of executable javascript`() {
        val script = buildReaderLensOverlayScript(
            pageIndex = 4,
            regions = listOf(
                ReaderLensRegion(0.1f, 0.2f, 0.3f, 0.1f, "</script><script>alert('x')</script>"),
            ),
        )

        script shouldContain "data-page-index=\"4\""
        script shouldContain "nova-lens-text-layer"
        script shouldNotContain "</script><script>alert('x')</script>"
    }
}
