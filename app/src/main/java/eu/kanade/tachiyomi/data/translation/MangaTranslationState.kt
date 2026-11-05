package eu.kanade.tachiyomi.data.translation

sealed interface MangaTranslationState {
    data object Idle : MangaTranslationState

    data object Preparing : MangaTranslationState

    data object Uploading : MangaTranslationState

    data class Queued(
        val completedPages: Int,
        val totalPages: Int,
    ) : MangaTranslationState

    data class Translating(
        val completedPages: Int,
        val totalPages: Int,
    ) : MangaTranslationState

    data class WaitingForNetwork(
        val attempt: Int,
        val maxAttempts: Int,
    ) : MangaTranslationState

    data object Downloading : MangaTranslationState

    data class Ready(
        val translation: InstalledTranslation,
    ) : MangaTranslationState

    data object Cancelling : MangaTranslationState

    data object Cancelled : MangaTranslationState

    data class Failed(
        val message: String,
        val canRetry: Boolean,
    ) : MangaTranslationState
}

val MangaTranslationState.isActive: Boolean
    get() = when (this) {
        MangaTranslationState.Preparing,
        MangaTranslationState.Uploading,
        is MangaTranslationState.Queued,
        is MangaTranslationState.Translating,
        is MangaTranslationState.WaitingForNetwork,
        MangaTranslationState.Downloading,
        MangaTranslationState.Cancelling,
        -> true
        else -> false
    }

val MangaTranslationState.progress: Pair<Int, Int>?
    get() = when (this) {
        is MangaTranslationState.Queued -> completedPages to totalPages
        is MangaTranslationState.Translating -> completedPages to totalPages
        else -> null
    }
