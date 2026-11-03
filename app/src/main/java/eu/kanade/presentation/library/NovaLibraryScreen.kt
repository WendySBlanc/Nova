package eu.kanade.presentation.library

import androidx.compose.runtime.Composable
import eu.kanade.presentation.library.components.NovaLibraryCategory
import eu.kanade.presentation.library.components.NovaVikiLibraryScreen

@Composable
fun NovaLibraryScreen(
    libraryItems: List<NovaLibraryEntry>,
    lightNovelItems: List<NovaLibraryEntry>,
    categories: List<NovaLibraryCategory>,
    isLibraryReady: Boolean,
    onPlay: (Long?) -> Unit,
    onSearch: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onSetArchived: (Long, Boolean) -> Unit,
    onRefresh: () -> Boolean,
) {
    NovaVikiLibraryScreen(
        libraryItems = libraryItems,
        lightNovelItems = lightNovelItems,
        categories = categories,
        isLibraryReady = isLibraryReady,
        onPlay = onPlay,
        onSearch = onSearch,
        onOpenManga = onOpenManga,
        onSetArchived = onSetArchived,
        onRefresh = onRefresh,
    )
}
