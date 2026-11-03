package eu.kanade.presentation.home

import eu.kanade.presentation.library.NovaLibraryEntry
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

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
