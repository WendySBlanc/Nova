package eu.kanade.tachiyomi.data.lens

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ReaderLensTranslatorTest {

    private val blocks = listOf(
        ReaderLensTextBlock(0f, 0f, 100f, 50f, "Keep your voice down. They'll hear us."),
        ReaderLensTextBlock(0f, 60f, 100f, 110f, "Let them. I'm done hiding."),
    )

    @Test
    fun `prompt translates one ordered page and teaches idiomatic French`() {
        val prompt = buildIndexTranslationPrompt(
            blocks = blocks,
            sourceLanguage = "en",
            previousContext = "Master Jin opened the training room.",
        )

        prompt shouldContain "b0"
        prompt shouldContain "b1"
        prompt shouldContain "Keep your voice down. They'll hear us."
        prompt shouldContain "L'académie met la bibliothèque à la disposition des élèves"
        prompt shouldContain "Master Jin opened the training room."
        prompt shouldContain "français naturel"
    }

    @Test
    fun `parser returns translations in expected bubble order`() {
        val result = parseIndexTranslation(
            raw = """
                [
                  {"id":"b0","text":"Baisse la voix. Ils vont nous entendre."},
                  {"id":"b1","text":"Qu'ils entendent. J'en ai assez de me cacher."}
                ]
            """.trimIndent(),
            expectedIds = listOf("b0", "b1"),
        )

        result.keys.toList().shouldContainExactly("b0", "b1")
        result shouldContainExactly mapOf(
            "b0" to "Baisse la voix. Ils vont nous entendre.",
            "b1" to "Qu'ils entendent. J'en ai assez de me cacher.",
        )
    }

    @Test
    fun `parser rejects missing duplicate unknown reordered and blank bubbles`() {
        val invalidResponses = listOf(
            """[{"id":"b0","text":"Bonjour"}]""",
            """[{"id":"b0","text":"Bonjour"},{"id":"b0","text":"Encore"}]""",
            """[{"id":"b0","text":"Bonjour"},{"id":"b2","text":"Inconnu"}]""",
            """[{"id":"b1","text":"Deux"},{"id":"b0","text":"Un"}]""",
            """[{"id":"b0","text":"Bonjour"},{"id":"b1","text":"  "}]""",
        )

        invalidResponses.forEach { response ->
            shouldThrow<ReaderLensTranslationException> {
                parseIndexTranslation(response, expectedIds = listOf("b0", "b1"))
            }
        }
    }

    @Test
    fun `parser rejects prose and unknown fields around structured output`() {
        val invalidResponses = listOf(
            """Voici la traduction: [{"id":"b0","text":"Bonjour"}]""",
            """[{"id":"b0","text":"Bonjour","note":"extra"}]""",
            """```json
                [{"id":"b0","text":"Bonjour"}]
                ```""".trimIndent(),
        )

        invalidResponses.forEach { response ->
            shouldThrow<ReaderLensTranslationException> {
                parseIndexTranslation(response, expectedIds = listOf("b0"))
            }
        }
    }
}
