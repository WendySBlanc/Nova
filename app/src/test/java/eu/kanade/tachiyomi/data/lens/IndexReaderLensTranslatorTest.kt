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

class HyMtReaderLensTranslatorTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `output token budget scales with page text and stays bounded`() {
        readerLensOutputTokenBudget(listOf("Urgh!")) shouldBe 64
        readerLensOutputTokenBudget(listOf("A".repeat(300))).shouldBeGreaterThan(64)
        readerLensOutputTokenBudget(listOf("A".repeat(5_000))) shouldBe 1_024
    }

    @Test
    fun `prompt follows the native HY MT translation format`() {
        val prompt = buildHyMtTranslationPrompt("The training room is open.")

        prompt shouldBe """
            Translate the following segment into French, without additional explanation.

            The training room is open.
        """.trimIndent()
    }

    @Test
    fun `translator maps one native response to each bubble`() = runTest {
        val runtime = FakeReaderLensRuntime(
            responses = listOf("Bonjour", "Comment allez-vous ?"),
        )
        val translator = HyMtReaderLensTranslator(
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

        runtime.calls shouldBe 2
        runtime.prompts[0].endsWith("Hello") shouldBe true
        runtime.prompts[1].endsWith("How are you?") shouldBe true
        runtime.maxOutputTokens shouldBe listOf(64, 64)
    }

    @Test
    fun `translator corrects the reassembled page exactly once`() = runTest {
        val corrector = FakeReaderLensFrenchCorrector(
            corrected = linkedMapOf(
                "b0" to "Ma soeur est très contente.",
                "b1" to "Ils sont déjà partis.",
            ),
        )
        val translator = HyMtReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = FakeReaderLensRuntime(
                responses = listOf("Ma soeur sont très content.", "Ils est déjà parti."),
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
    fun `translator keeps HY MT output when French correction fails`() = runTest {
        val translator = HyMtReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = FakeReaderLensRuntime(
                response = "La traduction reste visible.",
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
                when {
                    prompt.endsWith("A".repeat(800)) -> "Premier"
                    prompt.endsWith("B".repeat(800)) -> "Deuxième"
                    else -> "Troisième"
                }
            },
        )
        val translator = HyMtReaderLensTranslator(
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
        runtime.prompts.all { prompt ->
            prompt.startsWith("Translate the following segment into French, without additional explanation.")
        } shouldBe true
    }

    @Test
    fun `translator splits one oversized OCR bubble within the prompt budget`() = runTest {
        val runtime = FakeReaderLensRuntime(
            responseFactory = { "traduit" },
        )
        val translator = HyMtReaderLensTranslator(
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
    fun `empty model output is rejected`() = runTest {
        val translator = HyMtReaderLensTranslator(
            modelProvider = { modelFile() },
            runtime = FakeReaderLensRuntime("   "),
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
    response: String? = null,
    responses: List<String> = response?.let(::listOf).orEmpty(),
    private val responseFactory: ((String) -> String)? = null,
) : ReaderLensInferenceRuntime {
    private val responses = ArrayDeque(responses)
    var calls = 0
    var lastPrompt: String? = null
    val prompts = mutableListOf<String>()
    val maxOutputTokens = mutableListOf<Int>()

    override suspend fun complete(modelFile: File, prompt: String, maxOutputTokens: Int): String {
        calls++
        lastPrompt = prompt
        prompts += prompt
        this.maxOutputTokens += maxOutputTokens
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
