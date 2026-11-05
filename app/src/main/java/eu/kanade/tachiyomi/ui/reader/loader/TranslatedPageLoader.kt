package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.data.translation.InstalledTranslation
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

internal class TranslatedPageLoader(
    private val translation: InstalledTranslation,
) : PageLoader() {

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> = translation.pages.mapIndexed { index, file ->
        ReaderPage(index, file.toURI().toString(), stream = file::inputStream).apply {
            status = Page.State.Ready
        }
    }

    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }
}
