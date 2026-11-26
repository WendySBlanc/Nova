package eu.kanade.presentation.home

import eu.kanade.presentation.library.NovaLibraryEntry
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class NovaHomeModelsTest {

    @Test
    fun `empty library produces usable empty sections`() {
        deriveNovaHomeSections(emptyList()) shouldBe NovaHomeSections()
    }

    @Test
    fun `resume items are unique and ordered by last read`() {
        val sections = deriveNovaHomeSections(
            listOf(
                entry(id = 1, lastRead = 10),
                entry(id = 2, lastRead = 30),
                entry(id = 1, lastRead = 40),
                entry(id = 3),
            ),
        )

        sections.resume.map { it.id } shouldContainExactly listOf(2L, 1L)
    }

    @Test
    fun `new chapters keep successful partial refresh items`() {
        val sections = deriveNovaHomeSections(
            listOf(
                entry(id = 1, remaining = 2, latestUpload = 20),
                entry(id = 2, remaining = 1, latestUpload = 40),
                entry(id = 3, remaining = 0, latestUpload = 60),
            ),
        )

        sections.newChapters.map { it.id } shouldContainExactly listOf(2L, 1L)
    }

    @Test
    fun `recent items are ordered and bounded`() {
        val sections = deriveNovaHomeSections(
            items = listOf(
                entry(id = 1, dateAdded = 10),
                entry(id = 2, dateAdded = 30),
                entry(id = 3, dateAdded = 20),
            ),
            maxRecent = 2,
        )

        sections.recentlyAdded.map { it.id } shouldContainExactly listOf(2L, 3L)
    }

    @Test
    fun `home keeps priority and hides disabled or empty rows`() {
        val sections = NovaHomeSections(
            resume = listOf(entry(id = 3)),
            newChapters = listOf(entry(id = 1)),
            recentlyAdded = listOf(entry(id = 2)),
        )
        sections.visibleRows(showUpdates = true, showRecent = true, recentFirst = true)
            .map { it.first } shouldContainExactly listOf(NovaHomeRow.RecentlyAdded, NovaHomeRow.NewChapters)
        sections.visibleRows(showUpdates = false, showRecent = true, recentFirst = false)
            .map { it.first } shouldContainExactly listOf(NovaHomeRow.RecentlyAdded)
        sections.copy(recentlyAdded = emptyList()).visibleRows(showUpdates = false, showRecent = true, recentFirst = false)
            .map { it.first } shouldContainExactly emptyList()
        sections.featuredItems().map { it.id } shouldContainExactly listOf(3L)
        sections.copy(resume = emptyList()).featuredItems(maxItems = 1).map { it.id } shouldContainExactly listOf(1L)
    }

    @Test
    fun `recently completed titles never crowd out an older ongoing read`() {
        val items = (1L..12L).map { entry(id = it, lastRead = it, remaining = if (it == 1L) 1L else 0L) }

        deriveNovaHomeSections(items).featuredItems().map { it.id }
            .shouldContainExactly(listOf(1L, 12L, 11L, 10L, 9L))
    }

    @Test
    fun `resume preserves the active chapter even when earlier chapters were skipped`() {
        val skipped = Chapter.create().copy(id = 2L)
        val inProgress = Chapter.create().copy(id = 3L, lastPageRead = 8L)
        val next = Chapter.create().copy(id = 4L)

        selectNovaResumeChapter(listOf(skipped, inProgress, next), lastReadChapterId = 3L) shouldBe inProgress
        selectNovaResumeChapter(listOf(skipped, inProgress.copy(read = true), next), lastReadChapterId = 3L) shouldBe next
        selectNovaResumeChapter(listOf(skipped, inProgress), lastReadChapterId = 9L) shouldBe skipped
    }

    private fun entry(
        id: Long,
        lastRead: Long = 0,
        remaining: Long = 0,
        latestUpload: Long = 0,
        dateAdded: Long = 0,
    ) = NovaLibraryEntry(
        id = id,
        title = "Title $id",
        subtitle = "Subtitle",
        tag = "NOVA",
        remainingChapters = remaining,
        lastRead = lastRead,
        latestUpload = latestUpload,
        dateAdded = dateAdded,
    )
}
