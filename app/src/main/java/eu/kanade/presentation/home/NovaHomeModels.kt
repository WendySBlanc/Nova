package eu.kanade.presentation.home

import eu.kanade.presentation.library.NovaLibraryEntry

data class NovaHomeSections(
    val resume: List<NovaLibraryEntry> = emptyList(),
    val newChapters: List<NovaLibraryEntry> = emptyList(),
    val recentlyAdded: List<NovaLibraryEntry> = emptyList(),
)

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
            .sortedByDescending(NovaLibraryEntry::lastRead)
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
