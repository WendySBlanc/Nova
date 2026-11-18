package eu.kanade.tachiyomi.data.lens

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class IndexReaderLensTranslatorTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `translator sends one contextual request for the whole page`() = runTest {
        val runtime = FakeReaderLensRuntime(
            response = """[{"id":"b0","text":"Bonjour"},{"id":"b1","text":"Comment allez-vous ?"}]""",
        )
        val translator = IndexReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = runtime,
        )
        val blocks = listOf(
            ReaderLensTextBlock(0f, 0f, 100f, 100f, "Hello"),
            ReaderLensTextBlock(200f, 300f, 400f, 380f, "How are you?"),
        )

        translator.translatePage(blocks, sourceLanguage = "en") shouldContainExactly linkedMapOf(
            "b0" to "Bonjour",
            "b1" to "Comment allez-vous ?",
        )

        runtime.calls shouldBe 1
        runtime.lastPrompt?.contains("Hello") shouldBe true
        runtime.lastPrompt?.contains("How are you?") shouldBe true
    }

    @Test
    fun `page translation preserves coordinates and calls translator once`() = runTest {
        val translator = FakeReaderLensTranslator(
            translations = linkedMapOf("b0" to "Bonjour", "b1" to "Au revoir"),
        )
        val blocks = listOf(
            ReaderLensTextBlock(0f, 0f, 100f, 100f, "Hello"),
            ReaderLensTextBlock(200f, 300f, 400f, 380f, "Goodbye"),
        )

        val regions = translateReaderLensPage(
            blocks = blocks,
            sourceLanguage = "en",
            imageWidth = 1_000,
            imageHeight = 1_000,
            translator = translator,
        )

        translator.calls shouldBe 1
        regions shouldHaveSize 2
        regions[0].left shouldBe (0f plusOrMinus 0.0001f)
        regions[0].top shouldBe (0f plusOrMinus 0.0001f)
        regions[0].width shouldBe (0.112f plusOrMinus 0.0001f)
        regions[0].height shouldBe (0.118f plusOrMinus 0.0001f)
        regions[0].text shouldBe "Bonjour"
    }

    @Test
    fun `French pages bypass local translation`() = runTest {
        val translator = FakeReaderLensTranslator(emptyMap())

        translateReaderLensPage(
            blocks = listOf(ReaderLensTextBlock(0f, 0f, 100f, 100f, "Bonjour")),
            sourceLanguage = "fr",
            imageWidth = 1_000,
            imageHeight = 1_000,
            translator = translator,
        ) shouldBe emptyList()

        translator.calls shouldBe 0
    }

    @Test
    fun `malformed model output is rejected`() = runTest {
        val translator = IndexReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = FakeReaderLensRuntime("Voici la traduction : Bonjour"),
        )

        shouldThrow<ReaderLensTranslationException> {
            translator.translatePage(
                blocks = listOf(ReaderLensTextBlock(0f, 0f, 100f, 100f, "Hello")),
                sourceLanguage = "en",
            )
        }
    }

    private fun modelFile(): File = temporaryDirectory.resolve("model.gguf").toFile().apply {
        if (!exists()) writeText("model")
    }
}

private class FakeReaderLensRuntime(
    private val response: String,
) : ReaderLensInferenceRuntime {
    var calls = 0
    var lastPrompt: String? = null

    override suspend fun complete(modelFile: File, prompt: String): String {
        calls++
        lastPrompt = prompt
        return response
    }

    override fun close() = Unit
}

private class FakeReaderLensTranslator(
    private val translations: Map<String, String>,
) : ReaderLensTranslator {
    var calls = 0

    override suspend fun translatePage(
        blocks: List<ReaderLensTextBlock>,
        sourceLanguage: String,
        previousContext: String?,
    ): Map<String, String> {
        calls++
        return translations
    }
}
