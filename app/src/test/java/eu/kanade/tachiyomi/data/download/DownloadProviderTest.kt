package eu.kanade.tachiyomi.data.download

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.storage.service.StorageManager

class DownloadProviderTest {

    private val libraryPreferences = mockk<LibraryPreferences> {
        every { disallowNonAsciiFilenames().get() } returns false
    }
    private val downloadPreferences = mockk<DownloadPreferences> {
        every { includeChapterUrlHash().get() } returns true
    }
    private val provider = DownloadProvider(
        context = mockk<Context>(relaxed = true),
        storageManager = mockk<StorageManager>(relaxed = true),
        libraryPreferences = libraryPreferences,
        downloadPreferences = downloadPreferences,
    )

    @Test
    fun `recognizes a downloaded chapter after its source URL changes`() {
        provider.isValidChapterDirName(
            downloadedDirName = "Chapter 12_012abc",
            chapterName = "Chapter 12",
            chapterScanlator = null,
            chapterUrl = "/new/chapter-url",
            allowRestoredMatch = true,
        ) shouldBe true
    }

    @Test
    fun `does not use restored name matching without manga chapter context`() {
        provider.isValidChapterDirName(
            downloadedDirName = "Chapter 12_012abc",
            chapterName = "Chapter 12",
            chapterScanlator = null,
            chapterUrl = "/new/chapter-url",
        ) shouldBe false
    }

    @Test
    fun `recognizes a restored CBZ after its source URL changes`() {
        provider.isValidChapterDirName(
            downloadedDirName = "Chapter 12_012abc.cbz",
            chapterName = "Chapter 12",
            chapterScanlator = null,
            chapterUrl = "/new/chapter-url",
            allowRestoredMatch = true,
        ) shouldBe true
    }

    @Test
    fun `recognizes a legacy chapter directory without a URL hash`() {
        provider.isValidChapterDirName(
            downloadedDirName = "Team_Chapter 12",
            chapterName = "Chapter 12",
            chapterScanlator = "Team",
            chapterUrl = "/chapter-url",
            allowRestoredMatch = true,
        ) shouldBe true
    }

    @Test
    fun `rejects a directory with a malformed URL hash suffix`() {
        provider.isValidChapterDirName(
            downloadedDirName = "Chapter 12_012xyz",
            chapterName = "Chapter 12",
            chapterScanlator = null,
            chapterUrl = "/new/chapter-url",
            allowRestoredMatch = true,
        ) shouldBe false
    }

    @Test
    fun `rejects a different chapter name`() {
        provider.isValidChapterDirName(
            downloadedDirName = "Chapter 13_012abc",
            chapterName = "Chapter 12",
            chapterScanlator = null,
            chapterUrl = "/chapter-url",
            allowRestoredMatch = true,
        ) shouldBe false
    }

    @Test
    fun `prefers an exact download over restored hash matches`() {
        val selected = selectUnambiguousChapterDownload(
            candidates = listOf("Chapter 12_old001", "Chapter 12_current"),
            isExact = { it.endsWith("_current") },
            isCompatible = { it.startsWith("Chapter 12_") },
        )

        selected shouldBe "Chapter 12_current"
    }

    @Test
    fun `accepts one restored hash match`() {
        val selected = selectUnambiguousChapterDownload(
            candidates = listOf("Chapter 12_old001"),
            isExact = { false },
            isCompatible = { it.startsWith("Chapter 12_") },
        )

        selected shouldBe "Chapter 12_old001"
    }

    @Test
    fun `rejects ambiguous restored hash matches`() {
        val selected = selectUnambiguousChapterDownload(
            candidates = listOf("Chapter 12_old001", "Chapter 12_old002"),
            isExact = { false },
            isCompatible = { it.startsWith("Chapter 12_") },
        )

        selected shouldBe null
    }

    @Test
    fun `prefers an exact match from a fallback directory over a preferred fuzzy match`() {
        val selected = selectUnambiguousChapterDownloadAcrossDirectories(
            candidateGroups = listOf(
                listOf("Chapter 12_old001"),
                listOf("Chapter 12_current"),
            ),
            isExact = { it.endsWith("_current") },
            isCompatible = { it.startsWith("Chapter 12_") },
        )

        selected shouldBe "Chapter 12_current"
    }

    @Test
    fun `rejects fuzzy matches spread across multiple manga directories`() {
        val selected = selectUnambiguousChapterDownloadAcrossDirectories(
            candidateGroups = listOf(
                listOf("Chapter 12_old001"),
                listOf("Chapter 12_old002"),
            ),
            isExact = { false },
            isCompatible = { it.startsWith("Chapter 12_") },
        )

        selected shouldBe null
    }

    @Test
    fun `keeps identical fuzzy names ambiguous across directories`() {
        val selected = selectUnambiguousChapterDownloadAcrossDirectories(
            candidateGroups = listOf(
                listOf("Chapter 12_old001"),
                listOf("Chapter 12_old001"),
            ),
            isExact = { false },
            isCompatible = { it.startsWith("Chapter 12_") },
        )

        selected shouldBe null
    }

    @Test
    fun `does not assign one restored download to two same-name chapters`() {
        val chapters = listOf(
            chapter(1L, "Chapter 12", "/new/one"),
            chapter(2L, "Chapter 12", "/new/two"),
        )

        val resolved = provider.resolveDownloadedChapterIds(
            chapters = chapters,
            downloadedDirNames = listOf("Chapter 12_012abc"),
        )

        resolved shouldBe emptySet()
    }

    @Test
    fun `assigns one restored download when the chapter identity is unique`() {
        val chapters = listOf(
            chapter(1L, "Chapter 12", "/new/one"),
            chapter(2L, "Chapter 13", "/new/two"),
        )

        val resolved = provider.resolveDownloadedChapterIds(
            chapters = chapters,
            downloadedDirNames = listOf("Chapter 12_012abc"),
        )

        resolved shouldBe setOf(1L)
    }

    @Test
    fun `strong URL hash match wins when chapter names collide`() {
        val chapters = listOf(
            chapter(1L, "Chapter 12", "/current/one"),
            chapter(2L, "Chapter 12", "/current/two"),
        )
        val exactName = provider.getChapterDirName(
            chapterName = chapters.first().name,
            chapterScanlator = chapters.first().scanlator,
            chapterUrl = chapters.first().url,
            includeChapterUrlHash = true,
        )

        val resolved = provider.resolveDownloadedChapterIds(
            chapters = chapters,
            downloadedDirNames = listOf(exactName),
        )

        resolved shouldBe setOf(1L)
    }

    @Test
    fun `returns a globally resolved restored download for deletion`() {
        val selected = resolveUnambiguousChapterDownloadMap(
            chapters = listOf("Chapter 12", "Chapter 13"),
            downloads = listOf("Chapter 12_old001"),
            chapterKey = { it },
            isExact = { _, _ -> false },
            isCompatible = { chapter, download -> download.startsWith(chapter) },
        )

        selected shouldBe mapOf("Chapter 12" to "Chapter 12_old001")
    }

    private fun chapter(id: Long, name: String, url: String) = Chapter.create().copy(
        id = id,
        mangaId = 1L,
        name = name,
        url = url,
    )
}
