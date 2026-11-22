package eu.kanade.tachiyomi.ui.reader.viewer.lnreader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class ReaderImageLoadCoordinatorTest {

    @Test
    fun `slow page is delivered when it becomes ready after the old retry window`() = runTest {
        val page = ReaderPage(index = 7)
        var loadRequests = 0

        val result = async {
            awaitReaderImageLoad(page, timeoutMillis = 60_000) {
                loadRequests += 1
            }
        }
        runCurrent()

        loadRequests shouldBe 1
        advanceTimeBy(10_000)
        result.isCompleted shouldBe false

        page.stream = { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        page.status = Page.State.Ready
        runCurrent()

        result.await() shouldBe ReaderImageLoadResult.Ready
    }
}
