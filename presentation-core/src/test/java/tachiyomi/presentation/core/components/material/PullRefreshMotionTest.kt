package tachiyomi.presentation.core.components.material

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PullRefreshMotionTest {

    @Test
    fun `indicator follows pull progress and remains visible while refreshing`() {
        resolvePullRefreshMotion(distanceFraction = 0f, refreshing = false) shouldBe
            PullRefreshMotion.Hidden
        resolvePullRefreshMotion(distanceFraction = 0.45f, refreshing = false) shouldBe
            PullRefreshMotion.Pulling(progress = 0.45f)
        resolvePullRefreshMotion(distanceFraction = 1.4f, refreshing = false) shouldBe
            PullRefreshMotion.Pulling(progress = 1f)
        resolvePullRefreshMotion(distanceFraction = 0f, refreshing = true) shouldBe
            PullRefreshMotion.Refreshing
    }
}
