package eu.kanade.tachiyomi.data.lens

import android.content.ComponentCallbacks2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReaderLensInferenceRuntimeTest {

    @Test
    fun `HY MT runtime does not inject a system prompt`() {
        READER_LENS_SYSTEM_PROMPT shouldBe ""
    }

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `runtime support requires arm64 Android 9 and enough memory`() {
        isReaderLensRuntimeSupported(arrayOf("arm64-v8a"), gibibytes(12), sdkInt = 35) shouldBe true
        isReaderLensRuntimeSupported(arrayOf("x86_64"), gibibytes(12), sdkInt = 35) shouldBe false
        isReaderLensRuntimeSupported(arrayOf("arm64-v8a"), gibibytes(6), sdkInt = 35) shouldBe false
        isReaderLensRuntimeSupported(arrayOf("arm64-v8a"), gibibytes(12), sdkInt = 27) shouldBe false
    }

    @Suppress("DEPRECATION")
    @Test
    fun `memory policy releases in background and on legacy critical pressure`() {
        shouldReleaseReaderLensModel(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN, sdkInt = 35) shouldBe true
        shouldReleaseReaderLensModel(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL, sdkInt = 35) shouldBe false
        shouldReleaseReaderLensModel(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL, sdkInt = 33) shouldBe true
    }

    @Test
    fun `runtime lazily loads one deterministic cpu model and releases it`() = runTest {
        val backend = FakeLlamaBackend()
        val runtime = LlamaReaderLensInferenceRuntime(
            backend = backend,
            supportedAbis = arrayOf("arm64-v8a"),
            totalMemoryBytes = gibibytes(12),
            sdkInt = 35,
            availableProcessors = 12,
            releaseScope = this,
        )
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }

        runtime.complete(model, "first", maxOutputTokens = 64) shouldBe "translated:first"
        runtime.complete(model, "second", maxOutputTokens = 128) shouldBe "translated:second"
        runtime.close()
        advanceUntilIdle()

        backend.loadCalls shouldBe 1
        backend.releaseCalls shouldBe 1
        backend.lastConfig shouldBe ReaderLensLlamaConfig(
            contextSize = 4096,
            threads = 6,
            gpuLayers = 0,
            temperature = 0.7f,
            topP = 0.6f,
            topK = 20,
            seed = 0,
        )
        backend.maxTokens shouldBe 128
        backend.systemPrompt shouldBe READER_LENS_SYSTEM_PROMPT
    }

    @Test
    fun `runtime serializes concurrent inference`() = runTest {
        val backend = FakeLlamaBackend(completionDelayMs = 100)
        val runtime = LlamaReaderLensInferenceRuntime(
            backend = backend,
            supportedAbis = arrayOf("arm64-v8a"),
            totalMemoryBytes = gibibytes(12),
            sdkInt = 35,
            availableProcessors = 8,
            releaseScope = backgroundScope,
        )
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }

        val first = async { runtime.complete(model, "one", maxOutputTokens = 64) }
        val second = async { runtime.complete(model, "two", maxOutputTokens = 64) }
        advanceUntilIdle()

        first.await() shouldBe "translated:one"
        second.await() shouldBe "translated:two"
        backend.maxConcurrentCompletions shouldBe 1
    }

    @Test
    fun `unsupported runtime fails before loading the native backend`() = runTest {
        val backend = FakeLlamaBackend()
        val runtime = LlamaReaderLensInferenceRuntime(
            backend = backend,
            supportedAbis = arrayOf("x86_64"),
            totalMemoryBytes = gibibytes(12),
            sdkInt = 35,
            availableProcessors = 8,
            releaseScope = backgroundScope,
        )
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }

        shouldThrow<ReaderLensRuntimeException> {
            runtime.complete(model, "translate", maxOutputTokens = 64)
        }

        backend.loadCalls shouldBe 0
    }

    @Test
    fun `cancellation during native load keeps the loaded handle reusable`() = runTest {
        val loadStarted = CompletableDeferred<Unit>()
        val allowLoadToFinish = CompletableDeferred<Unit>()
        val backend = FakeLlamaBackend(loadStarted = loadStarted, allowLoadToFinish = allowLoadToFinish)
        val runtime = supportedRuntime(backend)
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }

        val first = launch { runtime.complete(model, "cancelled", maxOutputTokens = 64) }
        loadStarted.await()
        first.cancel()
        allowLoadToFinish.complete(Unit)
        first.join()

        runtime.complete(model, "retry", maxOutputTokens = 64) shouldBe "translated:retry"
        backend.loadCalls shouldBe 1
    }

    @Test
    fun `critical memory release unloads the warm model before the next request`() = runTest {
        val backend = FakeLlamaBackend()
        val runtime = supportedRuntime(backend)
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }
        runtime.complete(model, "first", maxOutputTokens = 64)

        runtime.releaseWarmModel()
        advanceUntilIdle()
        runtime.complete(model, "second", maxOutputTokens = 64)

        backend.releaseCalls shouldBe 1
        backend.loadCalls shouldBe 2
    }

    @Test
    fun `close returns before native model release finishes`() = runTest {
        val releaseStarted = CountDownLatch(1)
        val allowReleaseToFinish = CountDownLatch(1)
        val releaseFinished = CountDownLatch(1)
        val backend = FakeLlamaBackend(
            releaseStarted = releaseStarted,
            allowReleaseToFinish = allowReleaseToFinish,
            releaseFinished = releaseFinished,
        )
        val runtime = LlamaReaderLensInferenceRuntime(
            backend = backend,
            supportedAbis = arrayOf("arm64-v8a"),
            totalMemoryBytes = gibibytes(12),
            sdkInt = 35,
            availableProcessors = 8,
            releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }
        runtime.complete(model, "first", maxOutputTokens = 64)

        val closeJob = async(Dispatchers.Default) { runtime.close() }
        releaseStarted.await(2, TimeUnit.SECONDS) shouldBe true
        withTimeout(2_000) { closeJob.await() }
        releaseFinished.count shouldBe 1L
        allowReleaseToFinish.countDown()

        withTimeout(2_000) {
            while (!releaseFinished.await(10, TimeUnit.MILLISECONDS)) delay(10)
        }
    }

    private fun CoroutineScope.supportedRuntime(backend: ReaderLensLlamaBackend) =
        LlamaReaderLensInferenceRuntime(
            backend = backend,
            supportedAbis = arrayOf("arm64-v8a"),
            totalMemoryBytes = gibibytes(12),
            sdkInt = 35,
            availableProcessors = 8,
            releaseScope = this,
        )
}

private class FakeLlamaBackend(
    private val completionDelayMs: Long = 0,
    private val loadStarted: CompletableDeferred<Unit>? = null,
    private val allowLoadToFinish: CompletableDeferred<Unit>? = null,
    private val releaseStarted: CountDownLatch? = null,
    private val allowReleaseToFinish: CountDownLatch? = null,
    private val releaseFinished: CountDownLatch? = null,
) : ReaderLensLlamaBackend {
    var loadCalls = 0
    var releaseCalls = 0
    var lastConfig: ReaderLensLlamaConfig? = null
    var maxTokens: Int? = null
    var systemPrompt: String? = null
    var maxConcurrentCompletions = 0
    private var activeCompletions = 0

    override suspend fun load(modelPath: String, config: ReaderLensLlamaConfig): Any {
        loadCalls++
        lastConfig = config
        loadStarted?.complete(Unit)
        allowLoadToFinish?.await()
        return modelPath
    }

    override suspend fun complete(handle: Any, prompt: String, systemPrompt: String, maxTokens: Int): String {
        this.maxTokens = maxTokens
        this.systemPrompt = systemPrompt
        activeCompletions++
        maxConcurrentCompletions = maxOf(maxConcurrentCompletions, activeCompletions)
        try {
            if (completionDelayMs > 0) delay(completionDelayMs)
            return "translated:$prompt"
        } finally {
            activeCompletions--
        }
    }

    override fun release(handle: Any) {
        releaseCalls++
        releaseStarted?.countDown()
        allowReleaseToFinish?.await(2, TimeUnit.SECONDS)
        releaseFinished?.countDown()
    }
}

private fun gibibytes(value: Long): Long = value * 1024L * 1024L * 1024L
