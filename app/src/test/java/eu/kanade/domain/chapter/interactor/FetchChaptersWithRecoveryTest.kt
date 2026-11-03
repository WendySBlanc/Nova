package eu.kanade.domain.chapter.interactor

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import java.io.IOException

class FetchChaptersWithRecoveryTest {

    @Test
    fun `returns the direct chapter list without starting recovery`() = runTest {
        val source = mockk<Source>()
        val chapters = listOf(chapter("Chapter 1", "/chapter-1"))
        coEvery { source.getChapterList(any()) } returns chapters

        val result = source.getChapterListWithRecovery(manga())

        result.chapters.shouldContainExactly(chapters)
        result.details shouldBe null
        result.resolvedUrl shouldBe null
        coVerify(exactly = 0) { source.getMangaDetails(any()) }
    }

    @Test
    fun `uses refreshed details when the stored URL no longer has chapters`() = runTest {
        val source = mockk<Source>()
        val refreshedDetails = sourceManga("The Hero's Ballad", "/series/new", author = "Author A")
        val chapters = listOf(chapter("Chapter 42", "/chapter-42"))
        coEvery { source.getChapterList(match { it.url == "/series/old" }) } returns emptyList()
        coEvery { source.getMangaDetails(match { it.url == "/series/old" }) } returns refreshedDetails
        coEvery { source.getChapterList(match { it.url == "/series/new" }) } returns chapters

        val result = source.getChapterListWithRecovery(manga(author = "Author A"))

        result.chapters.shouldContainExactly(chapters)
        result.details shouldBeSameInstanceAs refreshedDetails
        result.resolvedUrl shouldBe "/series/new"
    }

    @Test
    fun `rejects refreshed details that redirect to a different author`() = runTest {
        val source = mockk<Source>()
        val redirectedDetails = sourceManga(
            "The Hero's Ballad",
            "/series/unrelated",
            author = "Different Author",
        )
        val unrelatedChapters = listOf(chapter("Chapter 1", "/unrelated/chapter-1"))

        coEvery { source.getChapterList(match { it.url == "/series/old" }) } returns emptyList()
        coEvery { source.getMangaDetails(match { it.url == "/series/old" }) } returns redirectedDetails
        coEvery { source.getChapterList(match { it.url == "/series/unrelated" }) } returns unrelatedChapters

        val result = source.getChapterListWithRecovery(manga(author = "Original Author"))

        result.chapters shouldBe emptyList()
        result.details shouldBe null
        result.resolvedUrl shouldBe null
        coVerify(exactly = 0) { source.getChapterList(match { it.url == "/series/unrelated" }) }
    }

    @Test
    fun `recovers an exact catalogue match after a source URL change`() = runTest {
        val source = mockk<CatalogueSource>()
        val candidate = sourceManga("  THE HERO'S   BALLAD ", "/series/new")
        val candidateDetails = sourceManga("The Hero's Ballad", "/series/new", author = "Author A")
        val chapters = listOf(chapter("Chapter 42", "/chapter-42"))
        val initialFailure = IOException("old URL not found")

        coEvery { source.getChapterList(match { it.url == "/series/old" }) } throws initialFailure
        coEvery { source.getMangaDetails(match { it.url == "/series/old" }) } throws initialFailure
        every { source.getFilterList() } returns FilterList()
        coEvery { source.getSearchManga(1, "The Hero's Ballad", any()) } returns
            MangasPage(listOf(candidate), false)
        coEvery { source.getMangaDetails(match { it.url == "/series/new" }) } returns candidateDetails
        coEvery { source.getChapterList(match { it.url == "/series/new" }) } returns chapters

        val result = source.getChapterListWithRecovery(manga(author = "Author A"))

        result.chapters.shouldContainExactly(chapters)
        result.details shouldBeSameInstanceAs candidateDetails
        result.resolvedUrl shouldBe "/series/new"
    }

    @Test
    fun `does not recover a title-only homonym with a different author`() = runTest {
        val source = mockk<CatalogueSource>()
        val candidate = sourceManga("The Hero's Ballad", "/series/homonym")
        val candidateDetails = sourceManga(
            "The Hero's Ballad",
            "/series/homonym",
            author = "Different Author",
        )
        val chapters = listOf(chapter("Chapter 1", "/homonym/chapter-1"))

        coEvery { source.getChapterList(match { it.url == "/series/old" }) } returns emptyList()
        coEvery { source.getMangaDetails(match { it.url == "/series/old" }) } throws IOException("not found")
        every { source.getFilterList() } returns FilterList()
        coEvery { source.getSearchManga(1, "The Hero's Ballad", any()) } returns
            MangasPage(listOf(candidate), false)
        coEvery { source.getMangaDetails(match { it.url == "/series/homonym" }) } returns candidateDetails
        coEvery { source.getChapterList(match { it.url == "/series/homonym" }) } returns chapters

        val result = source.getChapterListWithRecovery(manga(author = "Original Author"))

        result.chapters shouldBe emptyList()
        result.details shouldBe null
        result.resolvedUrl shouldBe null
        coVerify(exactly = 0) { source.getChapterList(match { it.url == "/series/homonym" }) }
    }

    @Test
    fun `does not select an ambiguous catalogue result`() = runTest {
        val source = mockk<CatalogueSource>()
        val first = sourceManga("The Hero's Ballad", "/series/one")
        val second = sourceManga("The Hero's Ballad", "/series/two")

        coEvery { source.getChapterList(any()) } returns emptyList()
        coEvery { source.getMangaDetails(any()) } throws IOException("not found")
        every { source.getFilterList() } returns FilterList()
        coEvery { source.getSearchManga(1, "The Hero's Ballad", any()) } returns
            MangasPage(listOf(first, second), false)

        val result = source.getChapterListWithRecovery(manga())

        result.chapters shouldBe emptyList()
        result.details shouldBe null
        result.resolvedUrl shouldBe null
        coVerify(exactly = 1) { source.getChapterList(any()) }
    }

    @Test
    fun `rethrows the original error when recovery is unavailable`() = runTest {
        val source = mockk<Source>()
        val initialFailure = IOException("old URL not found")
        coEvery { source.getChapterList(any()) } throws initialFailure
        coEvery { source.getMangaDetails(any()) } throws IOException("details unavailable")

        val thrown = shouldThrow<IOException> {
            source.getChapterListWithRecovery(manga())
        }

        thrown shouldBeSameInstanceAs initialFailure
    }

    private fun manga(author: String? = null) = Manga.create().copy(
        source = 1L,
        url = "/series/old",
        ogTitle = "The Hero's Ballad",
        ogAuthor = author,
    )

    private fun sourceManga(title: String, url: String, author: String? = null) = SManga(
        title = title,
        url = url,
        author = author,
    )

    private fun chapter(name: String, url: String) = SChapter(
        name = name,
        url = url,
    )
}
