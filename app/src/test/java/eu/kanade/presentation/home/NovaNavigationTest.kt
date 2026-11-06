package eu.kanade.presentation.home

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
