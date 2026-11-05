package eu.kanade.tachiyomi.data.translation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class MangaTranslationConnection(
    val baseUrl: String,
    val accessToken: String,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && accessToken.isNotBlank()
}

@Serializable
enum class TranslationJobStatus {
    @SerialName("queued")
    QUEUED,

    @SerialName("running")
    RUNNING,

    @SerialName("complete")
    COMPLETE,

    @SerialName("failed")
    FAILED,

    @SerialName("cancelled")
    CANCELLED,
}

@Serializable
data class TranslationJob(
    @SerialName("job_id") val jobId: String,
    @SerialName("chapter_key") val chapterKey: String,
    val status: TranslationJobStatus,
    @SerialName("completed_pages") val completedPages: Int,
    @SerialName("total_pages") val totalPages: Int,
    val error: String? = null,
    @SerialName("target_language") val targetLanguage: String? = null,
    @SerialName("config_revision") val configRevision: String? = null,
    val cached: Boolean = false,
)

@Serializable
data class TranslationResultMetadata(
    val chapterKey: String,
    val targetLanguage: String,
    val configRevision: String,
    val totalPages: Int,
)

@Serializable
internal data class MangaTranslationApiError(
    val detail: String? = null,
)

open class MangaTranslationException(message: String, cause: Throwable? = null) : Exception(message, cause)

class MangaTranslationHttpException(
    val statusCode: Int,
    detail: String? = null,
) : MangaTranslationException(
    buildString {
        append("MangaTranslator HTTP ")
        append(statusCode)
        if (!detail.isNullOrBlank()) {
            append(": ")
            append(detail.take(MAX_ERROR_DETAIL_LENGTH))
        }
    },
)

class MangaTranslationNetworkException(cause: Throwable) :
    MangaTranslationException("Impossible de joindre le serveur MangaTranslator", cause)

class MangaTranslationProtocolException(message: String, cause: Throwable? = null) :
    MangaTranslationException(message, cause)

private const val MAX_ERROR_DETAIL_LENGTH = 240
