package eu.kanade.tachiyomi.data.lens

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

internal interface ReaderLensTranslator : AutoCloseable {
    suspend fun translatePage(
        blocks: List<ReaderLensTextBlock>,
        sourceLanguage: String,
        previousContext: String? = null,
    ): Map<String, String>

    fun releaseWarmModel() = Unit

    override fun close() = Unit
}

internal class ReaderLensTranslationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal class IndexReaderLensTranslator(
    private val modelProvider: suspend () -> File,
    private val runtime: ReaderLensInferenceRuntime,
) : ReaderLensTranslator {

    constructor(
        modelStore: ReaderLensModelStore,
        runtime: ReaderLensInferenceRuntime,
    ) : this(modelStore::ensureModel, runtime)

    override suspend fun translatePage(
        blocks: List<ReaderLensTextBlock>,
        sourceLanguage: String,
        previousContext: String?,
    ): Map<String, String> {
        if (blocks.isEmpty()) return emptyMap()
        val model = modelProvider()
        val requests = buildReaderLensTranslationRequests(blocks)
        val translatedSegments = blocks.indices.associate { index -> "b$index" to mutableListOf<String>() }
        var requestOffset = 0
        var rollingContext = previousContext.orEmpty()

        while (requestOffset < requests.size) {
            val boundedContext = rollingContext.takeLastUtf8(READER_LENS_CONTEXT_BYTES)
            val batch = buildReaderLensTranslationBatch(
                requests = requests,
                startIndex = requestOffset,
                sourceLanguage = sourceLanguage,
                previousContext = boundedContext,
            )
            val prompt = buildIndexTranslationPromptForItems(
                items = batch.map(ReaderLensTranslationRequest::item),
                sourceLanguage = sourceLanguage,
                previousContext = boundedContext,
            )
            check(prompt.encodeToByteArray().size <= READER_LENS_MAX_PROMPT_BYTES)
            val expectedIds = batch.map { it.item.id }
            val response = runtime.complete(model, prompt)
            val parsed = parseIndexTranslation(response, expectedIds)
            batch.forEach { request ->
                translatedSegments.getValue(request.outputId) += parsed.getValue(request.item.id)
            }
            rollingContext = buildString {
                append(rollingContext)
                if (isNotEmpty()) appendLine()
                append(batch.joinToString("\n") { it.item.text })
            }.takeLastUtf8(READER_LENS_CONTEXT_BYTES)
            requestOffset += batch.size
        }
        return translatedSegments.mapValuesTo(linkedMapOf()) { (_, segments) -> segments.joinToString(" ") }
    }

    override fun close() {
        runtime.close()
    }

    override fun releaseWarmModel() {
        runtime.releaseWarmModel()
    }
}

@Serializable
private data class IndexTranslationItem(
    val id: String,
    val text: String,
)

internal fun buildIndexTranslationPrompt(
    blocks: List<ReaderLensTextBlock>,
    sourceLanguage: String,
    previousContext: String? = null,
    idOffset: Int = 0,
): String {
    val payload = blocks.mapIndexed { index, block ->
        IndexTranslationItem(id = "b${idOffset + index}", text = block.text)
    }
    return buildIndexTranslationPromptForItems(payload, sourceLanguage, previousContext)
}

private fun buildIndexTranslationPromptForItems(
    items: List<IndexTranslationItem>,
    sourceLanguage: String,
    previousContext: String?,
): String {
    val context = previousContext
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { "Contexte précédent en lecture seule : $it\n" }
        .orEmpty()

    return buildString {
        appendLine("Traduis cette page de manhwa de la langue '$sourceLanguage' vers un français naturel et concis.")
        appendLine("Traduis le sens plutôt que la structure anglaise, conserve les noms propres et adapte le registre aux personnages.")
        appendLine("Évite les calques et les formulations administratives. Préfère une formulation française directe.")
        appendLine("Si tu utilises un raisonnement interne, garde-le très bref avant de produire la réponse.")
        appendLine(
            "Exemple de style : \"The library is a facility provided to students by the academy\" devient " +
                "\"L'académie met la bibliothèque à la disposition des élèves\".",
        )
        appendLine("Réponds uniquement avec un tableau JSON. Garde exactement chaque id, dans le même ordre, sans champ supplémentaire.")
        append(context)
        append(Json.encodeToString(items))
    }
}

private data class ReaderLensTranslationRequest(
    val item: IndexTranslationItem,
    val outputId: String,
)

private fun buildReaderLensTranslationRequests(
    blocks: List<ReaderLensTextBlock>,
): List<ReaderLensTranslationRequest> = buildList {
    blocks.forEachIndexed { blockIndex, block ->
        val outputId = "b$blockIndex"
        val segments = block.text.splitForReaderLensTranslation()
        segments.forEachIndexed { segmentIndex, segment ->
            val requestId = if (segments.size == 1) outputId else "${outputId}s$segmentIndex"
            add(
                ReaderLensTranslationRequest(
                    item = IndexTranslationItem(requestId, segment),
                    outputId = outputId,
                ),
            )
        }
    }
}

private fun buildReaderLensTranslationBatch(
    requests: List<ReaderLensTranslationRequest>,
    startIndex: Int,
    sourceLanguage: String,
    previousContext: String,
): List<ReaderLensTranslationRequest> {
    val batch = mutableListOf<ReaderLensTranslationRequest>()
    for (index in startIndex until requests.size) {
        val candidate = batch + requests[index]
        val candidatePrompt = buildIndexTranslationPromptForItems(
            items = candidate.map(ReaderLensTranslationRequest::item),
            sourceLanguage = sourceLanguage,
            previousContext = previousContext,
        )
        if (batch.isNotEmpty() && candidatePrompt.encodeToByteArray().size > READER_LENS_MAX_PROMPT_BYTES) break
        if (candidatePrompt.encodeToByteArray().size > READER_LENS_MAX_PROMPT_BYTES) {
            throw ReaderLensTranslationException("Une bulle OCR dépasse la capacité du modèle Lens")
        }
        batch += requests[index]
    }
    return batch
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

private fun String.takeLastUtf8(maxBytes: Int): String {
    var start = length
    var bytes = 0
    while (start > 0) {
        val codePoint = codePointBefore(start)
        val encodedBytes = String(Character.toChars(codePoint)).encodeToByteArray().size
        if (bytes + encodedBytes > maxBytes) break
        bytes += encodedBytes
        start -= Character.charCount(codePoint)
    }
    return substring(start)
}

internal fun parseIndexTranslation(
    raw: String,
    expectedIds: List<String>,
): Map<String, String> {
    val structuredResponse = raw.trim().withoutLeadingThinkingEnvelope()
    val items = try {
        STRICT_TRANSLATION_JSON.decodeFromString<List<IndexTranslationItem>>(structuredResponse)
    } catch (error: SerializationException) {
        throw ReaderLensTranslationException("La réponse du modèle Lens n'est pas un JSON valide", error)
    } catch (error: IllegalArgumentException) {
        throw ReaderLensTranslationException("La réponse du modèle Lens est invalide", error)
    }

    val actualIds = items.map(IndexTranslationItem::id)
    if (actualIds != expectedIds || actualIds.toSet().size != actualIds.size) {
        throw ReaderLensTranslationException("Le modèle Lens n'a pas conservé l'ordre des bulles")
    }
    if (items.any { it.text.isBlank() }) {
        throw ReaderLensTranslationException("Le modèle Lens a renvoyé une bulle vide")
    }
    return items.associateTo(linkedMapOf()) { item -> item.id to item.text.trim() }
}

private fun String.withoutLeadingThinkingEnvelope(): String {
    if (!startsWith(THINKING_START_TAG)) return this
    val closingTagIndex = indexOf(THINKING_END_TAG, startIndex = THINKING_START_TAG.length)
    if (closingTagIndex < 0) {
        throw ReaderLensTranslationException("Le raisonnement du modèle Lens est incomplet")
    }
    return substring(closingTagIndex + THINKING_END_TAG.length).trim()
}

internal const val READER_LENS_PROMPT_SCHEMA_VERSION = 2

private val STRICT_TRANSLATION_JSON = Json {
    ignoreUnknownKeys = false
    isLenient = false
    explicitNulls = false
    coerceInputValues = false
}

private const val THINKING_START_TAG = "<think>"
private const val THINKING_END_TAG = "</think>"
internal const val READER_LENS_MAX_PROMPT_BYTES = 2_500
private const val READER_LENS_CONTEXT_BYTES = 300
private const val READER_LENS_SEGMENT_BYTES = 1_000
private const val READER_LENS_SEGMENT_CODE_POINTS = 1_000
