package eu.kanade.tachiyomi.data.lens

internal data class ProofbirdEncoding(
    val inputIds: LongArray,
    val firstTokenIndices: IntArray,
)

internal class ProofbirdWordPieceTokenizer(
    vocabulary: List<String>,
) {
    private val tokenIds = vocabulary.withIndex().associate { (index, token) -> token to index.toLong() }
    private val unknownTokenId = tokenIds.getValue(UNKNOWN_TOKEN)
    private val classTokenId = tokenIds.getValue(CLASS_TOKEN)
    private val separatorTokenId = tokenIds.getValue(SEPARATOR_TOKEN)

    fun encode(words: List<String>): ProofbirdEncoding {
        val inputIds = mutableListOf(classTokenId)
        val firstTokenIndices = mutableListOf(0)
        words.forEach { word ->
            firstTokenIndices += inputIds.size
            val pieces = basicTokens(word).flatMap(::wordPieces).ifEmpty { listOf(unknownTokenId) }
            inputIds += pieces
        }
        inputIds += separatorTokenId
        return ProofbirdEncoding(inputIds.toLongArray(), firstTokenIndices.toIntArray())
    }

    private fun wordPieces(token: String): List<Long> {
        if (token.codePointCount(0, token.length) > MAX_WORD_CODE_POINTS) return listOf(unknownTokenId)
        val pieces = mutableListOf<Long>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var selectedId: Long? = null
            var selectedEnd = start
            while (end > start) {
                val candidate = buildString {
                    if (start > 0) append(CONTINUATION_PREFIX)
                    append(token, start, end)
                }
                tokenIds[candidate]?.let { id ->
                    selectedId = id
                    selectedEnd = end
                }
                if (selectedId != null) break
                end = token.offsetByCodePoints(end, -1)
            }
            if (selectedId == null) return listOf(unknownTokenId)
            pieces += selectedId
            start = selectedEnd
        }
        return pieces
    }

    private fun basicTokens(word: String): List<String> = buildList {
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                add(current.toString())
                current.clear()
            }
        }

        var offset = 0
        while (offset < word.length) {
            val codePoint = word.codePointAt(offset)
            when {
                Character.isWhitespace(codePoint) || Character.isISOControl(codePoint) -> flush()
                isPunctuation(codePoint) -> {
                    flush()
                    add(String(Character.toChars(codePoint)))
                }
                else -> current.appendCodePoint(codePoint)
            }
            offset += Character.charCount(codePoint)
        }
        flush()
    }

    private fun isPunctuation(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(),
        -> true
        else -> false
    }
}

internal fun proofbirdWords(text: String): List<String> = PROOFBIRD_WORD.findAll(text)
    .map(MatchResult::value)
    .toList()

internal fun applyProofbirdEdits(
    words: List<String>,
    labels: List<String>,
    verbForms: Map<String, String> = emptyMap(),
): List<String> {
    require(labels.size == words.size + 1) { "Proofbird must return one label for START and every word" }
    val edited = (listOf(START_TOKEN) + words).zip(labels) { word, label ->
        applyProofbirdEdit(word, label, verbForms)
    }
    val merged = edited
        .filter(String::isNotEmpty)
        .joinToString(" ")
        .replace(" $MERGE_HYPHEN ", "-")
        .replace(" $MERGE_SPACE ", "")
        .split(PROOFBIRD_WHITESPACE)
        .filter { it.isNotEmpty() && it != START_TOKEN }
    return rejoinApostrophes(merged).flatMap(::proofbirdWords)
}

private fun applyProofbirdEdit(
    word: String,
    label: String,
    verbForms: Map<String, String>,
): String = when {
    label.startsWith(APPEND_PREFIX) -> "$word ${label.removePrefix(APPEND_PREFIX)}"
    word == START_TOKEN || label == KEEP_LABEL -> word
    label.startsWith(REPLACE_PREFIX) -> label.removePrefix(REPLACE_PREFIX)
    label == DELETE_LABEL -> ""
    label.startsWith(SUFFIX_PREFIX) -> {
        val (old, new) = label.removePrefix(SUFFIX_PREFIX).split('_', limit = 2).let { parts ->
            parts.first() to parts.getOrElse(1) { "" }
        }
        if (word.endsWith(old)) word.dropLast(old.length) + new else word
    }
    label.startsWith(VERB_PREFIX) -> verbForms["${word}_${label.removePrefix(VERB_PREFIX)}"] ?: word
    label == CAPITAL_CASE_LABEL -> word.replaceFirstChar { it.titlecase() }.lowercase().replaceFirstChar { it.titlecase() }
    label == LOWER_CASE_LABEL -> word.lowercase()
    label == UPPER_CASE_LABEL -> word.uppercase()
    label == PLURAL_LABEL -> word + "s"
    label == SINGULAR_LABEL -> word.dropLast(1)
    label == SPLIT_HYPHEN_LABEL -> word.replace('-', ' ')
    label.startsWith(MERGE_PREFIX) -> "$word $label"
    else -> word
}

internal fun joinProofbirdWords(words: List<String>): String = buildString {
    words.forEach { word ->
        val needsSpace = isNotEmpty() &&
            word.firstOrNull() !in CLOSING_MARKS &&
            lastOrNull() !in OPENING_MARKS
        if (needsSpace) append(' ')
        append(word)
    }
}

private fun rejoinApostrophes(tokens: List<String>): List<String> = buildList {
    tokens.forEach { token ->
        if (isNotEmpty() && last().length > 1 && last().endsWith('\'') && token.firstOrNull()?.isLetterOrDigit() == true) {
            this[lastIndex] = last() + token
        } else {
            add(token)
        }
    }
}

private val PROOFBIRD_WORD = Regex("[\\p{L}\\p{M}\\p{N}_]+(?:'[\\p{L}\\p{M}\\p{N}_]+)?|[^\\p{L}\\p{M}\\p{N}_\\s]")
private val PROOFBIRD_WHITESPACE = Regex("\\s+")
private const val START_TOKEN = "\$START"
private const val CLASS_TOKEN = "[CLS]"
private const val SEPARATOR_TOKEN = "[SEP]"
private const val UNKNOWN_TOKEN = "[UNK]"
private const val CONTINUATION_PREFIX = "##"
private const val MAX_WORD_CODE_POINTS = 100
private const val KEEP_LABEL = "\$KEEP"
private const val DELETE_LABEL = "\$DELETE"
private const val APPEND_PREFIX = "\$APPEND_"
private const val REPLACE_PREFIX = "\$REPLACE_"
private const val SUFFIX_PREFIX = "\$TRANSFORM_SUF_"
private const val VERB_PREFIX = "\$TRANSFORM_VERB_"
private const val CAPITAL_CASE_LABEL = "\$TRANSFORM_CASE_CAPITAL"
private const val LOWER_CASE_LABEL = "\$TRANSFORM_CASE_LOWER"
private const val UPPER_CASE_LABEL = "\$TRANSFORM_CASE_UPPER"
private const val PLURAL_LABEL = "\$TRANSFORM_AGREEMENT_PLURAL"
private const val SINGULAR_LABEL = "\$TRANSFORM_AGREEMENT_SINGULAR"
private const val SPLIT_HYPHEN_LABEL = "\$TRANSFORM_SPLIT_HYPHEN"
private const val MERGE_PREFIX = "\$MERGE_"
private const val MERGE_HYPHEN = "\$MERGE_HYPHEN"
private const val MERGE_SPACE = "\$MERGE_SPACE"
private val OPENING_MARKS = setOf('(', '[', '{', '“', '«', '"')
private val CLOSING_MARKS = setOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '%', '»', '”')
