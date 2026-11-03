package eu.kanade.presentation.activity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaActivityModelsTest {

    @Test
    fun `activity summary aggregates actionable counts`() {
        buildNovaActivitySummary(
            availableUpdates = 4,
            downloads = listOf(
                NovaDownloadQueueEntry(1L, NovaDownloadQueueState.Running),
                NovaDownloadQueueEntry(2L, NovaDownloadQueueState.Failed),
                NovaDownloadQueueEntry(3L, NovaDownloadQueueState.Queued),
            ),
            recentHistoryCount = 7,
        ) shouldBe NovaActivitySummary(
            availableUpdates = 4,
            activeDownloads = 2,
            failedDownloads = 1,
            recentHistoryCount = 7,
        )
    }

    @Test
    fun `failed downloads remain retryable and queue order stays stable`() {
        val downloads = listOf(
            NovaDownloadQueueEntry(9L, NovaDownloadQueueState.Failed),
            NovaDownloadQueueEntry(3L, NovaDownloadQueueState.Queued),
        )

        downloads.filter { it.canRetry }.map { it.id } shouldBe listOf(9L)
        downloads.map { it.id } shouldBe listOf(9L, 3L)
    }
}
