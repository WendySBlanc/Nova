package eu.kanade.tachiyomi.ui.browse.migration.search

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel
import kotlinx.coroutines.CancellationException
import mihon.feature.migration.list.search.MigrationSearchQueries
import tachiyomi.data.source.BaseSourcePagingSource
import tachiyomi.domain.source.repository.SourcePagingSource

class MigrationSourceSearchScreenModel(
    sourceId: Long,
    query: String?,
) : BrowseSourceScreenModel(sourceId, query) {

    override fun createSourcePagingSource(query: String, filters: FilterList): SourcePagingSource {
        val catalogueSource = source as? CatalogueSource
            ?: return super.createSourcePagingSource(query, filters)

        return MigrationSourcePagingSource(
            source = catalogueSource,
            queries = MigrationSearchQueries.build(query),
            filters = filters,
        )
    }
}

private class MigrationSourcePagingSource(
    source: CatalogueSource,
    private val queries: List<String>,
    private val filters: FilterList,
) : BaseSourcePagingSource(source) {

    private var activeQuery: String? = null

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        activeQuery?.let { return source.getSearchManga(currentPage, it, filters) }

        var firstEmptyResult: MangasPage? = null
        var firstError: Exception? = null

        for (query in queries) {
            try {
                val result = source.getSearchManga(currentPage, query, filters)
                if (result.mangas.isNotEmpty()) {
                    activeQuery = query
                    return result
                }
                if (firstEmptyResult == null) firstEmptyResult = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (firstError == null) firstError = e
            }
        }

        return firstEmptyResult ?: throw firstError ?: IllegalStateException("No migration search query available")
    }
}
