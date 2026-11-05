package eu.kanade.tachiyomi.data.translation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

class MangaTranslationClientTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `normalizes base url while preserving its path`() {
        normalizeMangaTranslationBaseUrl("  ${server.url("translator/root/")}///  ").toString() shouldBe
            server.url("translator/root/").toString()
    }

    @Test
    fun `submit sends bearer token and parses accepted job`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody(jobJson(status = "queued")),
        )
        val archive = File(temporaryDirectory.toFile(), "chapter.zip").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val client = client(token = "private-token")

        val job = client.submitChapter("source:manga:chapter", archive)
        val request = server.takeRequest()

        job.jobId shouldBe "job-1"
        job.status shouldBe TranslationJobStatus.QUEUED
        request.getHeader("Authorization") shouldBe "Bearer private-token"
        request.path shouldBe "/translator/v1/chapters"
        request.body.readUtf8() shouldContain "source:manga:chapter"
    }

    @Test
    fun `get job ignores unknown response fields`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                jobJson(status = "running").dropLast(1) + ",\"future_field\":true}",
            ),
        )

        val job = client().getJob("job-1")

        job.status shouldBe TranslationJobStatus.RUNNING
        job.completedPages shouldBe 1
        server.takeRequest().path shouldBe "/translator/v1/chapters/job-1"
    }

    @Test
    fun `download result writes atomically and returns metadata`() = runTest {
        server.enqueue(MockResponse().setBody(jobJson(status = "complete")))
        server.enqueue(MockResponse().setBody("translated-zip"))
        val destination = File(temporaryDirectory.toFile(), "result.zip")

        val metadata = client().downloadResult("job-1", destination)

        destination.readText() shouldBe "translated-zip"
        metadata.targetLanguage shouldBe "French"
        metadata.configRevision shouldBe "config-v1"
        metadata.totalPages shouldBe 2
        metadata.chapterKey shouldBe "chapter-key"
    }

    @Test
    fun `non json error reports status without leaking token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(502)
                .setBody("<html>proxy unavailable</html>"),
        )
        val token = "never-print-this-token"

        val error = runCatching { client(token = token).getJob("job-1") }.exceptionOrNull()!!

        error.message.orEmpty() shouldContain "HTTP 502"
        error.toString() shouldNotContain token
    }

    @Test
    fun `request timeout becomes a sanitized network error`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val token = "timeout-secret"

        val error = runCatching {
            client(token = token, timeoutMillis = 80).getJob("job-1")
        }.exceptionOrNull()!!

        error::class shouldBe MangaTranslationNetworkException::class
        error.toString() shouldNotContain token
    }

    private fun client(
        token: String = "token",
        timeoutMillis: Long = 2_000,
    ) = MangaTranslationClient(
        client = OkHttpClient(),
        json = Json { ignoreUnknownKeys = true },
        connection = {
            MangaTranslationConnection(
                baseUrl = server.url("translator/").toString(),
                accessToken = token,
            )
        },
        callTimeout = timeoutMillis.milliseconds,
    )

    private fun jobJson(status: String) =
        """{
            "job_id":"job-1",
            "chapter_key":"chapter-key",
            "status":"$status",
            "completed_pages":1,
            "total_pages":2,
            "error":null,
            "target_language":"French",
            "config_revision":"config-v1",
            "cached":false
        }""".trimIndent()
}
