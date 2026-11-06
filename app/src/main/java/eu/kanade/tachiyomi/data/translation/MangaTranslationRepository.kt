package eu.kanade.tachiyomi.data.translation

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MangaTranslationService {
    suspend fun checkHealth(): MangaTranslationServerInfo
    suspend fun submitChapter(
        chapterKey: String,
        archive: File,
        force: Boolean = false,
        replacementPageIndex: Int? = null,
    ): TranslationJob
    suspend fun getJob(jobId: String): TranslationJob
    suspend fun downloadResult(jobId: String, destination: File): TranslationResultMetadata
    suspend fun cancelJob(jobId: String)
}

interface MangaTranslationArchiveGateway {
    suspend fun createUpload(chapterKey: String, pages: List<ReaderPage>): File
}

interface MangaTranslationCacheGateway {
    fun installResult(metadata: TranslationResultMetadata, archive: File): InstalledTranslation
    fun replacePage(
        chapterKey: String,
        pageIndex: Int,
        resultMetadata: TranslationResultMetadata,
        archive: File,
    ): InstalledTranslation
    fun installedTranslation(chapterKey: String): InstalledTranslation?
    fun translatedPage(chapterKey: String, pageIndex: Int): File?
    fun activeMetadata(chapterKey: String): TranslationResultMetadata?
}

@Serializable
data class PendingMangaTranslationJob(
    val jobId: String,
    val replacementPageIndex: Int? = null,
)

interface MangaTranslationJobStore {
    fun get(chapterKey: String): PendingMangaTranslationJob?
    fun put(chapterKey: String, job: PendingMangaTranslationJob)
    fun remove(chapterKey: String)
}

class PreferenceMangaTranslationJobStore(
    preferences: MangaTranslationPreferences,
    private val json: Json,
) : MangaTranslationJobStore {
    private val preference = preferences.pendingJobs()

    @Synchronized
    override fun get(chapterKey: String): PendingMangaTranslationJob? = read()[chapterKey]

    @Synchronized
    override fun put(chapterKey: String, job: PendingMangaTranslationJob) {
        write(read() + (chapterKey to job))
    }

    @Synchronized
    override fun remove(chapterKey: String) {
        write(read() - chapterKey)
    }

    private fun read(): Map<String, PendingMangaTranslationJob> {
        val stored = preference.get()
        return runCatching {
            json.decodeFromString<Map<String, PendingMangaTranslationJob>>(stored)
        }.recoverCatching {
            json.decodeFromString<Map<String, String>>(stored)
                .mapValues { PendingMangaTranslationJob(jobId = it.value) }
        }.getOrDefault(emptyMap())
    }

    private fun write(jobs: Map<String, PendingMangaTranslationJob>) {
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

    fun hasTranslation(chapterKey: String, pageIndexes: List<Int>): Boolean =
        pageIndexes.isNotEmpty() && pageIndexes.all { cache.translatedPage(chapterKey, it) != null }

    fun translatedPage(chapterKey: String, pageIndex: Int): File? =
        cache.translatedPage(chapterKey, pageIndex)

    fun installedTranslation(chapterKey: String): InstalledTranslation? =
        cache.installedTranslation(chapterKey)

    suspend fun hasUsableTranslation(chapterKey: String, pageIndexes: List<Int>): Boolean {
        if (!hasTranslation(chapterKey, pageIndexes)) return false
        val metadata = cache.activeMetadata(chapterKey) ?: return false
        if (metadata.totalPages != pageIndexes.size) return false
        return true
    }

    suspend fun translate(chapterKey: String, pages: List<ReaderPage>, force: Boolean = false) {
        runOnce(chapterKey) {
            val serverInfo = service.checkHealth()
            val existingJob = jobStore.get(chapterKey)
            if (force) {
                if (existingJob != null) {
                    val serverJob = service.getJob(existingJob.jobId)
                    if (serverJob.status == TranslationJobStatus.QUEUED || serverJob.status == TranslationJobStatus.RUNNING) {
                        service.cancelJob(existingJob.jobId)
                    }
                    jobStore.remove(chapterKey)
                }
                submit(chapterKey, pages, serverInfo.configRevision, force = true)
            } else if (existingJob != null) {
                val serverJob = service.getJob(existingJob.jobId)
                if (serverJob.configRevision == serverInfo.configRevision) {
                    poll(
                        chapterKey = chapterKey,
                        jobId = existingJob.jobId,
                        expectedConfigRevision = serverInfo.configRevision,
                        initialJob = serverJob,
                        replacementPageIndex = existingJob.replacementPageIndex,
                    )
                } else {
                    if (serverJob.status == TranslationJobStatus.QUEUED || serverJob.status == TranslationJobStatus.RUNNING) {
                        service.cancelJob(existingJob.jobId)
                    }
                    jobStore.remove(chapterKey)
                    submit(chapterKey, pages, serverInfo.configRevision)
                }
            } else {
                submit(chapterKey, pages, serverInfo.configRevision)
            }
        }
    }

    suspend fun retranslatePage(chapterKey: String, pageIndex: Int, page: ReaderPage) {
        require(pageIndex >= 0) { "pageIndex cannot be negative" }
        runOnce(chapterKey) {
            val serverInfo = service.checkHealth()
            jobStore.get(chapterKey)?.let { existingJob ->
                val serverJob = service.getJob(existingJob.jobId)
                if (serverJob.status == TranslationJobStatus.QUEUED || serverJob.status == TranslationJobStatus.RUNNING) {
                    service.cancelJob(existingJob.jobId)
                }
                jobStore.remove(chapterKey)
            }
            val sourceStream = page.stream
                ?: throw MangaTranslationProtocolException("La page ${pageIndex + 1} n'est pas disponible")
            val uploadPage = ReaderPage(index = 0, stream = sourceStream)
            submit(
                chapterKey = chapterKey,
                pages = listOf(uploadPage),
                configRevision = serverInfo.configRevision,
                force = true,
                replacementPageIndex = pageIndex,
            )
        }
    }

    suspend fun resume(chapterKey: String) {
        val pendingJob = jobStore.get(chapterKey) ?: return
        runOnce(chapterKey) {
            val serverInfo = service.checkHealth()
            val job = service.getJob(pendingJob.jobId)
            if (job.configRevision != serverInfo.configRevision) {
                if (job.status == TranslationJobStatus.QUEUED || job.status == TranslationJobStatus.RUNNING) {
                    service.cancelJob(pendingJob.jobId)
                }
                jobStore.remove(chapterKey)
                state(chapterKey).value = MangaTranslationState.Idle
                return@runOnce
            }
            val replacementPageIndex = pendingJob.replacementPageIndex
                ?: pageTranslationIndex(chapterKey, job.chapterKey)
            poll(
                chapterKey = chapterKey,
                jobId = pendingJob.jobId,
                expectedConfigRevision = serverInfo.configRevision,
                initialJob = job,
                serverChapterKey = job.chapterKey,
                replacementPageIndex = replacementPageIndex,
            )
        }
    }

    suspend fun cancel(chapterKey: String) {
        cancellationRequests += chapterKey
        val currentState = state(chapterKey)
        currentState.value = MangaTranslationState.Cancelling
        val pendingJob = jobStore.get(chapterKey)
        if (pendingJob == null) {
            currentState.value = MangaTranslationState.Cancelled
            clearCancellationIfInactive(chapterKey)
            return
        }
        try {
            service.cancelJob(pendingJob.jobId)
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

    private suspend fun submit(
        chapterKey: String,
        pages: List<ReaderPage>,
        configRevision: String,
        force: Boolean = false,
        serverChapterKey: String = chapterKey,
        replacementPageIndex: Int? = null,
    ) {
        ensureNotCancelled(chapterKey)
        state(chapterKey).value = MangaTranslationState.Preparing
        val upload = archive.createUpload(serverChapterKey, pages)
        try {
            ensureNotCancelled(chapterKey)
            state(chapterKey).value = MangaTranslationState.Uploading
            val job = service.submitChapter(
                chapterKey = serverChapterKey,
                archive = upload,
                force = force,
                replacementPageIndex = replacementPageIndex,
            )
            if (job.configRevision != configRevision) {
                throw MangaTranslationProtocolException("La configuration du serveur a changé pendant l'envoi")
            }
            jobStore.put(
                chapterKey,
                PendingMangaTranslationJob(
                    jobId = job.jobId,
                    replacementPageIndex = replacementPageIndex,
                ),
            )
            cancelSubmittedJobIfRequested(chapterKey, job.jobId)
            poll(
                chapterKey = chapterKey,
                jobId = job.jobId,
                expectedConfigRevision = configRevision,
                initialJob = job,
                serverChapterKey = serverChapterKey,
                replacementPageIndex = replacementPageIndex,
            )
        } finally {
            upload.delete()
        }
    }

    private suspend fun poll(
        chapterKey: String,
        jobId: String,
        expectedConfigRevision: String? = null,
        initialJob: TranslationJob? = null,
        serverChapterKey: String = chapterKey,
        replacementPageIndex: Int? = null,
    ) {
        var nextJob = initialJob
        while (true) {
            ensureNotCancelled(chapterKey)
            val job = nextJob ?: getJobWithRetry(chapterKey, jobId) ?: return
            nextJob = null
            ensureNotCancelled(chapterKey)
            if (job.jobId != jobId || job.chapterKey != serverChapterKey) {
                throw MangaTranslationProtocolException("Le job reçu ne correspond pas au chapitre")
            }
            if (expectedConfigRevision != null && job.configRevision != expectedConfigRevision) {
                throw MangaTranslationProtocolException("Le job utilise une autre configuration")
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
                    if (replacementPageIndex == null) {
                        installCompletedResult(chapterKey, jobId, job.totalPages)
                    } else {
                        installCompletedPageResult(
                            chapterKey = chapterKey,
                            serverChapterKey = serverChapterKey,
                            pageIndex = replacementPageIndex,
                            jobId = jobId,
                            expectedPages = job.totalPages,
                        )
                    }
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

    private suspend fun installCompletedPageResult(
        chapterKey: String,
        serverChapterKey: String,
        pageIndex: Int,
        jobId: String,
        expectedPages: Int,
    ) {
        state(chapterKey).value = MangaTranslationState.Downloading
        resultDirectory.mkdirs()
        val result = File(resultDirectory, "${serverChapterKey.sha256()}-${jobId.sha256()}.zip")
        try {
            val metadata = service.downloadResult(jobId, result)
            ensureNotCancelled(chapterKey)
            if (metadata.chapterKey != serverChapterKey || metadata.totalPages != 1 || expectedPages != 1) {
                throw MangaTranslationProtocolException("Le résultat ne correspond pas à la page")
            }
            val installed = cache.replacePage(chapterKey, pageIndex, metadata, result)
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

private fun pageTranslationIndex(chapterKey: String, serverChapterKey: String): Int? =
    serverChapterKey.removePrefix("$chapterKey:page:")
        .takeIf { serverChapterKey.startsWith("$chapterKey:page:") }
        ?.toIntOrNull()
        ?.takeIf { it >= 0 }

private fun MangaTranslationException.isRetryable(): Boolean =
    this is MangaTranslationNetworkException ||
        (this is MangaTranslationHttpException && statusCode >= 500)

private fun MangaTranslationException.userMessage(): String =
    message?.take(MAX_ERROR_LENGTH) ?: "Erreur MangaTranslator"

private class ExplicitTranslationCancellation : Exception()

private const val MAX_ERROR_LENGTH = 240
