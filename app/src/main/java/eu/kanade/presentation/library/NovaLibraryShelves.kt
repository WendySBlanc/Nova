package eu.kanade.presentation.library

import eu.kanade.presentation.library.components.NovaLibraryCategory

const val NOVA_ARCHIVE_CATEGORY_NAME = "Archives"

enum class NovaLibraryShelf {
    Current,
    Archive,
    Collections,
}

fun findNovaArchiveCategory(categories: List<NovaLibraryCategory>): NovaLibraryCategory? = categories
    .asSequence()
    .filter { category -> category.name.equals(NOVA_ARCHIVE_CATEGORY_NAME, ignoreCase = true) }
    .minByOrNull(NovaLibraryCategory::id)

fun novaCollectionCategories(categories: List<NovaLibraryCategory>): List<NovaLibraryCategory> {
    val archiveId = findNovaArchiveCategory(categories)?.id
    return categories.filterNot { category -> category.id == archiveId }
}

fun filterNovaLibraryShelf(
    items: List<NovaLibraryEntry>,
    shelf: NovaLibraryShelf,
    archiveCategoryId: Long?,
): List<NovaLibraryEntry> = when (shelf) {
    NovaLibraryShelf.Current,
    NovaLibraryShelf.Collections,
    -> archiveCategoryId?.let { archiveId ->
        items.filterNot { item -> archiveId in item.categoryIds }
    } ?: items
    NovaLibraryShelf.Archive -> archiveCategoryId?.let { archiveId ->
        items.filter { item -> archiveId in item.categoryIds }
    }.orEmpty()
}
