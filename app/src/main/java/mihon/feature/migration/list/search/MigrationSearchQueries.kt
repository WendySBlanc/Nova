package mihon.feature.migration.list.search

import tachiyomi.core.common.util.QuerySanitizer.sanitize
import java.text.Normalizer
import java.util.Locale

object MigrationSearchQueries {

    fun build(title: String): List<String> {
        val original = title.sanitize()
        if (original.isBlank()) return listOf("")

        val withoutBrackets = original
            .replace(BRACKETED_TEXT, " ")
            .normalizeSpaces()
        val words = withoutBrackets
            .replace(PUNCTUATION, " ")
            .normalizeSpaces()
            .split(' ')
            .filter { it.length >= MIN_WORD_LENGTH }
        val meaningfulWords = words.filterNot { it.lowercase(Locale.FRENCH) in COMMON_WORDS }
            .ifEmpty { words }
        val distinctiveWords = meaningfulWords.sortedByDescending(String::length)

        return buildList {
            add(original)
            add(withoutBrackets)
            add(words.joinToString(" "))
            add(removeDiacritics(words.joinToString(" ")))
            add(distinctiveWords.take(2).joinToString(" "))
            add(words.take(2).joinToString(" "))
            distinctiveWords.forEach(::add)
        }
            .map { it.sanitize() }
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase(Locale.ROOT) }
    }

    private fun String.normalizeSpaces() = replace(CONSECUTIVE_SPACES, " ").trim()

    private fun removeDiacritics(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
    }

    private const val MIN_WORD_LENGTH = 3

    private val BRACKETED_TEXT = Regex("""\([^)]*\)|\[[^\]]*\]|\{[^}]*\}""")
    private val PUNCTUATION = Regex("""[^\p{L}\p{N}]+""")
    private val CONSECUTIVE_SPACES = Regex("""\s+""")
    private val DIACRITICS = Regex("""\p{M}+""")
    private val COMMON_WORDS = setOf(
        "and",
        "des",
        "les",
        "the",
        "une",
    )
}
