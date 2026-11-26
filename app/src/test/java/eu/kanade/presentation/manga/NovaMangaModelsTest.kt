package eu.kanade.presentation.manga

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaMangaModelsTest {

    @Test
    fun `chapter search finds a formatted number without matching longer numbers`() {
        val search = NovaChapterSearch("3\u202F254")
        search.matches("Les tentacules de la guerre", 3254.0) shouldBe true
        search.matches("Chapitre 32540", 32540.0) shouldBe false
        NovaChapterSearch("12,5").matches("Interlude", 12.5) shouldBe true
    }

    @Test
    fun `chapter search ignores French accents and leading spaces`() {
        val search = NovaChapterSearch("  tenebres ")
        search.matches("Ténèbres vides", 3218.0) shouldBe true
        search.matches("D'autres chemins", 50.0) shouldBe false
        NovaChapterSearch(" ").matches("Chapitre sans numéro", -1.0) shouldBe true
    }

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

    @Test
    fun `chapter header summary stays compact and reflects filters`() {
        novaChapterHeaderSummary(
            chapterCount = 120,
            visibleChapterCount = 120,
            unreadCount = 7,
            filterActive = false,
        ) shouldBe "120 chapitres - 7 à lire"

        novaChapterHeaderSummary(
            chapterCount = 120,
            visibleChapterCount = 18,
            unreadCount = 7,
            filterActive = true,
        ) shouldBe "18 sur 120 affichés"
    }
}
