package eu.kanade.tachiyomi.ui.library

import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import tachiyomi.source.local.LocalSource

internal class LibrarySearchMetadataIndex(
    private val metadataIds: Set<Long>,
    private val tagsByMangaId: Map<Long, List<SearchTag>>,
    private val titlesByMangaId: Map<Long, List<SearchTitle>>,
) {
    fun contains(mangaId: Long): Boolean = mangaId in metadataIds

    fun tagsFor(mangaId: Long): List<SearchTag> = tagsByMangaId[mangaId].orEmpty()

    fun titlesFor(mangaId: Long): List<SearchTitle> = titlesByMangaId[mangaId].orEmpty()
}

internal fun buildLibrarySearchMetadataIndex(
    metadataIds: Collection<Long>,
    tags: List<SearchTag>,
    titles: List<SearchTitle>,
): LibrarySearchMetadataIndex {
    return LibrarySearchMetadataIndex(
        metadataIds = metadataIds.toHashSet(),
        tagsByMangaId = tags.groupBy(SearchTag::mangaId),
        titlesByMangaId = titles.groupBy(SearchTitle::mangaId),
    )
}

internal fun librarySourceMatchesQuery(sourceId: Long, query: String): Boolean {
    return sourceId != LocalSource.ID && sourceId.toString() == query
}
