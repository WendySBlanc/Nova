package eu.kanade.presentation.home

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.math.abs

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

    @Test
    fun `liquid indicator stays centered on the selected navigation item`() {
        val metrics = resolveNovaLiquidIndicatorMetrics(
            position = 1f,
            itemCount = 5,
        )

        (abs(metrics.centerFraction - 0.3f) < 0.0001f) shouldBe true
        (abs(metrics.widthFraction - 0.144f) < 0.0001f) shouldBe true
        metrics.transitionPhase shouldBe 0f
    }

    @Test
    fun `liquid indicator stretches only while moving between items`() {
        val resting = resolveNovaLiquidIndicatorMetrics(position = 0f, itemCount = 2)
        val moving = resolveNovaLiquidIndicatorMetrics(position = 0.5f, itemCount = 2)

        (abs(resting.widthFraction - 0.36f) < 0.0001f) shouldBe true
        moving.centerFraction shouldBe 0.5f
        (abs(moving.widthFraction - 0.47f) < 0.0001f) shouldBe true
        moving.transitionPhase shouldBe 1f
    }

    @Test
    fun `liquid indicator clamps invalid positions and handles an empty row`() {
        val first = resolveNovaLiquidIndicatorMetrics(position = -2f, itemCount = 5)
        val last = resolveNovaLiquidIndicatorMetrics(position = 8f, itemCount = 5)

        (abs(first.centerFraction - 0.1f) < 0.0001f) shouldBe true
        (abs(last.centerFraction - 0.9f) < 0.0001f) shouldBe true
        resolveNovaLiquidIndicatorMetrics(position = 0f, itemCount = 0) shouldBe NovaLiquidIndicatorMetrics.Zero
    }
}
