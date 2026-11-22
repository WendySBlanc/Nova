package tachiyomi.data.source

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.MangasPage
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.NetworkToLocalManga

class SourcePagingSourceTest {

    @Test
    fun `load propagates coroutine cancellation`() = runTest {
        val pagingSource = object : BaseSourcePagingSource(
            source = mockk<CatalogueSource>(relaxed = true),
            networkToLocalManga = mockk<NetworkToLocalManga>(relaxed = true),
        ) {
            override suspend fun requestNextPage(currentPage: Int): MangasPage {
                throw CancellationException("cancelled")
            }
        }

        shouldThrow<CancellationException> {
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 25,
                    placeholdersEnabled = false,
                ),
            )
        }
    }
}
