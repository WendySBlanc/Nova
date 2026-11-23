package eu.kanade.tachiyomi.data.lens

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderLensTranslatorTest {

    @Test
    fun `plain HY MT response is accepted and trimmed`() {
        parseHyMtTranslation("  La salle d'entraînement est prête.\n") shouldBe
            "La salle d'entraînement est prête."
    }

    @Test
    fun `prompt contains only the official instruction and source segment`() {
        buildHyMtTranslationPrompt("Keep your voice down. They'll hear us.") shouldBe """
            Translate the following segment into French, without additional explanation.

            Keep your voice down. They'll hear us.
        """.trimIndent()
    }

    @Test
    fun `parser rejects an empty native response`() {
        shouldThrow<ReaderLensTranslationException> {
            parseHyMtTranslation("\n\t ")
        }
    }
}
