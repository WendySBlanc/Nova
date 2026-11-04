package eu.kanade.presentation.library

import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaLibraryModelsTest {

    @Test
    fun `completed reading fraction progressively desaturates a cover`() {
        coverSaturation(null).shouldBeExactly(1f)
        coverSaturation(0f).shouldBeExactly(1f)
        coverSaturation(0.4f).shouldBeExactly(0.6f)
        coverSaturation(1f).shouldBeExactly(0f)
    }

    @Test
    fun `an unread chapter restores the cover to full color`() {
        coverSaturation(progress = 0.995f, remainingChapters = 1L).shouldBeExactly(1f)
        coverSaturation(progress = 1f, remainingChapters = 3L).shouldBeExactly(1f)
    }

    @Test
    fun `library filtering keeps media type and user category independent`() {
        val entries = listOf(
            entry(id = 1L, mediaType = NovaMediaType.Manhwa, categories = setOf(10L)),
            entry(id = 2L, mediaType = NovaMediaType.LightNovel, categories = setOf(10L)),
            entry(id = 3L, mediaType = NovaMediaType.Manhwa, categories = setOf(20L)),
        )

        filterNovaLibraryEntries(entries, NovaMediaType.Manhwa, categoryId = 10L).map { it.id } shouldBe listOf(1L)
        filterNovaLibraryEntries(entries, NovaMediaType.LightNovel, categoryId = null).map { it.id } shouldBe listOf(2L)
    }

    @Test
    fun `empty state distinguishes loading empty library and empty category`() {
        resolveNovaLibraryEmptyState(isReady = false, itemCount = 0, hasCategoryFilter = false) shouldBe
            NovaLibraryEmptyState.Loading
        resolveNovaLibraryEmptyState(isReady = true, itemCount = 0, hasCategoryFilter = false) shouldBe
            NovaLibraryEmptyState.EmptyLibrary
        resolveNovaLibraryEmptyState(isReady = true, itemCount = 0, hasCategoryFilter = true) shouldBe
            NovaLibraryEmptyState.EmptyFilter
        resolveNovaLibraryEmptyState(isReady = true, itemCount = 2, hasCategoryFilter = true) shouldBe null
    }

    private fun entry(
        id: Long,
        mediaType: NovaMediaType,
        categories: Set<Long>,
    ) = NovaLibraryEntry(
        id = id,
        title = "Title $id",
        subtitle = "Subtitle",
        tag = "Source",
        mediaType = mediaType,
        categoryIds = categories,
    )
}
