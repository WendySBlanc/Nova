package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.CancellationException
import tachiyomi.domain.manga.model.Manga
import java.util.Locale

data class ChapterFetchResult(
    val chapters: List<SChapter>,
    val details: SManga? = null,
    val resolvedUrl: String? = null,
)

/**
 * Recovers chapter lists when a source changes a series URL without migrating existing library entries.
 */
suspend fun Source.getChapterListWithRecovery(manga: Manga): ChapterFetchResult {
    val localManga = manga.toSManga()
    var initialError: Throwable? = null
    val initialChapters = try {
        getChapterList(localManga)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        initialError = e
        emptyList()
    }
    initialChapters.takeIf { it.isNotEmpty() }?.let {
        return ChapterFetchResult(chapters = it)
    }

    val refreshedDetails = tryForRecovery { getMangaDetails(localManga) }
    if (refreshedDetails != null) {
        tryForRecovery { getChapterList(refreshedDetails) }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                return ChapterFetchResult(
                    chapters = it,
                    details = refreshedDetails,
                    resolvedUrl = refreshedDetails.safeUrl(),
                )
            }
    }

    val catalogueSource = this as? CatalogueSource ?: run {
        initialError?.let { throw it }
        return ChapterFetchResult(emptyList())
    }
    val knownTitles = listOf(manga.ogTitle, manga.title)
        .map(::normalizeTitle)
        .filter { it.isNotEmpty() }
        .distinct()

    for (query in listOf(manga.ogTitle, manga.title).filter { it.isNotBlank() }.distinct()) {
        val exactMatches = tryForRecovery {
            catalogueSource.getSearchManga(1, query, catalogueSource.getFilterList()).mangas
        }
            .orEmpty()
            .filter { normalizeTitle(it.safeTitle()) in knownTitles }
            .filter { !it.safeUrl().isNullOrBlank() }
            .distinctBy { it.safeUrl() }

        // Multiple exact results are ambiguous and must not silently migrate the library entry.
        val candidate = exactMatches.singleOrNull() ?: continue
        val candidateUrl = candidate.safeUrl() ?: continue
        val candidateDetails = tryForRecovery { getMangaDetails(candidate) }
        val chapterTarget = candidateDetails?.apply {
            if (safeUrl().isNullOrBlank()) url = candidateUrl
        } ?: candidate
        val chapters = tryForRecovery { getChapterList(chapterTarget) }.orEmpty()

        if (chapters.isNotEmpty()) {
            return ChapterFetchResult(
                chapters = chapters,
                details = candidateDetails,
                resolvedUrl = candidateUrl,
            )
        }
    }

    initialError?.let { throw it }
    return ChapterFetchResult(emptyList())
}

private suspend fun <T> tryForRecovery(block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }
}

private fun SManga.safeTitle(): String = runCatching { originalTitle }.getOrDefault("")

private fun SManga.safeUrl(): String? = runCatching { url.trim() }.getOrNull()?.takeIf { it.isNotEmpty() }

private fun normalizeTitle(title: String): String {
    return title
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)
}
