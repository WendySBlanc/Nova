package eu.kanade.presentation.home

import eu.kanade.presentation.util.NovaSharedTransitionState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaNavigationTest {

    @Test
    fun `Nova exposes five stable primary destinations`() {
        NovaDestination.entries shouldContainExactly listOf(
            NovaDestination.Home,
            NovaDestination.Discover,
            NovaDestination.Library,
            NovaDestination.Activity,
            NovaDestination.Me,
        )
    }

    @Test
    fun `Nova starts on Home`() {
        defaultNovaDestination() shouldBe NovaDestination.Home
    }

    @Test
    fun `compact windows use bottom navigation`() {
        resolveNovaNavigationLayout(windowWidthDp = 599) shouldBe NovaNavigationLayout.BottomBar
    }

    @Test
    fun `medium and expanded windows use navigation rail`() {
        resolveNovaNavigationLayout(windowWidthDp = 600) shouldBe NovaNavigationLayout.Rail
        resolveNovaNavigationLayout(windowWidthDp = 840) shouldBe NovaNavigationLayout.Rail
    }

    @Test
    fun `navigation wave follows destination direction`() {
        resolveNovaNavigationDirection(previousIndex = 1, currentIndex = 4) shouldBe 1
        resolveNovaNavigationDirection(previousIndex = 4, currentIndex = 1) shouldBe -1
        resolveNovaNavigationDirection(previousIndex = 2, currentIndex = 2) shouldBe 0
    }

    @Test
    fun `horizontal motion mirrors in right to left layouts`() {
        resolveNovaNavigationDirection(previousIndex = 1, currentIndex = 4, isRtl = true) shouldBe -1
        resolveNovaNavigationDirection(previousIndex = 4, currentIndex = 1, isRtl = true) shouldBe 1
    }

    @Test
    fun `pager effects are bounded and go still when system motion is disabled`() {
        novaPagerMotionOffset(2, 2, 0.4f, isRtl = false, reducedMotion = false) shouldBe -0.4f
        // An outgoing layer can still draw its old page after the list shrinks.
        novaPagerMotionOffset(4, 1, 0f, isRtl = true, reducedMotion = false) shouldBe -1f
        novaPagerMotionOffset(2, 2, 0.4f, isRtl = false, reducedMotion = true) shouldBe 0f
    }

    @Test
    fun `carousel turns around instead of jumping across the full row`() {
        nextNovaCarouselPage(page = 0, pageCount = 4, direction = -1) shouldBe 1
        nextNovaCarouselPage(page = 3, pageCount = 4, direction = 1) shouldBe 2
        nextNovaCarouselPage(page = 2, pageCount = 4, direction = -1) shouldBe 1
        nextNovaCarouselPage(page = 0, pageCount = 1, direction = 1) shouldBe 0
    }

    @Test
    fun `a shared cover belongs to the selected title and opening screen only`() {
        val state = NovaSharedTransitionState()
        state.selectManga(12L, "cover", "Title", sourceKey = "home")

        state.hasArtworkFor(12L, "home") shouldBe true
        state.hasArtworkFor(14L, "home") shouldBe false
        state.hasArtworkFor(12L, "search") shouldBe false
        state.clearSelection()
        state.hasArtworkFor(12L, "home") shouldBe false
    }

    @Test
    fun `title details use one focused transition in both directions`() {
        resolveNovaScreenMotion(
            initialIsTitleDetail = false,
            targetIsTitleDetail = true,
        ) shouldBe NovaScreenMotion.TitleDetail
        resolveNovaScreenMotion(
            initialIsTitleDetail = true,
            targetIsTitleDetail = false,
        ) shouldBe NovaScreenMotion.TitleDetail
        resolveNovaScreenMotion(
            initialIsTitleDetail = false,
            targetIsTitleDetail = false,
        ) shouldBe NovaScreenMotion.SharedAxis
    }
}
