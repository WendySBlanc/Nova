package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

class DownloadStoreTest {

    private lateinit var preferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sourceManager: SourceManager
    private lateinit var getManga: GetManga
    private lateinit var getChapter: GetChapter
    private lateinit var store: DownloadStore

    @BeforeEach
    fun setUp() {
        val context = mockk<Context>()
        preferences = mockk()
        editor = mockk(relaxed = true)
        sourceManager = mockk(relaxed = true)
        getManga = mockk()
        getChapter = mockk()

        every { context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE) } returns preferences
        every { preferences.edit() } returns editor
        every { sourceManager.isInitialized } returns MutableStateFlow(true)

        store = DownloadStore(
            context = context,
            sourceManager = sourceManager,
            json = Json,
            getManga = getManga,
            getChapter = getChapter,
        )
    }

    @Test
    fun `restore keeps an entry when its extension is unavailable`() = runTest {
        val manga = Manga.create().copy(id = 10L, source = 20L, ogTitle = "Series")
        every { preferences.all } returns mapOf("1" to storedDownload(manga.id, 1L))
        coEvery { getManga.await(manga.id) } returns manga
        every { sourceManager.get(manga.source) } returns null

        store.restore().shouldBe(emptyList())

        verify(exactly = 0) { editor.remove("1") }
        verify(exactly = 0) { editor.clear() }
    }

    @Test
    fun `restore removes only the entry successfully returned to the queue`() = runTest {
        val manga = Manga.create().copy(id = 10L, source = 20L, ogTitle = "Series")
        val chapter = Chapter.create().copy(id = 1L, mangaId = manga.id, name = "Chapter 1")
        val source = mockk<HttpSource>()
        every { preferences.all } returns mapOf(chapter.id.toString() to storedDownload(manga.id, chapter.id))
        coEvery { getManga.await(manga.id) } returns manga
        coEvery { getChapter.await(chapter.id) } returns chapter
        every { sourceManager.get(manga.source) } returns source

        val restored = store.restore()

        restored.shouldHaveSize(1)
        restored.single().chapter shouldBe chapter
        verify { editor.remove(chapter.id.toString()) }
        verify(exactly = 0) { editor.clear() }
    }

    private fun storedDownload(mangaId: Long, chapterId: Long): String {
        return """{"mangaId":$mangaId,"chapterId":$chapterId,"order":0}"""
    }
}
