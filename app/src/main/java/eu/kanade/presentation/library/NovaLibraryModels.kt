package eu.kanade.presentation.library

import androidx.compose.ui.graphics.Color

enum class NovaMediaType {
    Manhwa,
    LightNovel,
}

enum class NovaLibraryEmptyState {
    Loading,
    EmptyLibrary,
    EmptyFilter,
}

data class NovaLibraryEntry(
    val id: Long,
    val title: String,
    val subtitle: String,
    val tag: String,
    val coverData: Any? = null,
    val colors: List<Color> = listOf(Color(0xFF11352F), Color(0xFF007D6C), Color(0xFF78F8DC)),
    val progress: Float? = null,
    val remainingChapters: Long = 0L,
    val totalChapters: Long = 0L,
    val downloadedChapters: Long = 0L,
    val lastRead: Long = 0L,
    val dateAdded: Long = 0L,
    val latestUpload: Long = 0L,
    val chapterFetchedAt: Long = 0L,
    val mediaType: NovaMediaType = NovaMediaType.Manhwa,
    val categoryIds: Set<Long> = emptySet(),
)

fun coverSaturation(
    progress: Float?,
    remainingChapters: Long = 0L,
): Float = if (remainingChapters > 0L) {
    1f
} else {
    1f - (progress ?: 0f).coerceIn(0f, 1f)
}

fun filterNovaLibraryEntries(
    entries: List<NovaLibraryEntry>,
    mediaType: NovaMediaType,
    categoryId: Long?,
): List<NovaLibraryEntry> = entries.filter { entry ->
    entry.mediaType == mediaType && (categoryId == null || categoryId in entry.categoryIds)
}

fun resolveNovaLibraryEmptyState(
    isReady: Boolean,
    itemCount: Int,
    hasCategoryFilter: Boolean,
): NovaLibraryEmptyState? = when {
    !isReady -> NovaLibraryEmptyState.Loading
    itemCount > 0 -> null
    hasCategoryFilter -> NovaLibraryEmptyState.EmptyFilter
    else -> NovaLibraryEmptyState.EmptyLibrary
}
