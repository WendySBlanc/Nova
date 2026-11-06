package eu.kanade.presentation.library

import eu.kanade.presentation.library.components.NovaLibraryCategory
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaLibraryShelvesTest {

    @Test
    fun `archive category matching is case insensitive and deterministic`() {
        findNovaArchiveCategory(
            listOf(
                NovaLibraryCategory(9, "ARCHIVES"),
                NovaLibraryCategory(4, "archives"),
                NovaLibraryCategory(2, "Favorites"),
            ),
        ) shouldBe NovaLibraryCategory(4, "archives")
    }

    @Test
    fun `missing archive category leaves every title current`() {
        val items = listOf(entry(1), entry(2))

        filterNovaLibraryShelf(items, NovaLibraryShelf.Current, null) shouldContainExactly items
        filterNovaLibraryShelf(items, NovaLibraryShelf.Archive, null) shouldContainExactly emptyList()
    }

    @Test
    fun `current and archive shelves are explicit opposites`() {
        val items = listOf(entry(1), entry(2, categories = setOf(7)), entry(3, categories = setOf(8)))

        filterNovaLibraryShelf(items, NovaLibraryShelf.Current, 7).map { it.id } shouldContainExactly listOf(1L, 3L)
        filterNovaLibraryShelf(items, NovaLibraryShelf.Archive, 7).map { it.id } shouldContainExactly listOf(2L)
    }

    @Test
    fun `archive category is excluded from collections`() {
        novaCollectionCategories(
            listOf(
                NovaLibraryCategory(7, "Archives"),
                NovaLibraryCategory(8, "Action"),
            ),
        ) shouldContainExactly listOf(NovaLibraryCategory(8, "Action"))
    }

    @Test
    fun `library tabs expose current archive and real categories`() {
        novaLibraryShelfOptions(
            currentCount = 12,
            archiveCount = 3,
            categories = listOf(
                NovaLibraryCategory(8, "Action"),
                NovaLibraryCategory(11, "A lire"),
            ),
            categoryCounts = mapOf(8L to 7, 11L to 2),
        ) shouldContainExactly listOf(
            NovaLibraryShelfOption(NovaLibraryShelf.Current, null, "En cours", 12),
            NovaLibraryShelfOption(NovaLibraryShelf.Archive, null, "Archives", 3),
            NovaLibraryShelfOption(NovaLibraryShelf.Collections, 8, "Action", 7),
            NovaLibraryShelfOption(NovaLibraryShelf.Collections, 11, "A lire", 2),
        )
    }

    private fun entry(id: Long, categories: Set<Long> = emptySet()) = NovaLibraryEntry(
        id = id,
        title = "Title $id",
        subtitle = "Subtitle",
        tag = "NOVA",
        categoryIds = categories,
    )
}
