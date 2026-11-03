package eu.kanade.tachiyomi.data.backup.restore.restorers

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class RestoredChapterMergeTest {

    @Test
    fun `standard restore never reduces local reading progress`() {
        val backupChapter = chapter(
            id = -1L,
            read = false,
            bookmark = true,
            lastPageRead = 5L,
            sourceOrder = 3L,
        )
        val localChapter = chapter(
            id = 42L,
            read = true,
            bookmark = false,
            lastPageRead = 20L,
            sourceOrder = 8L,
        )

        val merged = mergeRestoredChapter(backupChapter, localChapter, isSync = false)

        merged.id shouldBe 42L
        merged.read shouldBe true
        merged.bookmark shouldBe true
        merged.lastPageRead shouldBe 20L
        merged.sourceOrder shouldBe 8L
    }

    @Test
    fun `standard restore keeps newer backup progress`() {
        val backupChapter = chapter(lastPageRead = 30L)
        val localChapter = chapter(id = 42L, lastPageRead = 20L)

        val merged = mergeRestoredChapter(backupChapter, localChapter, isSync = false)

        merged.lastPageRead shouldBe 30L
    }

    @Test
    fun `sync restore keeps the incoming read state and progress`() {
        val backupChapter = chapter(
            read = false,
            bookmark = false,
            lastPageRead = 5L,
            sourceOrder = 3L,
            dateUpload = 2_000L,
        )
        val localChapter = chapter(
            id = 42L,
            read = true,
            bookmark = true,
            lastPageRead = 20L,
            sourceOrder = 8L,
            dateUpload = 1_000L,
        )

        val merged = mergeRestoredChapter(backupChapter, localChapter, isSync = true)

        merged.id shouldBe 42L
        merged.read shouldBe false
        merged.bookmark shouldBe true
        merged.lastPageRead shouldBe 5L
        merged.sourceOrder shouldBe 8L
        merged.dateUpload shouldBe 1_000L
    }

    private fun chapter(
        id: Long = -1L,
        read: Boolean = false,
        bookmark: Boolean = false,
        lastPageRead: Long = 0L,
        sourceOrder: Long = 0L,
        dateUpload: Long = 1_000L,
    ) = Chapter.create().copy(
        id = id,
        mangaId = 1L,
        url = "/chapter-1",
        name = "Chapter 1",
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        sourceOrder = sourceOrder,
        dateUpload = dateUpload,
    )
}
