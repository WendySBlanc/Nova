package eu.kanade.tachiyomi.data.lens

import android.content.res.AssetManager
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import java.io.File

internal interface ProofbirdGectorRuntime : AutoCloseable {
    suspend fun predict(modelFile: File, encodings: List<ProofbirdEncoding>): List<IntArray>

    fun releaseWarmModel() = Unit

    override fun close() = Unit
}

internal class ProofbirdFrenchCorrector(
    private val modelProvider: suspend () -> File,
    private val runtime: ProofbirdGectorRuntime,
    private val tokenizer: ProofbirdWordPieceTokenizer,
    private val labels: List<String>,
) : ReaderLensFrenchCorrector {

    override suspend fun correctPage(translations: Map<String, String>): Map<String, String> {
        if (translations.isEmpty()) return emptyMap()
        val states = translations.map { (id, text) -> prepareState(id, text) }
        val modelFile = modelProvider()

        repeat(MAX_CORRECTION_PASSES) {
            val active = states.filterNot(CorrectionState::done)
            if (active.isEmpty()) return@repeat
            val encoded = active.map { state -> tokenizer.encode(state.words) }
            active.zip(encoded).forEach { (state, encoding) ->
                if (encoding.inputIds.size > MAX_MODEL_TOKENS) state.done = true
            }
            val runnable = active.zip(encoded).filter { (state) -> !state.done }
            if (runnable.isEmpty()) return@repeat
            runnable.chunked(MAX_CORRECTION_BATCH_SIZE).forEach { batch ->
                val predictions = runtime.predict(modelFile, batch.map { it.second })
                check(predictions.size == batch.size) { "Proofbird returned an invalid batch" }
                batch.zip(predictions).forEach { (input, prediction) ->
                    val state = input.first
                    check(prediction.size == state.words.size + 1) { "Proofbird returned invalid word labels" }
                    val predictedLabels = prediction.map { labelIndex ->
                        labels.getOrElse(labelIndex) { throw IllegalStateException("Proofbird returned an unknown label") }
                    }
                    val correctedWords = applyProofbirdEdits(state.words, predictedLabels)
                    if (correctedWords == state.words) {
                        state.done = true
                    } else {
                        state.words = correctedWords
                        state.changed = true
                    }
                }
            }
        }

        return states.associateTo(linkedMapOf()) { state ->
            val modelOutput = if (state.changed) {
                restoreProofbirdSurface(state.core, state.originalWords, state.words) + state.tail
            } else {
                state.original
            }
            state.id to correctFrenchOu(modelOutput)
        }
    }

    override fun close() {
        runtime.close()
    }

    override fun releaseWarmModel() {
        runtime.releaseWarmModel()
    }

    private fun prepareState(id: String, text: String): CorrectionState {
        val terminatorIndex = text.indexOfLast { !it.isWhitespace() }
            .takeIf { index -> index >= 0 && text[index] in TRAILING_TERMINATORS }
        val core = terminatorIndex?.let { text.substring(0, it) } ?: text
        val tail = terminatorIndex?.let { text.substring(it) }.orEmpty()
        return CorrectionState(
            id = id,
            original = text,
            core = core,
            originalWords = proofbirdWords(core.replace('’', '\'')),
            words = proofbirdWords(core.replace('’', '\'')),
            tail = tail,
            done = core.isBlank(),
        )
    }
}

internal class ProofbirdReaderLensModelStore(
    rootDirectory: File,
    client: OkHttpClient,
) : AutoCloseable {
    private val delegate = ReaderLensModelStore(
        rootDirectory = rootDirectory,
        client = client,
        spec = PROOFBIRD_READER_LENS_MODEL,
    )

    val state: StateFlow<ReaderLensModelState> = delegate.state

    suspend fun ensureModel(): File = delegate.ensureModel()

    fun cancelDownload() = delegate.cancelDownload()

    override fun close() = delegate.close()
}

internal data class ProofbirdGectorAssets(
    val tokenizer: ProofbirdWordPieceTokenizer,
    val labels: List<String>,
)

internal fun loadProofbirdGectorAssets(assetManager: AssetManager): ProofbirdGectorAssets {
    fun readLines(name: String): List<String> = assetManager.open("$PROOFBIRD_ASSET_DIRECTORY/$name")
        .bufferedReader(Charsets.UTF_8)
        .use { reader -> reader.readLines() }

    return ProofbirdGectorAssets(
        tokenizer = ProofbirdWordPieceTokenizer(readLines("vocab.txt")),
        labels = readLines("labels.txt"),
    )
}

private data class CorrectionState(
    val id: String,
    val original: String,
    val core: String,
    val originalWords: List<String>,
    var words: List<String>,
    val tail: String,
    var done: Boolean,
    var changed: Boolean = false,
)

internal fun restoreProofbirdSurface(
    original: String,
    originalWords: List<String>,
    correctedWords: List<String>,
): String {
    if (originalWords == correctedWords) return original
    val normalized = original.replace('’', '\'')
    val spans = PROOFBIRD_SURFACE_WORD.findAll(normalized).map { it.range }.toList()
    if (spans.size != originalWords.size) return joinProofbirdWords(correctedWords)

    val anchors = longestCommonWordSubsequence(originalWords, correctedWords) +
        listOf(originalWords.size to correctedWords.size)
    return buildString {
        var originalCursor = 0
        var previousOriginal = -1
        var previousCorrected = -1
        anchors.forEach { (originalAnchor, correctedAnchor) ->
            val originalStart = previousOriginal + 1
            val correctedStart = previousCorrected + 1
            if (originalStart < originalAnchor || correctedStart < correctedAnchor) {
                val replacement = joinProofbirdWords(correctedWords.subList(correctedStart, correctedAnchor))
                if (originalStart < originalAnchor) {
                    val changedStart = spans[originalStart].first
                    val changedEnd = spans[originalAnchor - 1].last + 1
                    append(original, originalCursor, changedStart)
                    append(replacement)
                    originalCursor = changedEnd
                } else if (replacement.isNotEmpty()) {
                    if (isNotEmpty() && last().isLetterOrDigit() && replacement.first().isLetterOrDigit()) append(' ')
                    append(replacement)
                }
            }
            if (originalAnchor < originalWords.size) {
                val anchorEnd = spans[originalAnchor].last + 1
                append(original, originalCursor, anchorEnd)
                originalCursor = anchorEnd
            }
            previousOriginal = originalAnchor
            previousCorrected = correctedAnchor
        }
        append(original, originalCursor, original.length)
    }
}

private fun longestCommonWordSubsequence(
    original: List<String>,
    corrected: List<String>,
): List<Pair<Int, Int>> {
    val lengths = Array(original.size + 1) { IntArray(corrected.size + 1) }
    for (originalIndex in original.indices.reversed()) {
        for (correctedIndex in corrected.indices.reversed()) {
            lengths[originalIndex][correctedIndex] = if (original[originalIndex] == corrected[correctedIndex]) {
                lengths[originalIndex + 1][correctedIndex + 1] + 1
            } else {
                maxOf(lengths[originalIndex + 1][correctedIndex], lengths[originalIndex][correctedIndex + 1])
            }
        }
    }
    return buildList {
        var originalIndex = 0
        var correctedIndex = 0
        while (originalIndex < original.size && correctedIndex < corrected.size) {
            when {
                original[originalIndex] == corrected[correctedIndex] -> {
                    add(originalIndex to correctedIndex)
                    originalIndex++
                    correctedIndex++
                }
                lengths[originalIndex + 1][correctedIndex] >= lengths[originalIndex][correctedIndex + 1] -> originalIndex++
                else -> correctedIndex++
            }
        }
    }
}

private const val MAX_CORRECTION_PASSES = 5
private const val MAX_CORRECTION_BATCH_SIZE = 8
private const val MAX_MODEL_TOKENS = 512
private val TRAILING_TERMINATORS = setOf('.', '!', '?')
private const val PROOFBIRD_ASSET_DIRECTORY = "reader-lens/proofbird"
private val PROOFBIRD_SURFACE_WORD = Regex("[\\p{L}\\p{M}\\p{N}_]+(?:'[\\p{L}\\p{M}\\p{N}_]+)?|[^\\p{L}\\p{M}\\p{N}_\\s]")

private fun correctFrenchOu(text: String): String {
    val indirect = OU_INDIRECT.replace(text) { match ->
        match.groupValues[1] + match.groupValues[2].toFrenchWhere()
    }
    return OU_FINAL.replace(indirect) { match ->
        match.groupValues[1] + match.groupValues[2].toFrenchWhere() + match.groupValues[3]
    }
}

private fun String.toFrenchWhere(): String = if (firstOrNull()?.isUpperCase() == true) "Où" else "où"

private val OU_INDIRECT = Regex(
    pattern =
    """(\b(?:(?:ne|n['’])\s*(?:sais|sait|savons|savez|savent|savais|savait|savions|saviez|savaient|saurais|saurait)\s+(?:pas|plus|jamais|vraiment\s+pas|pas\s+trop|pas\s+du\s+tout)\s+|(?:me|te|se|nous|vous)\s+(?:demande|demandes|demandons|demandez|demandent|demander)\s+|(?:dis-moi|dites-moi|dis-nous|dites-nous|explique-moi|expliquez-moi|montre-moi|montrez-moi|devine|devinez)\s+|aucune\s+idée\s+))(ou)(?=\s+(?:je|j['’]|tu|il|elle|on|nous|vous|ils|elles|ce|ça|cela|la|le|les|est|sont|sera|seront|va|vont|aller|habite|habitent|vit|vivent|trouve|trouvent|mettre|ranger|acheter|vivre|dormir|manger|partir)\b)""",
    option = RegexOption.IGNORE_CASE,
)
private val OU_FINAL = Regex("""(\S\s+)(ou)(\s*[.!…]*\s*$)""", RegexOption.IGNORE_CASE)
