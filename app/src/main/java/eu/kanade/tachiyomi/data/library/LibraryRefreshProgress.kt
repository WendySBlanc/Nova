package eu.kanade.tachiyomi.data.library

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

internal class LibraryRefreshProgress<T>(
    private val total: Int,
    private val publish: suspend (active: List<T>, completed: Int, total: Int) -> Unit,
) {
    private val mutex = Mutex()
    private val active = mutableListOf<T>()
    private var completed = 0

    suspend fun track(item: T, update: suspend () -> Unit) {
        mutex.withLock {
            active.add(item)
            publish(active.toList(), completed, total)
        }
        update()
        mutex.withLock {
            active.remove(item)
            completed++
            publish(active.toList(), completed, total)
        }
    }
}

internal class LibrarySourceTimeoutException(cause: Throwable? = null) : Exception(cause)

internal suspend fun <T : Any> withLibrarySourceDeadline(timeoutMillis: Long, fetch: suspend () -> T): T =
    try {
        withTimeoutOrNull(timeoutMillis) { fetch() } ?: throw LibrarySourceTimeoutException()
    } catch (e: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        throw LibrarySourceTimeoutException(e)
    }
