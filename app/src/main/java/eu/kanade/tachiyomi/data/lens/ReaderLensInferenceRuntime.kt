package eu.kanade.tachiyomi.data.lens

import android.os.Build
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

internal data class ReaderLensLlamaConfig(
    val contextSize: Int,
    val threads: Int,
    val gpuLayers: Int,
    val temperature: Float,
    val topP: Float,
    val topK: Int,
    val seed: Int,
)

internal interface ReaderLensLlamaBackend {
    suspend fun load(modelPath: String, config: ReaderLensLlamaConfig): Any
    suspend fun complete(handle: Any, prompt: String, maxTokens: Int): String
    fun release(handle: Any)
}

internal interface ReaderLensInferenceRuntime : AutoCloseable {
    suspend fun complete(modelFile: File, prompt: String): String
}

internal class ReaderLensRuntimeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal fun isReaderLensRuntimeSupported(abis: Array<String>): Boolean =
    abis.any { it.equals(READER_LENS_SUPPORTED_ABI, ignoreCase = true) }

internal class LlamaReaderLensInferenceRuntime(
    private val backend: ReaderLensLlamaBackend = AndroidReaderLensLlamaBackend(),
    private val supportedAbis: Array<String> = Build.SUPPORTED_ABIS,
    availableProcessors: Int = Runtime.getRuntime().availableProcessors(),
) : ReaderLensInferenceRuntime {

    private val inferenceMutex = Mutex()
    private val stateLock = Any()
    private val config = ReaderLensLlamaConfig(
        contextSize = READER_LENS_CONTEXT_SIZE,
        threads = availableProcessors.coerceIn(MIN_READER_LENS_THREADS, MAX_READER_LENS_THREADS),
        gpuLayers = 0,
        temperature = 0f,
        topP = 1f,
        topK = 1,
        seed = 0,
    )

    private var loadedModelPath: String? = null
    private var modelHandle: Any? = null
    private var inferenceActive = false
    private var closed = false

    override suspend fun complete(modelFile: File, prompt: String): String = inferenceMutex.withLock {
        ensureSupported()
        beginInference()
        try {
            val handle = loadModelIfNeeded(modelFile)
            backend.complete(handle, prompt, READER_LENS_MAX_OUTPUT_TOKENS)
        } catch (error: CancellationException) {
            throw error
        } catch (error: ReaderLensRuntimeException) {
            throw error
        } catch (error: Throwable) {
            throw ReaderLensRuntimeException("Le moteur local Lens n'a pas pu traduire cette page", error)
        } finally {
            endInference()
        }
    }

    override fun close() {
        val handleToRelease = synchronized(stateLock) {
            closed = true
            if (inferenceActive) {
                null
            } else {
                detachModel()
            }
        }
        handleToRelease?.let(backend::release)
    }

    private fun ensureSupported() {
        if (!isReaderLensRuntimeSupported(supportedAbis)) {
            throw ReaderLensRuntimeException("Lens local nécessite un appareil Android 64 bits ARM")
        }
    }

    private fun beginInference() = synchronized(stateLock) {
        if (closed) {
            throw ReaderLensRuntimeException("Le moteur local Lens est déjà fermé")
        }
        inferenceActive = true
    }

    private suspend fun loadModelIfNeeded(modelFile: File): Any {
        val canonicalPath = modelFile.canonicalPath
        synchronized(stateLock) {
            if (loadedModelPath == canonicalPath) {
                return requireNotNull(modelHandle)
            }
        }

        val previousHandle = synchronized(stateLock) { detachModel() }
        previousHandle?.let(backend::release)
        val loadedHandle = backend.load(canonicalPath, config)
        synchronized(stateLock) {
            if (closed) {
                backend.release(loadedHandle)
                throw ReaderLensRuntimeException("Le moteur local Lens a été fermé pendant son chargement")
            }
            loadedModelPath = canonicalPath
            modelHandle = loadedHandle
        }
        return loadedHandle
    }

    private fun endInference() {
        val handleToRelease = synchronized(stateLock) {
            inferenceActive = false
            if (closed) detachModel() else null
        }
        handleToRelease?.let(backend::release)
    }

    private fun detachModel(): Any? {
        val handle = modelHandle
        modelHandle = null
        loadedModelPath = null
        return handle
    }
}

private class AndroidReaderLensLlamaBackend : ReaderLensLlamaBackend {
    override suspend fun load(modelPath: String, config: ReaderLensLlamaConfig): Any =
        Llama.loadModel(
            modelPath = modelPath,
            config = LlamaConfig(
                contextSize = config.contextSize,
                threads = config.threads,
                gpuLayers = config.gpuLayers,
                temperature = config.temperature,
                topP = config.topP,
                topK = config.topK,
                seed = config.seed,
            ),
        )

    override suspend fun complete(handle: Any, prompt: String, maxTokens: Int): String =
        Llama.complete(
            model = handle as LlamaModel,
            prompt = prompt,
            maxTokens = maxTokens,
        ).text

    override fun release(handle: Any) {
        Llama.releaseModel(handle as LlamaModel)
    }
}

private const val READER_LENS_SUPPORTED_ABI = "arm64-v8a"
private const val READER_LENS_CONTEXT_SIZE = 2048
private const val READER_LENS_MAX_OUTPUT_TOKENS = 768
private const val MIN_READER_LENS_THREADS = 2
private const val MAX_READER_LENS_THREADS = 6
