package eu.kanade.tachiyomi.data.lens

import kotlinx.coroutines.CancellationException
import java.io.File

internal interface ReaderLensTranslator : AutoCloseable {
    suspend fun translatePage(
        blocks: List<ReaderLensTextBlock>,
        sourceLanguage: String,
        previousContext: String? = null,
    ): ReaderLensTranslationResult

    fun releaseWarmModel() = Unit

    override fun close() = Unit
}

internal data class ReaderLensTranslationResult(
    val translations: Map<String, String>,
    val cacheable: Boolean,
) : Map<String, String> by translations

internal class ReaderLensTranslationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal interface ReaderLensFrenchCorrector : AutoCloseable {
    suspend fun correctPage(translations: Map<String, String>): Map<String, String>

    fun releaseWarmModel() = Unit

    override fun close() = Unit
}

private object NoOpReaderLensFrenchCorrector : ReaderLensFrenchCorrector {
    override suspend fun correctPage(translations: Map<String, String>): Map<String, String> = translations
}

internal class HyMtReaderLensTranslator(
    private val modelProvider: suspend () -> File,
    private val runtime: ReaderLensInferenceRuntime,
    private val corrector: ReaderLensFrenchCorrector = NoOpReaderLensFrenchCorrector,
) : ReaderLensTranslator {

    constructor(
        modelStore: ReaderLensModelStore,
        runtime: ReaderLensInferenceRuntime,
        corrector: ReaderLensFrenchCorrector = NoOpReaderLensFrenchCorrector,
    ) : this(modelStore::ensureModel, runtime, corrector)

    override suspend fun translatePage(
        blocks: List<ReaderLensTextBlock>,
        sourceLanguage: String,
        previousContext: String?,
    ): ReaderLensTranslationResult {
        if (blocks.isEmpty()) return ReaderLensTranslationResult(emptyMap(), cacheable = true)
        val model = modelProvider()
        val requests = buildReaderLensTranslationRequests(blocks)
        val translatedSegments = blocks.indices.associate { index -> "b$index" to mutableListOf<String>() }

        requests.forEach { request ->
            val prompt = buildHyMtTranslationPrompt(request.text)
            check(prompt.encodeToByteArray().size <= READER_LENS_MAX_PROMPT_BYTES)
            val maxOutputTokens = readerLensOutputTokenBudget(listOf(request.text))
            val response = runtime.complete(model, prompt, maxOutputTokens)
            translatedSegments.getValue(request.outputId) += parseHyMtTranslation(response)
        }
        val translations = translatedSegments.mapValuesTo(linkedMapOf()) { (_, segments) -> segments.joinToString(" ") }
        return try {
            val corrected = corrector.correctPage(translations)
            if (corrected.keys == translations.keys) {
                ReaderLensTranslationResult(corrected, cacheable = true)
            } else {
                ReaderLensTranslationResult(translations, cacheable = false)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            ReaderLensTranslationResult(translations, cacheable = false)
        }
    }

    override fun close() {
        corrector.close()
        runtime.close()
    }

    override fun releaseWarmModel() {
        corrector.releaseWarmModel()
        runtime.releaseWarmModel()
    }
}

internal fun buildHyMtTranslationPrompt(segment: String): String = buildString {
    appendLine("Translate the following segment into French, without additional explanation.")
    appendLine()
    append(segment.trim())
}

private data class ReaderLensTranslationRequest(
    val text: String,
    val outputId: String,
)

private fun buildReaderLensTranslationRequests(
    blocks: List<ReaderLensTextBlock>,
): List<ReaderLensTranslationRequest> = buildList {
    blocks.forEachIndexed { blockIndex, block ->
        val outputId = "b$blockIndex"
        val segments = block.text.splitForReaderLensTranslation()
        segments.forEach { segment ->
            add(
                ReaderLensTranslationRequest(
                    text = segment,
                    outputId = outputId,
                ),
            )
        }
    }
}

private fun String.splitForReaderLensTranslation(): List<String> {
    if (codePointCount(0, length) <= READER_LENS_SEGMENT_CODE_POINTS && encodeToByteArray().size <= READER_LENS_SEGMENT_BYTES) {
        return listOf(this)
    }
    val segments = mutableListOf<String>()
    var current = StringBuilder()
    var currentBytes = 0
    var currentCodePoints = 0
    var offset = 0
    while (offset < length) {
        val codePoint = codePointAt(offset)
        val encoded = String(Character.toChars(codePoint))
        val encodedBytes = encoded.encodeToByteArray().size
        if (current.isNotEmpty() &&
            (currentCodePoints >= READER_LENS_SEGMENT_CODE_POINTS || currentBytes + encodedBytes > READER_LENS_SEGMENT_BYTES)
        ) {
            segments += current.toString().trim()
            current = StringBuilder()
            currentBytes = 0
            currentCodePoints = 0
        }
        current.append(encoded)
        currentBytes += encodedBytes
        currentCodePoints++
        offset += Character.charCount(codePoint)
    }
    if (current.isNotEmpty()) segments += current.toString().trim()
    return segments.filter(String::isNotEmpty)
}

internal fun readerLensOutputTokenBudget(texts: List<String>): Int {
    val sourceCodePoints = texts.sumOf { it.codePointCount(0, it.length) }
    val estimatedOutputCharacters = sourceCodePoints * 2
    val estimatedTokens = (estimatedOutputCharacters + 1) / 2
    return estimatedTokens.coerceIn(READER_LENS_MIN_OUTPUT_TOKENS, READER_LENS_MAX_OUTPUT_TOKENS)
}

internal fun parseHyMtTranslation(raw: String): String =
    raw.trim().takeIf(String::isNotEmpty) ?: run {
        throw ReaderLensTranslationException("Le modèle Lens a renvoyé une bulle vide")
    }

internal const val READER_LENS_PROMPT_SCHEMA_VERSION = 9

internal const val READER_LENS_MAX_PROMPT_BYTES = 2_500
private const val READER_LENS_SEGMENT_BYTES = 1_000
private const val READER_LENS_SEGMENT_CODE_POINTS = 1_000
private const val READER_LENS_MIN_OUTPUT_TOKENS = 64
private const val READER_LENS_MAX_OUTPUT_TOKENS = 1_024
