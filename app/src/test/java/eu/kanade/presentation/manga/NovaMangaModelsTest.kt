package eu.kanade.presentation.manga

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaMangaModelsTest {

    @Test
    fun `primary action follows actual reading progress`() {
        resolvePrimaryReadAction(
            totalChapterCount = 12,
            unreadChapterCount = 12,
            hasPageProgress = false,
        ) shouldBe NovaPrimaryReadAction.Read

        resolvePrimaryReadAction(
            totalChapterCount = 12,
            unreadChapterCount = 8,
            hasPageProgress = true,
        ) shouldBe NovaPrimaryReadAction.Resume

        resolvePrimaryReadAction(
            totalChapterCount = 12,
            unreadChapterCount = 0,
            hasPageProgress = false,
        ) shouldBe NovaPrimaryReadAction.Reread
    }

    @Test
    fun `chapter state distinguishes unread progress and completed`() {
        resolveChapterReadingState(isRead = false, lastPageRead = 0L) shouldBe NovaChapterReadingState.Unread
        resolveChapterReadingState(isRead = false, lastPageRead = 7L) shouldBe NovaChapterReadingState.InProgress
        resolveChapterReadingState(isRead = true, lastPageRead = 0L) shouldBe NovaChapterReadingState.Read
    }

    @Test
    fun `local archive wins when restored download metadata is stale`() {
        resolveDownloadedState(
            isMarkedDownloaded = false,
            hasLocalArchive = true,
            isDownloadRunning = false,
            hasDownloadError = false,
        ) shouldBe NovaChapterDownloadState.Downloaded

        resolveDownloadedState(
            isMarkedDownloaded = false,
            hasLocalArchive = false,
            isDownloadRunning = true,
            hasDownloadError = false,
        ) shouldBe NovaChapterDownloadState.Downloading
    }
}
