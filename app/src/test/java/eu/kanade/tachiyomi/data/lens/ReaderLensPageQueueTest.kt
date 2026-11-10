package eu.kanade.tachiyomi.data.lens

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderLensPageQueueTest {

    @Test
    fun `visible page is processed before its prefetch page`() {
        val queue = ReaderLensPageQueue()

        queue.offerVisible(pageIndex = 5, nextPageIndex = 6)

        queue.poll() shouldBe 5
        queue.poll() shouldBe 6
        queue.poll() shouldBe null
    }

    @Test
    fun `new visible page discards obsolete queued work`() {
        val queue = ReaderLensPageQueue()

        queue.offerVisible(pageIndex = 2, nextPageIndex = 3)
        queue.offerVisible(pageIndex = 3, nextPageIndex = 4)

        queue.poll() shouldBe 3
        queue.poll() shouldBe 4
        queue.poll() shouldBe null
    }

    @Test
    fun `completed pages are not queued again`() {
        val queue = ReaderLensPageQueue()
        queue.markCompleted(8)

        queue.offerVisible(pageIndex = 8, nextPageIndex = 9)

        queue.poll() shouldBe 9
    }
}
