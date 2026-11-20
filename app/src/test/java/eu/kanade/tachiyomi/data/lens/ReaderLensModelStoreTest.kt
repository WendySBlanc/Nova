package eu.kanade.tachiyomi.data.lens

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ReaderLensModelStoreTest {

    @Test
    fun `combined model state exposes the corrector download`() {
        val index = ReaderLensModelState.Ready(File("index.gguf"))
        val corrector = ReaderLensModelState.Downloading(downloadedBytes = 42, totalBytes = 100)

        activeReaderLensModelState(index, corrector) shouldBe corrector
    }

    @Test
    fun `combined model state exposes either model failure`() {
        val failure = ReaderLensModelState.Failed("corrector unavailable", attempt = 1)

        activeReaderLensModelState(ReaderLensModelState.NotInstalled, failure) shouldBe failure
    }

    @TempDir
    lateinit var temporaryDirectory: Path

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fresh model is downloaded verified and promoted`() = runTest {
        val content = "index-model".encodeToByteArray()
        val store = modelStore(content)
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(content)))

        val model = store.ensureModel()

        model.readBytes() shouldBe content
        store.state.value shouldBe ReaderLensModelState.Ready(model)
        temporaryDirectory.resolve("test.gguf.verified").toFile().readText() shouldBe content.sha256()
        server.takeRequest().getHeader("Range") shouldBe null
    }

    @Test
    fun `partial model resumes with a validated byte range`() = runTest {
        val content = "complete-model".encodeToByteArray()
        val partial = content.copyOfRange(0, 5)
        temporaryDirectory.resolve("test.gguf.part").toFile().writeBytes(partial)
        val store = modelStore(content)
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes 5-${content.lastIndex}/${content.size}")
                .setBody(Buffer().write(content.copyOfRange(5, content.size))),
        )

        store.ensureModel().readBytes() shouldBe content
        server.takeRequest().getHeader("Range") shouldBe "bytes=5-"
    }

    @Test
    fun `range refusal restarts the partial file instead of appending`() = runTest {
        val content = "replacement-model".encodeToByteArray()
        temporaryDirectory.resolve("test.gguf.part").toFile().writeText("stale")
        val store = modelStore(content)
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(content)))

        store.ensureModel().readBytes() shouldBe content
    }

    @Test
    fun `complete partial model is verified without an invalid range request`() = runTest {
        val content = "already-complete-model".encodeToByteArray()
        temporaryDirectory.resolve("test.gguf.part").toFile().writeBytes(content)
        val store = modelStore(content)
        server.enqueue(MockResponse().setResponseCode(416))

        store.ensureModel().readBytes() shouldBe content

        server.requestCount shouldBe 0
    }

    @Test
    fun `checksum failure never promotes the model`() = runTest {
        val content = "corrupted-model".encodeToByteArray()
        val store = modelStore(content, expectedSha256 = "0".repeat(64))
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(content)))

        shouldThrow<ReaderLensModelException> { store.ensureModel() }

        temporaryDirectory.resolve("test.gguf").toFile().exists() shouldBe false
        temporaryDirectory.resolve("test.gguf.part").toFile().exists() shouldBe false
        (store.state.value is ReaderLensModelState.Failed) shouldBe true
    }

    @Test
    fun `insufficient free space fails before opening the network`() = runTest {
        val content = "large-model".encodeToByteArray()
        val store = modelStore(content, usableSpace = { 0L })

        shouldThrow<ReaderLensModelException> { store.ensureModel() }

        server.requestCount shouldBe 0
    }

    @Test
    fun `retrying the same preparation failure emits a fresh failed state`() = runTest {
        val content = "large-model".encodeToByteArray()
        val store = modelStore(content, usableSpace = { 0L })

        shouldThrow<ReaderLensModelException> { store.ensureModel() }
        val firstFailure = store.state.value as ReaderLensModelState.Failed
        shouldThrow<ReaderLensModelException> { store.ensureModel() }
        val secondFailure = store.state.value as ReaderLensModelState.Failed

        listOf(firstFailure.attempt, secondFailure.attempt) shouldBe listOf(1L, 2L)
        (firstFailure == secondFailure) shouldBe false
    }

    @Test
    fun `cancellation keeps resumable partial bytes and does not promote`(): Unit = runBlocking {
        val content = ByteArray(256 * 1024) { (it % 251).toByte() }
        val store = modelStore(content)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(content))
                .throttleBody(1024, 20, TimeUnit.MILLISECONDS),
        )

        val job = async { store.ensureModel() }
        while (temporaryDirectory.resolve("test.gguf.part").toFile().length() == 0L) {
            delay(10)
        }
        store.cancelDownload()

        shouldThrow<CancellationException> { job.await() }
        val partialLength = temporaryDirectory.resolve("test.gguf.part").toFile().length()
        (partialLength > 0 && partialLength < content.size) shouldBe true
        temporaryDirectory.resolve("test.gguf").toFile().exists() shouldBe false
    }

    private fun modelStore(
        content: ByteArray,
        expectedSha256: String = content.sha256(),
        usableSpace: () -> Long = { Long.MAX_VALUE },
    ) = ReaderLensModelStore(
        rootDirectory = temporaryDirectory.toFile(),
        client = OkHttpClient(),
        spec = ReaderLensModelSpec(
            id = "test-model",
            fileName = "test.gguf",
            url = server.url("/test.gguf").toString(),
            sizeBytes = content.size.toLong(),
            sha256 = expectedSha256,
        ),
        usableSpace = usableSpace,
        safetyReserveBytes = 0,
    )
}

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { byte -> "%02x".format(byte) }
