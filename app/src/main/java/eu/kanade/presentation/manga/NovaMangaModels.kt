package eu.kanade.presentation.manga

import java.text.Normalizer

internal class NovaChapterSearch(query: String) {
    private val text = query.trim().foldChapterText()
    private val number = text.filterNot(Char::isWhitespace).replace(',', '.').toDoubleOrNull()

    val isActive: Boolean get() = text.isNotEmpty()

    fun matches(title: String, chapterNumber: Double): Boolean = when {
        !isActive -> true
        number != null && chapterNumber >= 0.0 -> chapterNumber == number
        else -> title.foldChapterText().contains(text)
    }
}

private val chapterDiacritics = Regex("\\p{M}+")

private fun String.foldChapterText(): String =
    chapterDiacritics.replace(Normalizer.normalize(this, Normalizer.Form.NFD), "").lowercase()

enum class NovaPrimaryReadAction {
    Read,
    Resume,
    Reread,
}

enum class NovaChapterReadingState {
    Unread,
    InProgress,
    Read,
}

enum class NovaChapterDownloadState {
    Remote,
    Downloading,
    Downloaded,
    Error,
}

fun resolvePrimaryReadAction(
    totalChapterCount: Int,
    unreadChapterCount: Int,
    hasPageProgress: Boolean,
): NovaPrimaryReadAction = when {
    totalChapterCount > 0 && unreadChapterCount == 0 -> NovaPrimaryReadAction.Reread
    hasPageProgress || unreadChapterCount < totalChapterCount -> NovaPrimaryReadAction.Resume
    else -> NovaPrimaryReadAction.Read
}

fun resolveChapterReadingState(
    isRead: Boolean,
    lastPageRead: Long,
): NovaChapterReadingState = when {
    isRead -> NovaChapterReadingState.Read
    lastPageRead > 0L -> NovaChapterReadingState.InProgress
    else -> NovaChapterReadingState.Unread
}

fun resolveDownloadedState(
    isMarkedDownloaded: Boolean,
    hasLocalArchive: Boolean,
    isDownloadRunning: Boolean,
    hasDownloadError: Boolean,
): NovaChapterDownloadState = when {
    isMarkedDownloaded || hasLocalArchive -> NovaChapterDownloadState.Downloaded
    isDownloadRunning -> NovaChapterDownloadState.Downloading
    hasDownloadError -> NovaChapterDownloadState.Error
    else -> NovaChapterDownloadState.Remote
}

fun novaChapterHeaderSummary(
    chapterCount: Int,
    visibleChapterCount: Int,
    unreadCount: Int,
    filterActive: Boolean,
): String = if (filterActive) {
    "$visibleChapterCount sur $chapterCount affichés"
} else {
    "$chapterCount chapitres - $unreadCount à lire"
}
