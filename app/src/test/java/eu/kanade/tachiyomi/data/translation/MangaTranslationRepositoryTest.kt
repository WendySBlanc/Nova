package eu.kanade.tachiyomi.data.translation

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.ZERO

@OptIn(ExperimentalCoroutinesApi::class)
class MangaTranslationRepositoryTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `translate submits once and publishes server progress`() = runTest {
        val service = FakeService(
            jobs = ArrayDeque(
                listOf(
                    job(TranslationJobStatus.QUEUED, completed = 0),
                    job(TranslationJobStatus.RUNNING, completed = 1),
                    job(TranslationJobStatus.COMPLETE, completed = 2),
                ),
            ),
        )
        val states = mutableListOf<MangaTranslationState>()
        val repository = repository(service = service)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe(CHAPTER_KEY).toList(states)
        }

        repository.translate(CHAPTER_KEY, pages())

        service.submitCount shouldBe 1
        states.map { it::class }.shouldContain(MangaTranslationState.Queued::class)
        states.map { it::class }.shouldContain(MangaTranslationState.Translating::class)
        repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Ready>()
    }

    @Test
    fun `concurrent translate calls are deduplicated`() = runTest {
        val releaseJob = CompletableDeferred<Unit>()
        val service = FakeService(
            getJobHandler = {
                releaseJob.await()
                job(TranslationJobStatus.COMPLETE, completed = 2)
            },
        )
        val repository = repository(service = service)

        val first = async { repository.translate(CHAPTER_KEY, pages()) }
        runCurrent()
        val second = async { repository.translate(CHAPTER_KEY, pages()) }
        runCurrent()

        service.submitCount shouldBe 1
        releaseJob.complete(Unit)
        first.await()
        second.await()
        repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Ready>()
    }

    @Test
    fun `temporary network failures are retried`() = runTest {
        var calls = 0
        val service = FakeService(
            getJobHandler = {
                calls++
                if (calls < 3) throw MangaTranslationNetworkException(IllegalStateException("offline"))
                job(TranslationJobStatus.COMPLETE, completed = 2)
            },
        )
        val repository = repository(service = service)

        repository.translate(CHAPTER_KEY, pages())

        calls shouldBe 3
        repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Ready>()
    }

    @Test
    fun `exhausted network retries preserve the job for resume`() = runTest {
        val store = FakeJobStore()
        val service = FakeService(
            getJobHandler = {
                throw MangaTranslationNetworkException(IllegalStateException("offline"))
            },
        )
        val repository = repository(service = service, jobStore = store)

        repository.translate(CHAPTER_KEY, pages())

        store.get(CHAPTER_KEY) shouldBe JOB_ID
        val failure = repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Failed>()
        failure.canRetry shouldBe true
    }

    @Test
    fun `resume after process recreation polls the persisted job without uploading`() = runTest {
        val store = FakeJobStore(mutableMapOf(CHAPTER_KEY to JOB_ID))
        val service = FakeService(jobs = ArrayDeque(listOf(job(TranslationJobStatus.COMPLETE, completed = 2))))
        val repository = repository(service = service, jobStore = store)

        repository.resume(CHAPTER_KEY)

        service.submitCount shouldBe 0
        store.get(CHAPTER_KEY) shouldBe null
        repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Ready>()
    }

    @Test
    fun `resume discards a pending job from an older server configuration`() = runTest {
        val store = FakeJobStore(mutableMapOf(CHAPTER_KEY to JOB_ID))
        val service = FakeService(
            jobs = ArrayDeque(listOf(job(TranslationJobStatus.RUNNING, completed = 1))),
            serverInfo = serverInfo("config-v2"),
        )
        val repository = repository(service = service, jobStore = store)

        repository.resume(CHAPTER_KEY)

        service.cancelledJobIds shouldBe listOf(JOB_ID)
        store.get(CHAPTER_KEY) shouldBe null
        repository.observe(CHAPTER_KEY).value shouldBe MangaTranslationState.Idle
    }

    @Test
    fun `server failure becomes terminal and clears the persisted job`() = runTest {
        val store = FakeJobStore()
        val service = FakeService(
            jobs = ArrayDeque(listOf(job(TranslationJobStatus.FAILED, error = "OCR failed"))),
        )
        val repository = repository(service = service, jobStore = store)

        repository.translate(CHAPTER_KEY, pages())

        store.get(CHAPTER_KEY) shouldBe null
        val failure = repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Failed>()
        failure.canRetry shouldBe false
        failure.message shouldBe "OCR failed"
    }

    @Test
    fun `explicit cancellation cancels the persisted server job`() = runTest {
        val store = FakeJobStore(mutableMapOf(CHAPTER_KEY to JOB_ID))
        val service = FakeService()
        val repository = repository(service = service, jobStore = store)

        repository.cancel(CHAPTER_KEY)

        service.cancelledJobIds shouldBe listOf(JOB_ID)
        store.get(CHAPTER_KEY) shouldBe null
        repository.observe(CHAPTER_KEY).value shouldBe MangaTranslationState.Cancelled
    }

    @Test
    fun `cancelling without an active job does not cancel the next translation`() = runTest {
        val service = FakeService()
        val repository = repository(service = service)

        repository.cancel(CHAPTER_KEY)
        repository.translate(CHAPTER_KEY, pages())

        service.submitCount shouldBe 1
        repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Ready>()
    }

    @Test
    fun `invalid downloaded result never becomes ready`() = runTest {
        val store = FakeJobStore()
        val repository = repository(
            service = FakeService(
                jobs = ArrayDeque(listOf(job(TranslationJobStatus.COMPLETE, completed = 2))),
            ),
            jobStore = store,
            cache = FakeCache(installError = MangaTranslationProtocolException("bad result")),
        )

        repository.translate(CHAPTER_KEY, pages())

        store.get(CHAPTER_KEY) shouldBe null
        val failure = repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Failed>()
        failure.message shouldBe "bad result"
        failure.canRetry shouldBe false
    }

    @Test
    fun `result for another chapter is rejected`() = runTest {
        val repository = repository(
            service = FakeService(
                jobs = ArrayDeque(listOf(job(TranslationJobStatus.COMPLETE, completed = 2))),
                resultMetadata = metadata().copy(chapterKey = "another:chapter"),
            ),
        )

        repository.translate(CHAPTER_KEY, pages())

        repository.observe(CHAPTER_KEY).value.shouldBeInstanceOf<MangaTranslationState.Failed>()
    }

    @Test
    fun `online configuration change makes an older cached translation stale`() = runTest {
        val cache = FakeCache(
            activeMetadata = metadata().copy(configRevision = "config-v1"),
            translatedPages = setOf(0, 1),
        )
        val service = FakeService(
            serverInfo = serverInfo("config-v2"),
        )
        val repository = repository(service = service, cache = cache)

        repository.hasUsableTranslation(CHAPTER_KEY, listOf(0, 1)) shouldBe false
    }

    @Test
    fun `offline mode keeps a complete cached translation usable`() = runTest {
        val cache = FakeCache(
            activeMetadata = metadata(),
            translatedPages = setOf(0, 1),
        )
        val service = FakeService(
            healthError = MangaTranslationNetworkException(IllegalStateException("offline")),
        )
        val repository = repository(service = service, cache = cache)

        repository.hasUsableTranslation(CHAPTER_KEY, listOf(0, 1)) shouldBe true
    }

    private fun repository(
        service: MangaTranslationService = FakeService(),
        jobStore: MangaTranslationJobStore = FakeJobStore(),
        cache: MangaTranslationCacheGateway = FakeCache(),
    ) = MangaTranslationRepository(
        service = service,
        archive = FakeArchive(File(temporaryDirectory.toFile(), "upload.zip")),
        cache = cache,
        jobStore = jobStore,
        resultDirectory = File(temporaryDirectory.toFile(), "results"),
        pollInterval = ZERO,
        retryDelays = listOf(ZERO, ZERO),
    )

    private fun pages() = listOf(ReaderPage(0), ReaderPage(1))

    private class FakeService(
        private val jobs: ArrayDeque<TranslationJob> = ArrayDeque(),
        private val getJobHandler: (suspend () -> TranslationJob)? = null,
        private val resultMetadata: TranslationResultMetadata = metadata(),
        private val serverInfo: MangaTranslationServerInfo = serverInfo("config-v1"),
        private val healthError: MangaTranslationException? = null,
    ) : MangaTranslationService {
        var submitCount = 0
        val cancelledJobIds = mutableListOf<String>()

        override suspend fun checkHealth(): MangaTranslationServerInfo {
            healthError?.let { throw it }
            return serverInfo
        }

        override suspend fun submitChapter(chapterKey: String, archive: File): TranslationJob {
            submitCount++
            return job(TranslationJobStatus.QUEUED, completed = 0)
        }

        override suspend fun getJob(jobId: String): TranslationJob =
            getJobHandler?.invoke() ?: jobs.removeFirstOrNull() ?: job(TranslationJobStatus.COMPLETE, completed = 2)

        override suspend fun downloadResult(jobId: String, destination: File): TranslationResultMetadata {
            destination.parentFile?.mkdirs()
            destination.writeText("result")
            return resultMetadata
        }

        override suspend fun cancelJob(jobId: String) {
            cancelledJobIds += jobId
        }
    }

    private class FakeArchive(private val upload: File) : MangaTranslationArchiveGateway {
        override suspend fun createUpload(chapterKey: String, pages: List<ReaderPage>): File =
            upload.apply {
                parentFile?.mkdirs()
                writeText("upload")
            }
    }

    private class FakeCache(
        private val installError: MangaTranslationException? = null,
        private val activeMetadata: TranslationResultMetadata? = null,
        private val translatedPages: Set<Int> = emptySet(),
    ) : MangaTranslationCacheGateway {
        override fun installResult(metadata: TranslationResultMetadata, archive: File): InstalledTranslation {
            installError?.let { throw it }
            val directory = File(archive.parentFile, "installed").apply { mkdirs() }
            val pages = (0 until metadata.totalPages).map { index ->
                File(directory, "%06d.png".format(index)).apply { writeText("page") }
            }
            return InstalledTranslation(metadata, directory, pages)
        }

        override fun translatedPage(chapterKey: String, pageIndex: Int): File? = pageIndex
            .takeIf(translatedPages::contains)
            ?.let { File("cached-$it.png") }

        override fun activeMetadata(chapterKey: String): TranslationResultMetadata? = activeMetadata
    }

    private class FakeJobStore(
        private val jobs: MutableMap<String, String> = mutableMapOf(),
    ) : MangaTranslationJobStore {
        override fun get(chapterKey: String): String? = jobs[chapterKey]

        override fun put(chapterKey: String, jobId: String) {
            jobs[chapterKey] = jobId
        }

        override fun remove(chapterKey: String) {
            jobs.remove(chapterKey)
        }
    }

    private companion object {
        const val CHAPTER_KEY = "source:manga:chapter"
        const val JOB_ID = "job-1"

        fun job(
            status: TranslationJobStatus,
            completed: Int = 0,
            error: String? = null,
        ) = TranslationJob(
            jobId = JOB_ID,
            chapterKey = CHAPTER_KEY,
            status = status,
            completedPages = completed,
            totalPages = 2,
            error = error,
            targetLanguage = if (status == TranslationJobStatus.COMPLETE) "French" else null,
            configRevision = "config-v1",
        )

        fun metadata() = TranslationResultMetadata(
            chapterKey = CHAPTER_KEY,
            targetLanguage = "French",
            configRevision = "config-v1",
            totalPages = 2,
        )

        fun serverInfo(configRevision: String) = MangaTranslationServerInfo(
            apiVersion = 1,
            status = "ok",
            configRevision = configRevision,
        )
    }
}
