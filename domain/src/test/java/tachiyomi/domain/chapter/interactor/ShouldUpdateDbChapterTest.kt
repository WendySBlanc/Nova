package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class ShouldUpdateDbChapterTest {

    private val subject = ShouldUpdateDbChapter()

    @Test
    fun `memo changes trigger a database update`() {
        val stored = Chapter.create()
        val fetched = stored.copy(
            memo = buildJsonObject {
                put("mangaSlug", "the-heros-ballad")
                put("number", "9")
            },
        )

        subject.await(stored, fetched) shouldBe true
        subject.await(fetched, fetched.copy()) shouldBe false
    }
}
