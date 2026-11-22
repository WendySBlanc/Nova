package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

internal class SearchRequestGate(
    parallelism: Int,
    private val timeoutMillis: Long,
) {
    private val semaphore = Semaphore(parallelism)

    init {
        require(parallelism > 0)
        require(timeoutMillis > 0)
    }

    suspend fun <T> execute(block: suspend () -> T): T {
        return semaphore.withPermit {
            withTimeout(timeoutMillis) { block() }
        }
    }
}
