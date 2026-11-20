package eu.kanade.tachiyomi.data.lens

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
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
    fun `translator corrects the reassembled page exactly once`() = runTest {
        val corrector = FakeReaderLensFrenchCorrector(
            corrected = linkedMapOf(
                "b0" to "Ma soeur est très contente.",
                "b1" to "Ils sont déjà partis.",
            ),
        )
        val translator = IndexReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = FakeReaderLensRuntime(
                response = """[{"id":"b0","text":"Ma soeur sont très content."},{"id":"b1","text":"Ils est déjà parti."}]""",
            ),
            corrector = corrector,
        )
        val blocks = listOf(
            ReaderLensTextBlock(0f, 0f, 100f, 100f, "My sister is very happy."),
            ReaderLensTextBlock(0f, 100f, 100f, 200f, "They already left."),
        )

        translator.translatePage(blocks, sourceLanguage = "en") shouldContainExactly corrector.corrected

        corrector.calls shouldBe 1
        corrector.lastPage shouldContainExactly linkedMapOf(
            "b0" to "Ma soeur sont très content.",
            "b1" to "Ils est déjà parti.",
        )
    }

    @Test
    fun `translator keeps Index output when French correction fails`() = runTest {
        val translator = IndexReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = FakeReaderLensRuntime(
                response = """[{"id":"b0","text":"La traduction reste visible."}]""",
            ),
            corrector = FakeReaderLensFrenchCorrector(error = IllegalStateException("ONNX unavailable")),
        )

        val result = translator.translatePage(
            blocks = listOf(ReaderLensTextBlock(0f, 0f, 100f, 100f, "Translation remains visible.")),
            sourceLanguage = "en",
        )

        result.translations shouldContainExactly linkedMapOf("b0" to "La traduction reste visible.")
        result.cacheable shouldBe false
    }

    @Test
    fun `translator splits oversized pages while preserving global bubble ids`() = runTest {
        val runtime = FakeReaderLensRuntime(
            responseFactory = { prompt ->
                promptIds(prompt).joinToString(prefix = "[", postfix = "]") { id ->
                    val translation = mapOf("b0" to "Premier", "b1" to "Deuxième", "b2" to "Troisième").getValue(id)
                    """{"id":"$id","text":"$translation"}"""
                }
            },
        )
        val translator = IndexReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = runtime,
        )
        val blocks = listOf(
            ReaderLensTextBlock(0f, 0f, 100f, 100f, "A".repeat(800)),
            ReaderLensTextBlock(0f, 100f, 100f, 200f, "B".repeat(800)),
            ReaderLensTextBlock(0f, 200f, 100f, 300f, "C".repeat(800)),
        )

        translator.translatePage(blocks, sourceLanguage = "en") shouldContainExactly linkedMapOf(
            "b0" to "Premier",
            "b1" to "Deuxième",
            "b2" to "Troisième",
        )

        runtime.calls.shouldBeGreaterThan(1)
        val combinedPrompts = runtime.prompts.joinToString()
        combinedPrompts.contains("\"id\":\"b0\"") shouldBe true
        combinedPrompts.contains("\"id\":\"b1\"") shouldBe true
        combinedPrompts.contains("\"id\":\"b2\"") shouldBe true
    }

    @Test
    fun `translator splits one oversized OCR bubble within the prompt budget`() = runTest {
        val runtime = FakeReaderLensRuntime(
            responseFactory = { prompt ->
                promptIds(prompt).joinToString(prefix = "[", postfix = "]") { id ->
                    """{"id":"$id","text":"traduit"}"""
                }
            },
        )
        val translator = IndexReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = runtime,
        )
        val blocks = listOf(
            ReaderLensTextBlock(0f, 0f, 100f, 100f, "漢".repeat(5_000)),
        )

        val result = translator.translatePage(blocks, sourceLanguage = "zh")

        runtime.calls.shouldBeGreaterThan(1)
        runtime.prompts.forEach { prompt ->
            prompt.encodeToByteArray().size.shouldBeLessThanOrEqual(READER_LENS_MAX_PROMPT_BYTES)
        }
        result.keys shouldBe setOf("b0")
        result.getValue("b0").split(' ').all { it == "traduit" } shouldBe true
        result.getValue("b0").split(' ').size.shouldBeGreaterThan(1)
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

        val result = translateReaderLensPage(
            blocks = blocks,
            sourceLanguage = "en",
            imageWidth = 1_000,
            imageHeight = 1_000,
            translator = translator,
        )

        translator.calls shouldBe 1
        result.regions shouldHaveSize 2
        result.regions[0].left shouldBe (0f plusOrMinus 0.0001f)
        result.regions[0].top shouldBe (0f plusOrMinus 0.0001f)
        result.regions[0].width shouldBe (0.112f plusOrMinus 0.0001f)
        result.regions[0].height shouldBe (0.118f plusOrMinus 0.0001f)
        result.regions[0].text shouldBe "Bonjour"
        result.cacheable shouldBe true
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
        ).regions shouldBe emptyList()

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

private fun promptIds(prompt: String): List<String> = Regex(""""id":"([^"]+)"""")
    .findAll(prompt)
    .map { it.groupValues[1] }
    .toList()

private class FakeReaderLensRuntime(
    response: String? = null,
    responses: List<String> = response?.let(::listOf).orEmpty(),
    private val responseFactory: ((String) -> String)? = null,
) : ReaderLensInferenceRuntime {
    private val responses = ArrayDeque(responses)
    var calls = 0
    var lastPrompt: String? = null
    val prompts = mutableListOf<String>()

    override suspend fun complete(modelFile: File, prompt: String): String {
        calls++
        lastPrompt = prompt
        prompts += prompt
        return responseFactory?.invoke(prompt) ?: responses.removeFirst()
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
    ): ReaderLensTranslationResult {
        calls++
        return ReaderLensTranslationResult(translations, cacheable = true)
    }
}

private class FakeReaderLensFrenchCorrector(
    val corrected: Map<String, String> = emptyMap(),
    private val error: Throwable? = null,
) : ReaderLensFrenchCorrector {
    var calls = 0
    var lastPage: Map<String, String> = emptyMap()

    override suspend fun correctPage(translations: Map<String, String>): Map<String, String> {
        calls++
        lastPage = translations
        error?.let { throw it }
        return corrected
    }
}
