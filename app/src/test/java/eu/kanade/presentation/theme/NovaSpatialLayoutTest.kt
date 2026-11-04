package eu.kanade.presentation.theme

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaSpatialLayoutTest {

    @Test
    fun `standard phones use a three column library`() {
        novaSpatialLibraryColumns(320) shouldBe 2
        novaSpatialLibraryColumns(360) shouldBe 3
        novaSpatialLibraryColumns(599) shouldBe 3
    }

    @Test
    fun `library becomes a split workspace at expanded width`() {
        novaSpatialLibraryLayout(839) shouldBe NovaSpatialLibraryLayout.Compact
        novaSpatialLibraryLayout(840) shouldBe NovaSpatialLibraryLayout.Split
    }

    @Test
    fun `home bento grid grows with available width`() {
        novaSpatialHomeColumns(360) shouldBe 2
        novaSpatialHomeColumns(720) shouldBe 3
        novaSpatialHomeColumns(1200) shouldBe 4
    }
}
