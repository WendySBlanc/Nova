package eu.kanade.presentation.library

import androidx.compose.runtime.Composable
import eu.kanade.presentation.library.components.NovaLibraryCategory
import eu.kanade.presentation.library.components.NovaVikiLibraryScreen

@Composable
fun NovaLibraryScreen(
    libraryItems: List<NovaLibraryEntry>,
    lightNovelItems: List<NovaLibraryEntry>,
    heroItems: List<NovaLibraryEntry>,
    categories: List<NovaLibraryCategory>,
    isLibraryReady: Boolean,
    onPlay: (Long?) -> Unit,
    onSearch: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onRefresh: () -> Boolean,
) {
    NovaVikiLibraryScreen(
        libraryItems = libraryItems,
        lightNovelItems = lightNovelItems,
        heroItems = heroItems,
        categories = categories,
        isLibraryReady = isLibraryReady,
        onPlay = onPlay,
        onSearch = onSearch,
        onOpenManga = onOpenManga,
        onRefresh = onRefresh,
    )
}
