package eu.kanade.presentation.theme

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
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

    @Test
    fun `carousel height remains compact across window sizes`() {
        novaSpatialCarouselHeightDp(320) shouldBe 220
        novaSpatialCarouselHeightDp(380) shouldBe 232
        novaSpatialCarouselHeightDp(700) shouldBe 248
        novaSpatialCarouselHeightDp(-1) shouldBe 220
    }

    @Test
    fun `carousel width dynamically reveals more items as space grows`() {
        val compact = novaSpatialCarouselPreferredWidthDp(320, itemCount = 5)
        val medium = novaSpatialCarouselPreferredWidthDp(700, itemCount = 5)
        val expanded = novaSpatialCarouselPreferredWidthDp(1200, itemCount = 5)

        compact.shouldBeGreaterThan(0)
        compact.shouldBeLessThanOrEqual(320)
        medium.shouldBeLessThanOrEqual(700)
        expanded.shouldBeLessThanOrEqual(1200)
        ((320f / compact) < (700f / medium)) shouldBe true
        ((700f / medium) < (1200f / expanded)) shouldBe true
    }

    @Test
    fun `single carousel item uses the available stage`() {
        novaSpatialCarouselPreferredWidthDp(380, itemCount = 1) shouldBe 356
        novaSpatialCarouselPreferredWidthDp(0, itemCount = 0) shouldBe 1
    }
}
