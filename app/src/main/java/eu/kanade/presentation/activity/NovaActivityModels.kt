package eu.kanade.presentation.activity

enum class NovaDownloadQueueState {
    Queued,
    Running,
    Paused,
    Failed,
    Completed,
}

data class NovaDownloadQueueEntry(
    val id: Long,
    val state: NovaDownloadQueueState,
) {
    val canRetry: Boolean
        get() = state == NovaDownloadQueueState.Failed
}

data class NovaActivitySummary(
    val availableUpdates: Int,
    val activeDownloads: Int,
    val failedDownloads: Int,
    val recentHistoryCount: Int,
)

fun buildNovaActivitySummary(
    availableUpdates: Int,
    downloads: List<NovaDownloadQueueEntry>,
    recentHistoryCount: Int,
): NovaActivitySummary = NovaActivitySummary(
    availableUpdates = availableUpdates.coerceAtLeast(0),
    activeDownloads = downloads.count {
        it.state == NovaDownloadQueueState.Running || it.state == NovaDownloadQueueState.Queued
    },
    failedDownloads = downloads.count { it.state == NovaDownloadQueueState.Failed },
    recentHistoryCount = recentHistoryCount.coerceAtLeast(0),
)
