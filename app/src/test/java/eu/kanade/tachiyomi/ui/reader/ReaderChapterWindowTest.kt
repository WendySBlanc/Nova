package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class ReaderChapterWindowTest {

    @Test
    fun `only latest chapter load request may update reader`() {
        val gate = ChapterLoadRequestGate()
        val firstRequest = gate.next()
        val secondRequest = gate.next()

        gate.isCurrent(firstRequest) shouldBe false
        gate.isCurrent(secondRequest) shouldBe true
    }

    @Test
    fun `regular load cancels an adjacent loading indicator`() {
        val gate = ChapterLoadRequestGate()
        val adjacentRequest = gate.next(isAdjacent = true)

        gate.isAdjacentLoading() shouldBe true

        gate.next()
        gate.complete(adjacentRequest)

        gate.isAdjacentLoading() shouldBe false
    }

    @Test
    fun `older adjacent load cannot clear a newer adjacent loading indicator`() {
        val gate = ChapterLoadRequestGate()
        val firstRequest = gate.next(isAdjacent = true)
        val secondRequest = gate.next(isAdjacent = true)

        gate.complete(firstRequest)
        gate.isAdjacentLoading() shouldBe true

        gate.complete(secondRequest)
        gate.isAdjacentLoading() shouldBe false
    }

    @Test
    fun `finds adjacent chapters by stable chapter id`() {
        val chapters = listOf(readerChapter(1L), readerChapter(2L), readerChapter(3L))
        val reloadedCurrentChapter = readerChapter(2L)

        val window = buildViewerChapters(chapters, reloadedCurrentChapter)

        window.currChapter shouldBe reloadedCurrentChapter
        window.prevChapter?.chapter?.id shouldBe 1L
        window.nextChapter?.chapter?.id shouldBe 3L
    }

    @Test
    fun `does not invent adjacent chapters when current chapter is absent`() {
        val chapters = listOf(readerChapter(1L), readerChapter(2L), readerChapter(3L))
        val missingCurrentChapter = readerChapter(99L)

        val window = buildViewerChapters(chapters, missingCurrentChapter)

        window.currChapter shouldBe missingCurrentChapter
        window.prevChapter shouldBe null
        window.nextChapter shouldBe null
    }

    @Test
    fun `keeps list boundaries when selecting first and last chapters`() {
        val chapters = listOf(readerChapter(1L), readerChapter(2L), readerChapter(3L))

        val firstWindow = buildViewerChapters(chapters, chapters.first())
        val lastWindow = buildViewerChapters(chapters, chapters.last())

        firstWindow.prevChapter shouldBe null
        firstWindow.nextChapter?.chapter?.id shouldBe 2L
        lastWindow.prevChapter?.chapter?.id shouldBe 2L
        lastWindow.nextChapter shouldBe null
    }

    private fun readerChapter(id: Long) = ReaderChapter(
        Chapter.create().copy(
            id = id,
            mangaId = 1L,
            url = "/chapter-$id",
            name = "Chapter $id",
        ),
    )
}
