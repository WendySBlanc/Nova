package eu.kanade.tachiyomi.data.lens

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ReaderLensInferenceRuntimeTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `runtime support is restricted to arm64 devices`() {
        isReaderLensRuntimeSupported(arrayOf("arm64-v8a", "armeabi-v7a")) shouldBe true
        isReaderLensRuntimeSupported(arrayOf("x86_64", "x86")) shouldBe false
        isReaderLensRuntimeSupported(emptyArray()) shouldBe false
    }

    @Test
    fun `runtime lazily loads one deterministic cpu model and releases it`() = runTest {
        val backend = FakeLlamaBackend()
        val runtime = LlamaReaderLensInferenceRuntime(
            backend = backend,
            supportedAbis = arrayOf("arm64-v8a"),
            availableProcessors = 12,
        )
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }

        runtime.complete(model, "first") shouldBe "translated:first"
        runtime.complete(model, "second") shouldBe "translated:second"
        runtime.close()

        backend.loadCalls shouldBe 1
        backend.releaseCalls shouldBe 1
        backend.lastConfig shouldBe ReaderLensLlamaConfig(
            contextSize = 2048,
            threads = 6,
            gpuLayers = 0,
            temperature = 0f,
            topP = 1f,
            topK = 1,
            seed = 0,
        )
        backend.maxTokens shouldBe 768
    }

    @Test
    fun `runtime serializes concurrent inference`() = runTest {
        val backend = FakeLlamaBackend(completionDelayMs = 100)
        val runtime = LlamaReaderLensInferenceRuntime(
            backend = backend,
            supportedAbis = arrayOf("arm64-v8a"),
            availableProcessors = 8,
        )
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }

        val first = async { runtime.complete(model, "one") }
        val second = async { runtime.complete(model, "two") }
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
            availableProcessors = 8,
        )
        val model = temporaryDirectory.resolve("model.gguf").toFile().apply { writeText("model") }

        shouldThrow<ReaderLensRuntimeException> {
            runtime.complete(model, "translate")
        }

        backend.loadCalls shouldBe 0
    }
}

private class FakeLlamaBackend(
    private val completionDelayMs: Long = 0,
) : ReaderLensLlamaBackend {
    var loadCalls = 0
    var releaseCalls = 0
    var lastConfig: ReaderLensLlamaConfig? = null
    var maxTokens: Int? = null
    var maxConcurrentCompletions = 0
    private var activeCompletions = 0

    override suspend fun load(modelPath: String, config: ReaderLensLlamaConfig): Any {
        loadCalls++
        lastConfig = config
        return modelPath
    }

    override suspend fun complete(handle: Any, prompt: String, maxTokens: Int): String {
        this.maxTokens = maxTokens
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
    }
}
