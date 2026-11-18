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
        val expectedIds = blocks.indices.map { index -> "b$index" }
        val prompt = buildIndexTranslationPrompt(blocks, sourceLanguage, previousContext)
        val model = modelProvider()
        val response = runtime.complete(model, prompt)
        return parseIndexTranslation(response, expectedIds)
    }

    override fun close() {
        runtime.close()
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
): String {
    val payload = blocks.mapIndexed { index, block ->
        IndexTranslationItem(id = "b$index", text = block.text)
    }
    val context = previousContext
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { "Contexte précédent en lecture seule : $it\n" }
        .orEmpty()

    return buildString {
        appendLine("Traduis cette page de manhwa de la langue '$sourceLanguage' vers un français naturel et concis.")
        appendLine("Traduis le sens plutôt que la structure anglaise, conserve les noms propres et adapte le registre aux personnages.")
        appendLine("Évite les calques et les formulations administratives. Préfère une formulation française directe.")
        appendLine(
            "Exemple de style : \"The library is a facility provided to students by the academy\" devient " +
                "\"L'académie met la bibliothèque à la disposition des élèves\".",
        )
        appendLine("Réponds uniquement avec un tableau JSON. Garde exactement chaque id, dans le même ordre, sans champ supplémentaire.")
        append(context)
        append(Json.encodeToString(payload))
    }
}

internal fun parseIndexTranslation(
    raw: String,
    expectedIds: List<String>,
): Map<String, String> {
    val items = try {
        STRICT_TRANSLATION_JSON.decodeFromString<List<IndexTranslationItem>>(raw.trim())
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

internal const val READER_LENS_PROMPT_SCHEMA_VERSION = 1

private val STRICT_TRANSLATION_JSON = Json {
    ignoreUnknownKeys = false
    isLenient = false
    explicitNulls = false
    coerceInputValues = false
}
