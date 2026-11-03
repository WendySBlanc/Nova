package eu.kanade.presentation.home

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaNavigationTest {

    @Test
    fun `Nova exposes four stable primary destinations`() {
        NovaDestination.entries shouldContainExactly listOf(
            NovaDestination.Library,
            NovaDestination.Discover,
            NovaDestination.Activity,
            NovaDestination.Me,
        )
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
    fun `title details open upward and return along the same path`() {
        resolveNovaScreenMotion(
            initialIsTitleDetail = false,
            targetIsTitleDetail = true,
            isPop = false,
        ) shouldBe NovaScreenMotion.TitleForward
        resolveNovaScreenMotion(
            initialIsTitleDetail = true,
            targetIsTitleDetail = false,
            isPop = true,
        ) shouldBe NovaScreenMotion.TitleBackward
        resolveNovaScreenMotion(
            initialIsTitleDetail = false,
            targetIsTitleDetail = false,
            isPop = false,
        ) shouldBe NovaScreenMotion.SharedAxis
    }
}
