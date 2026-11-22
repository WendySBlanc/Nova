package eu.kanade.tachiyomi.ui.library

import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.source.local.LocalSource

class LibrarySearchMetadataIndexTest {

    @Test
    fun `metadata membership does not depend on database ordering`() {
        val tag = SearchTag(id = 1L, mangaId = 7L, namespace = "genre", name = "action", type = 0)
        val title = SearchTitle(id = 1L, mangaId = 7L, title = "Hero", type = 0)
        val index = buildLibrarySearchMetadataIndex(
            metadataIds = listOf(42L, 7L, 19L),
            tags = listOf(tag),
            titles = listOf(title),
        )

        index.contains(7L) shouldBe true
        index.contains(19L) shouldBe true
        index.tagsFor(7L).shouldContainExactly(tag)
        index.titlesFor(7L).shouldContainExactly(title)
    }

    @Test
    fun `local source never matches a numeric source query`() {
        librarySourceMatchesQuery(LocalSource.ID, LocalSource.ID.toString()) shouldBe false
        librarySourceMatchesQuery(123L, "123") shouldBe true
        librarySourceMatchesQuery(123L, "456") shouldBe false
    }
}
