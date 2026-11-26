package eu.kanade.presentation.home

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaSignatureGeometryTest {
    @Test
    fun `cut leaves one whole cover at each settled position`() {
        for (y in listOf(0f, 0.5f, 1f)) {
            novaPanelEdge(0f, y, isRtl = false) shouldBe 1f
            novaPanelEdge(1f, y, isRtl = false) shouldBe 0f
        }
    }

    @Test
    fun `cut is diagonal and mirrors in right to left layouts`() {
        val top = novaPanelEdge(0.5f, 0f, isRtl = false)
        val bottom = novaPanelEdge(0.5f, 1f, isRtl = false)
        (top > bottom) shouldBe true
        novaPanelEdge(0.5f, 0f, isRtl = true) shouldBe 1f - top
        novaPanelEdge(0.5f, 1f, isRtl = true) shouldBe 1f - bottom
    }

    @Test
    fun `sweeping cut never reverses or leaves an empty frame`() {
        for (y in listOf(0f, 0.5f, 1f)) {
            val positions = (0..100).map { novaPanelEdge(it / 100f, y, isRtl = false) }
            positions.all { it in 0f..1f } shouldBe true
            positions.zipWithNext().all { (start, end) -> start >= end } shouldBe true
        }
    }

    @Test
    fun `dock wake stays bounded even across multiple destinations`() {
        novaDockWake(head = 4f, tail = 0f) shouldBe 0.92f
        novaDockWake(head = 0f, tail = 4f) shouldBe -0.92f
        novaDockWake(head = 2f, tail = 2f) shouldBe 0f
    }
}
