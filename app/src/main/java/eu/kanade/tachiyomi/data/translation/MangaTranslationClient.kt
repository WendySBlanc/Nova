package eu.kanade.tachiyomi.data.translation

import eu.kanade.tachiyomi.network.await
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class MangaTranslationClient(
    client: OkHttpClient,
    private val json: Json,
    private val connection: () -> MangaTranslationConnection,
    callTimeout: Duration = 10.minutes,
) : MangaTranslationService {
    private val client = client.newBuilder()
        .callTimeout(callTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun checkHealth() {
        val request = requestBuilder("v1/health").get().build()
        executeBody(request).close()
    }

    override suspend fun submitChapter(chapterKey: String, archive: File): TranslationJob {
        require(chapterKey.isNotBlank()) { "chapterKey cannot be blank" }
        require(archive.isFile) { "Translation archive does not exist" }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chapter_key", chapterKey)
            .addFormDataPart(
                "archive",
                archive.name,
                archive.asRequestBody(ZIP_MEDIA_TYPE),
            )
            .build()
        val request = requestBuilder("v1/chapters").post(body).build()
        return executeJson(request)
    }

    override suspend fun getJob(jobId: String): TranslationJob {
        val request = requestBuilder("v1/chapters/${encodeSegment(jobId)}").get().build()
        return executeJson(request)
    }

    override suspend fun downloadResult(jobId: String, destination: File): TranslationResultMetadata {
        val job = getJob(jobId)
        if (job.status != TranslationJobStatus.COMPLETE) {
            throw MangaTranslationProtocolException("La traduction n'est pas terminée")
        }
        val targetLanguage = job.targetLanguage
            ?: throw MangaTranslationProtocolException("Langue cible absente de la réponse")
        val configRevision = job.configRevision
            ?: throw MangaTranslationProtocolException("Révision de configuration absente de la réponse")
        val request = requestBuilder("v1/chapters/${encodeSegment(jobId)}/result").get().build()
        val temporary = File(destination.parentFile, "${destination.name}.part")
        temporary.parentFile?.mkdirs()
        try {
            executeBody(request).use { body ->
                temporary.outputStream().buffered().use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
            }
            promoteAtomically(temporary, destination)
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
        return TranslationResultMetadata(
            chapterKey = job.chapterKey,
            targetLanguage = targetLanguage,
            configRevision = configRevision,
            totalPages = job.totalPages,
        )
    }

    override suspend fun cancelJob(jobId: String) {
        val emptyBody = ByteArray(0).toRequestBody(null)
        val request = requestBuilder("v1/chapters/${encodeSegment(jobId)}").delete(emptyBody).build()
        executeBody(request).close()
    }

    private fun requestBuilder(relativePath: String): Request.Builder {
        val current = connection()
        if (!current.isConfigured) {
            throw MangaTranslationProtocolException("Serveur MangaTranslator non configuré")
        }
        val url = normalizeMangaTranslationBaseUrl(current.baseUrl).resolve(relativePath)
            ?: throw MangaTranslationProtocolException("Adresse MangaTranslator invalide")
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${current.accessToken}")
            .header("Accept", "application/json, application/zip")
    }

    private suspend inline fun <reified T> executeJson(request: Request): T {
        val body = executeBody(request).use { it.string() }
        return try {
            json.decodeFromString(body)
        } catch (error: SerializationException) {
            throw MangaTranslationProtocolException("Réponse MangaTranslator invalide", error)
        }
    }

    private suspend fun executeBody(request: Request): okhttp3.ResponseBody {
        val response = try {
            client.newCall(request).await()
        } catch (error: IOException) {
            throw MangaTranslationNetworkException(error)
        }
        if (!response.isSuccessful) {
            val detail = response.use {
                val body = it.body.string()
                runCatching { json.decodeFromString<MangaTranslationApiError>(body).detail }.getOrNull()
            }
            throw MangaTranslationHttpException(response.code, detail)
        }
        return response.body
    }

    private fun promoteAtomically(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

internal fun normalizeMangaTranslationBaseUrl(rawUrl: String): HttpUrl {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) {
        throw MangaTranslationProtocolException("Adresse requise")
    }
    val explicitScheme = SCHEME_PATTERN.find(trimmed)
        ?.value
        ?.removeSuffix("://")
        ?.lowercase()
    if (explicitScheme != null && explicitScheme !in SUPPORTED_SCHEMES) {
        throw MangaTranslationProtocolException("Le serveur doit utiliser HTTP ou HTTPS")
    }
    val withScheme = if (SCHEME_PATTERN.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
    val parsed = withScheme.toHttpUrlOrNull()
        ?: throw MangaTranslationProtocolException("Adresse MangaTranslator invalide")
    if (parsed.scheme != "http" && parsed.scheme != "https") {
        throw MangaTranslationProtocolException("Le serveur doit utiliser HTTP ou HTTPS")
    }
    val normalizedPath = parsed.encodedPath.trimEnd('/') + "/"
    return parsed.newBuilder()
        .encodedPath(normalizedPath)
        .query(null)
        .fragment(null)
        .build()
}

internal fun validateMangaTranslationServerUrl(rawUrl: String): String? = runCatching {
    normalizeMangaTranslationBaseUrl(rawUrl)
}.exceptionOrNull()?.message

private fun encodeSegment(value: String): String = HttpUrl.Builder()
    .scheme("https")
    .host("localhost")
    .addPathSegment(value)
    .build()
    .encodedPathSegments
    .last()

private val SCHEME_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
private val SUPPORTED_SCHEMES = setOf("http", "https")
private val ZIP_MEDIA_TYPE = "application/zip".toMediaType()
