package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import java.util.concurrent.atomic.AtomicInteger

class SearchRequestGateTest {

    @Test
    fun `network gate limits in-flight requests across suspension`() = runTest {
        val gate = SearchRequestGate(parallelism = 2, timeoutMillis = 1_000)
        val active = AtomicInteger()
        val maxActive = AtomicInteger()

        List(8) {
            async {
                gate.execute {
                    val current = active.incrementAndGet()
                    maxActive.updateAndGet { previous -> maxOf(previous, current) }
                    delay(100)
                    active.decrementAndGet()
                }
            }
        }.awaitAll()

        maxActive.get() shouldBeLessThanOrEqual 2
    }

    @Test
    fun `network gate times out a stalled source`() = runTest {
        val gate = SearchRequestGate(parallelism = 1, timeoutMillis = 50)

        shouldThrow<TimeoutCancellationException> {
            gate.execute { delay(1_000) }
        }
    }

    @Test
    fun `retry policy keeps only successful non-empty results`() {
        val success = SearchItemResult.Success(listOf(mockk<Manga>()))

        searchResultForRetry(success, sameQuery = true) shouldBe success
        searchResultForRetry(SearchItemResult.Success(emptyList()), sameQuery = true) shouldBe SearchItemResult.Loading
        searchResultForRetry(SearchItemResult.Error(IllegalStateException()), sameQuery = true) shouldBe SearchItemResult.Loading
        searchResultForRetry(success, sameQuery = false) shouldBe SearchItemResult.Loading
    }
}
