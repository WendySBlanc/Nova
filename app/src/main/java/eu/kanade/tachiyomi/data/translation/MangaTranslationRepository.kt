package eu.kanade.tachiyomi.data.translation

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MangaTranslationService {
    suspend fun checkHealth()
    suspend fun submitChapter(chapterKey: String, archive: File): TranslationJob
    suspend fun getJob(jobId: String): TranslationJob
    suspend fun downloadResult(jobId: String, destination: File): TranslationResultMetadata
    suspend fun cancelJob(jobId: String)
}

interface MangaTranslationArchiveGateway {
    suspend fun createUpload(chapterKey: String, pages: List<ReaderPage>): File
}

interface MangaTranslationCacheGateway {
    fun installResult(metadata: TranslationResultMetadata, archive: File): InstalledTranslation
    fun translatedPage(chapterKey: String, pageIndex: Int): File?
}

interface MangaTranslationJobStore {
    fun get(chapterKey: String): String?
    fun put(chapterKey: String, jobId: String)
    fun remove(chapterKey: String)
}

class PreferenceMangaTranslationJobStore(
    preferences: MangaTranslationPreferences,
    private val json: Json,
) : MangaTranslationJobStore {
    private val preference = preferences.pendingJobs()

    @Synchronized
    override fun get(chapterKey: String): String? = read()[chapterKey]

    @Synchronized
    override fun put(chapterKey: String, jobId: String) {
        write(read() + (chapterKey to jobId))
    }

    @Synchronized
    override fun remove(chapterKey: String) {
        write(read() - chapterKey)
    }

    private fun read(): Map<String, String> = runCatching {
        json.decodeFromString<Map<String, String>>(preference.get())
    }.getOrDefault(emptyMap())

    private fun write(jobs: Map<String, String>) {
        if (jobs.isEmpty()) {
            preference.delete()
        } else {
            preference.set(json.encodeToString(jobs))
        }
    }
}

class MangaTranslationRepository(
    private val service: MangaTranslationService,
    private val archive: MangaTranslationArchiveGateway,
    private val cache: MangaTranslationCacheGateway,
    private val jobStore: MangaTranslationJobStore,
    private val resultDirectory: File,
    private val pollInterval: Duration = 1.seconds,
    private val retryDelays: List<Duration> = listOf(1.seconds, 2.seconds, 4.seconds),
) {
    private val states = ConcurrentHashMap<String, MutableStateFlow<MangaTranslationState>>()
    private val activeChapters = mutableSetOf<String>()
    private val activeMutex = Mutex()
    private val cancellationRequests = ConcurrentHashMap.newKeySet<String>()

    fun observe(chapterKey: String): StateFlow<MangaTranslationState> =
        state(chapterKey).asStateFlow()

    suspend fun translate(chapterKey: String, pages: List<ReaderPage>) {
        runOnce(chapterKey) {
            val existingJobId = jobStore.get(chapterKey)
            if (existingJobId != null) {
                poll(chapterKey, existingJobId)
            } else {
                submit(chapterKey, pages)
            }
        }
    }

    suspend fun resume(chapterKey: String) {
        val jobId = jobStore.get(chapterKey) ?: return
        runOnce(chapterKey) {
            poll(chapterKey, jobId)
        }
    }

    suspend fun cancel(chapterKey: String) {
        cancellationRequests += chapterKey
        val currentState = state(chapterKey)
        currentState.value = MangaTranslationState.Cancelling
        val jobId = jobStore.get(chapterKey)
        if (jobId == null) {
            currentState.value = MangaTranslationState.Cancelled
            clearCancellationIfInactive(chapterKey)
            return
        }
        try {
            service.cancelJob(jobId)
            jobStore.remove(chapterKey)
            currentState.value = MangaTranslationState.Cancelled
            clearCancellationIfInactive(chapterKey)
        } catch (error: MangaTranslationException) {
            cancellationRequests -= chapterKey
            currentState.value = MangaTranslationState.Failed(
                message = error.userMessage(),
                canRetry = error.isRetryable(),
            )
        }
    }

    private suspend fun submit(chapterKey: String, pages: List<ReaderPage>) {
        ensureNotCancelled(chapterKey)
        state(chapterKey).value = MangaTranslationState.Preparing
        val upload = archive.createUpload(chapterKey, pages)
        try {
            ensureNotCancelled(chapterKey)
            state(chapterKey).value = MangaTranslationState.Uploading
            val job = service.submitChapter(chapterKey, upload)
            jobStore.put(chapterKey, job.jobId)
            cancelSubmittedJobIfRequested(chapterKey, job.jobId)
            poll(chapterKey, job.jobId)
        } finally {
            upload.delete()
        }
    }

    private suspend fun poll(chapterKey: String, jobId: String) {
        while (true) {
            ensureNotCancelled(chapterKey)
            val job = getJobWithRetry(chapterKey, jobId) ?: return
            ensureNotCancelled(chapterKey)
            if (job.jobId != jobId || job.chapterKey != chapterKey) {
                throw MangaTranslationProtocolException("Le job reçu ne correspond pas au chapitre")
            }
            when (job.status) {
                TranslationJobStatus.QUEUED -> {
                    state(chapterKey).value = MangaTranslationState.Queued(
                        completedPages = job.completedPages,
                        totalPages = job.totalPages,
                    )
                    delay(pollInterval)
                }
                TranslationJobStatus.RUNNING -> {
                    state(chapterKey).value = MangaTranslationState.Translating(
                        completedPages = job.completedPages,
                        totalPages = job.totalPages,
                    )
                    delay(pollInterval)
                }
                TranslationJobStatus.COMPLETE -> {
                    installCompletedResult(chapterKey, jobId, job.totalPages)
                    return
                }
                TranslationJobStatus.FAILED -> {
                    jobStore.remove(chapterKey)
                    state(chapterKey).value = MangaTranslationState.Failed(
                        message = job.error?.take(MAX_ERROR_LENGTH) ?: "La traduction a échoué",
                        canRetry = false,
                    )
                    return
                }
                TranslationJobStatus.CANCELLED -> {
                    jobStore.remove(chapterKey)
                    state(chapterKey).value = MangaTranslationState.Cancelled
                    return
                }
            }
        }
    }

    private suspend fun getJobWithRetry(chapterKey: String, jobId: String): TranslationJob? {
        var failures = 0
        while (true) {
            try {
                return service.getJob(jobId)
            } catch (error: MangaTranslationException) {
                if (!error.isRetryable() || failures >= retryDelays.size) {
                    if (!error.isRetryable()) jobStore.remove(chapterKey)
                    state(chapterKey).value = MangaTranslationState.Failed(
                        message = error.userMessage(),
                        canRetry = error.isRetryable(),
                    )
                    return null
                }
                val retryDelay = retryDelays[failures]
                failures++
                state(chapterKey).value = MangaTranslationState.WaitingForNetwork(
                    attempt = failures,
                    maxAttempts = retryDelays.size,
                )
                delay(retryDelay)
                ensureNotCancelled(chapterKey)
            }
        }
    }

    private suspend fun installCompletedResult(chapterKey: String, jobId: String, expectedPages: Int) {
        state(chapterKey).value = MangaTranslationState.Downloading
        resultDirectory.mkdirs()
        val result = File(resultDirectory, "${chapterKey.sha256()}-${jobId.sha256()}.zip")
        try {
            val metadata = service.downloadResult(jobId, result)
            ensureNotCancelled(chapterKey)
            if (metadata.chapterKey != chapterKey || metadata.totalPages != expectedPages) {
                throw MangaTranslationProtocolException("Le résultat ne correspond pas au chapitre")
            }
            val installed = cache.installResult(metadata, result)
            jobStore.remove(chapterKey)
            state(chapterKey).value = MangaTranslationState.Ready(installed)
        } finally {
            result.delete()
        }
    }

    private suspend fun runOnce(chapterKey: String, block: suspend () -> Unit) {
        val started = activeMutex.withLock { activeChapters.add(chapterKey) }
        if (!started) return
        try {
            block()
        } catch (_: ExplicitTranslationCancellation) {
            state(chapterKey).value = MangaTranslationState.Cancelled
        } catch (error: CancellationException) {
            throw error
        } catch (error: MangaTranslationException) {
            val retryable = error.isRetryable()
            if (!retryable) jobStore.remove(chapterKey)
            state(chapterKey).value = MangaTranslationState.Failed(
                message = error.userMessage(),
                canRetry = retryable,
            )
        } catch (_: Exception) {
            jobStore.remove(chapterKey)
            state(chapterKey).value = MangaTranslationState.Failed(
                message = "Impossible de traduire ce chapitre",
                canRetry = false,
            )
        } finally {
            cancellationRequests -= chapterKey
            activeMutex.withLock { activeChapters.remove(chapterKey) }
        }
    }

    private fun ensureNotCancelled(chapterKey: String) {
        if (chapterKey in cancellationRequests) throw ExplicitTranslationCancellation()
    }

    private suspend fun cancelSubmittedJobIfRequested(chapterKey: String, jobId: String) {
        if (chapterKey !in cancellationRequests) return
        service.cancelJob(jobId)
        jobStore.remove(chapterKey)
        throw ExplicitTranslationCancellation()
    }

    private suspend fun clearCancellationIfInactive(chapterKey: String) {
        val isActive = activeMutex.withLock { chapterKey in activeChapters }
        if (!isActive) cancellationRequests -= chapterKey
    }

    private fun state(chapterKey: String) = states.computeIfAbsent(chapterKey) {
        MutableStateFlow(MangaTranslationState.Idle)
    }
}

private fun MangaTranslationException.isRetryable(): Boolean =
    this is MangaTranslationNetworkException ||
        (this is MangaTranslationHttpException && statusCode >= 500)

private fun MangaTranslationException.userMessage(): String =
    message?.take(MAX_ERROR_LENGTH) ?: "Erreur MangaTranslator"

private class ExplicitTranslationCancellation : Exception()

private const val MAX_ERROR_LENGTH = 240
