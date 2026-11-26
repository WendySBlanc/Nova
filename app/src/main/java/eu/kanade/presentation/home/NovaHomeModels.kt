package eu.kanade.presentation.home

import eu.kanade.presentation.library.NovaLibraryEntry
import tachiyomi.domain.chapter.model.Chapter

data class NovaHomeSections(
    val resume: List<NovaLibraryEntry> = emptyList(),
    val newChapters: List<NovaLibraryEntry> = emptyList(),
    val recentlyAdded: List<NovaLibraryEntry> = emptyList(),
) {
    fun featuredItems(maxItems: Int = 5): List<NovaLibraryEntry> =
        resume.ifEmpty { newChapters }.ifEmpty { recentlyAdded }.take(maxItems)

    fun visibleRows(
        showUpdates: Boolean,
        showRecent: Boolean,
        recentFirst: Boolean,
    ): List<Pair<NovaHomeRow, List<NovaLibraryEntry>>> {
        val rows = buildList {
            if (showUpdates && newChapters.isNotEmpty()) add(NovaHomeRow.NewChapters to newChapters)
            if (showRecent && recentlyAdded.isNotEmpty()) add(NovaHomeRow.RecentlyAdded to recentlyAdded)
        }
        return if (recentFirst) rows.asReversed() else rows
    }
}

enum class NovaHomeRow {
    NewChapters,
    RecentlyAdded,
}

internal fun selectNovaResumeChapter(chapters: List<Chapter>, lastReadChapterId: Long): Chapter? {
    val lastReadIndex = chapters.indexOfFirst { it.id == lastReadChapterId }
    return chapters.asSequence().drop(lastReadIndex.coerceAtLeast(0)).firstOrNull { !it.read }
        ?: chapters.firstOrNull { !it.read }
}

fun deriveNovaHomeSections(
    items: List<NovaLibraryEntry>,
    maxResume: Int = 8,
    maxRecent: Int = 12,
): NovaHomeSections {
    require(maxResume >= 0) { "maxResume must be non-negative" }
    require(maxRecent >= 0) { "maxRecent must be non-negative" }

    val uniqueItems = items.distinctBy(NovaLibraryEntry::id)
    return NovaHomeSections(
        resume = uniqueItems
            .asSequence()
            .filter { item -> item.lastRead > 0L }
            .sortedWith(
                compareByDescending<NovaLibraryEntry> { it.remainingChapters > 0L }
                    .thenByDescending { it.lastRead },
            )
            .take(maxResume)
            .toList(),
        newChapters = uniqueItems
            .asSequence()
            .filter { item -> item.remainingChapters > 0L }
            .sortedWith(
                compareByDescending<NovaLibraryEntry> { item -> item.latestUpload }
                    .thenByDescending { item -> item.chapterFetchedAt },
            )
            .take(maxRecent)
            .toList(),
        recentlyAdded = uniqueItems
            .sortedByDescending(NovaLibraryEntry::dateAdded)
            .take(maxRecent),
    )
}
