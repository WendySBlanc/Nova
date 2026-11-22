package eu.kanade.tachiyomi.ui.reader.viewer.lnreader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal sealed interface ReaderImageLoadResult {
    data object Ready : ReaderImageLoadResult
    data class Failed(val error: Throwable) : ReaderImageLoadResult
    data object TimedOut : ReaderImageLoadResult
}

internal suspend fun awaitReaderImageLoad(
    page: ReaderPage,
    timeoutMillis: Long,
    requestLoad: () -> Unit,
): ReaderImageLoadResult {
    if (page.status == Page.State.Ready && page.stream != null) {
        return ReaderImageLoadResult.Ready
    }

    requestLoad()
    return withTimeoutOrNull(timeoutMillis) {
        when (
            val status = page.statusFlow.first {
                (it == Page.State.Ready && page.stream != null) || it is Page.State.Error
            }
        ) {
            Page.State.Ready -> ReaderImageLoadResult.Ready
            is Page.State.Error -> ReaderImageLoadResult.Failed(status.error)
            else -> error("Unexpected reader image state: $status")
        }
    } ?: ReaderImageLoadResult.TimedOut
}
