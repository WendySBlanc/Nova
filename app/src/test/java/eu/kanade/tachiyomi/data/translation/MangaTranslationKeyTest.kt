package eu.kanade.tachiyomi.data.translation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MangaTranslationKeyTest {

    @Test
    fun `chapter key does not depend on a page count`() {
        buildMangaTranslationChapterKey(sourceId = 11L, mangaId = 22L, chapterId = 33L) shouldBe
            "11:22:33"
    }
}
