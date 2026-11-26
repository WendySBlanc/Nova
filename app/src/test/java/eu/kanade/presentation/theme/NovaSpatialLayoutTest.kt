package eu.kanade.presentation.theme

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaSpatialLayoutTest {

    @Test
    fun `phones use two gallery columns and wider screens keep denser layouts`() {
        novaSpatialLibraryColumns(320) shouldBe 2
        novaSpatialLibraryColumns(360) shouldBe 2
        novaSpatialLibraryColumns(599) shouldBe 2
        novaSpatialLibraryColumns(600) shouldBe 3
        novaSpatialLibraryColumns(840) shouldBe 4
    }

    @Test
    fun `library becomes a split workspace at expanded width`() {
        novaSpatialLibraryLayout(839) shouldBe NovaSpatialLibraryLayout.Compact
        novaSpatialLibraryLayout(840) shouldBe NovaSpatialLibraryLayout.Split
    }

    @Test
    fun `editorial carousel matches the reference on a 375 dp screen`() {
        novaSpatialCarouselCardWidthDp(375) shouldBe 327
        novaSpatialCarouselHeightDp(375) shouldBe 370
    }

    @Test
    fun `editorial carousel keeps its aspect ratio on narrow and wide screens`() {
        novaSpatialCarouselCardWidthDp(320) shouldBe 279
        novaSpatialCarouselHeightDp(320) shouldBe 316
        novaSpatialCarouselCardWidthDp(432) shouldBe 377
        novaSpatialCarouselHeightDp(432) shouldBe 427
        novaSpatialCarouselCardWidthDp(700) shouldBe 420
        novaSpatialCarouselHeightDp(700) shouldBe 475
    }

    @Test
    fun `carousel keeps positive constraints if measured in an empty stage`() {
        novaSpatialCarouselCardWidthDp(0) shouldBe 1
        novaSpatialCarouselHeightDp(0) shouldBe 1
    }
}
