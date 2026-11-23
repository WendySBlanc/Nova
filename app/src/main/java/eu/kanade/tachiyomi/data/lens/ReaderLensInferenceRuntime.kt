package eu.kanade.tachiyomi.data.lens

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

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
    suspend fun complete(handle: Any, prompt: String, systemPrompt: String, maxTokens: Int): String
    fun release(handle: Any)
}

internal interface ReaderLensInferenceRuntime : AutoCloseable {
    suspend fun complete(modelFile: File, prompt: String, maxOutputTokens: Int): String
    fun releaseWarmModel() = Unit
}

internal class ReaderLensRuntimeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal fun isReaderLensRuntimeSupported(
    abis: Array<String>,
    totalMemoryBytes: Long,
    sdkInt: Int,
): Boolean =
    sdkInt >= READER_LENS_MIN_SDK &&
        totalMemoryBytes >= READER_LENS_MIN_TOTAL_MEMORY_BYTES &&
        abis.any { it.equals(READER_LENS_SUPPORTED_ABI, ignoreCase = true) }

internal fun totalDeviceMemoryBytes(context: Context): Long {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo).totalMem
}

internal class ReaderLensMemoryPressureMonitor(
    private val application: Application,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : ComponentCallbacks2, AutoCloseable {

    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    init {
        application.registerComponentCallbacks(this)
    }

    fun register(listener: () -> Unit): AutoCloseable {
        listeners += listener
        return AutoCloseable { listeners -= listener }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        if (shouldReleaseReaderLensModel(level, sdkInt)) {
            listeners.forEach { it() }
        }
    }

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        listeners.forEach { it() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    override fun close() {
        listeners.clear()
        application.unregisterComponentCallbacks(this)
    }
}

@Suppress("DEPRECATION")
internal fun shouldReleaseReaderLensModel(trimLevel: Int, sdkInt: Int): Boolean =
    trimLevel >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
        (
            sdkInt < Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                trimLevel >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
            )

internal class LlamaReaderLensInferenceRuntime(
    private val backend: ReaderLensLlamaBackend = AndroidReaderLensLlamaBackend(),
    private val supportedAbis: Array<String> = Build.SUPPORTED_ABIS,
    private val totalMemoryBytes: Long,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    availableProcessors: Int = Runtime.getRuntime().availableProcessors(),
    private val releaseScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    memoryPressureMonitor: ReaderLensMemoryPressureMonitor? = null,
) : ReaderLensInferenceRuntime {

    private val inferenceMutex = Mutex()
    private val stateLock = Any()
    private val config = ReaderLensLlamaConfig(
        contextSize = READER_LENS_CONTEXT_SIZE,
        threads = availableProcessors.coerceIn(MIN_READER_LENS_THREADS, MAX_READER_LENS_THREADS),
        gpuLayers = 0,
        temperature = 0.7f,
        topP = 0.6f,
        topK = 20,
        seed = 0,
    )

    private var loadedModelPath: String? = null
    private var modelHandle: Any? = null
    private var releaseRequested = false
    private var closed = false
    private val memoryPressureRegistration = memoryPressureMonitor?.register(::releaseWarmModel)

    override suspend fun complete(
        modelFile: File,
        prompt: String,
        maxOutputTokens: Int,
    ): String = inferenceMutex.withLock {
        ensureSupported()
        ensureOpen()
        try {
            releaseRequestedModelIfNeeded(dispatchToBackground = true)
            val handle = loadModelIfNeeded(modelFile)
            backend.complete(handle, prompt, READER_LENS_SYSTEM_PROMPT, maxOutputTokens)
        } catch (error: CancellationException) {
            throw error
        } catch (error: ReaderLensRuntimeException) {
            throw error
        } catch (error: Throwable) {
            throw ReaderLensRuntimeException("Le moteur local Lens n'a pas pu traduire cette page", error)
        }
    }

    override fun close() {
        val shouldRelease = synchronized(stateLock) {
            if (closed) return
            closed = true
            releaseRequested = true
            memoryPressureRegistration?.close()
            true
        }
        if (shouldRelease) scheduleModelRelease()
    }

    override fun releaseWarmModel() {
        synchronized(stateLock) {
            releaseRequested = true
        }
        scheduleModelRelease()
    }

    private fun ensureSupported() {
        if (!isReaderLensRuntimeSupported(supportedAbis, totalMemoryBytes, sdkInt)) {
            throw ReaderLensRuntimeException("Lens local nécessite Android 9, ARM 64 bits et au moins 8 Gio de RAM")
        }
    }

    private fun ensureOpen() = synchronized(stateLock) {
        if (closed) {
            throw ReaderLensRuntimeException("Le moteur local Lens est déjà fermé")
        }
    }

    private suspend fun loadModelIfNeeded(modelFile: File): Any {
        val canonicalPath = modelFile.canonicalPath
        synchronized(stateLock) {
            if (loadedModelPath == canonicalPath) {
                return requireNotNull(modelHandle)
            }
        }

        val previousHandle = synchronized(stateLock) { detachModel() }
        releaseHandle(previousHandle)
        val loadedHandle = withContext(NonCancellable) {
            backend.load(canonicalPath, config)
        }
        val closedDuringLoad = synchronized(stateLock) {
            if (closed) {
                true
            } else {
                loadedModelPath = canonicalPath
                modelHandle = loadedHandle
                false
            }
        }
        if (closedDuringLoad) {
            releaseHandle(loadedHandle)
            throw ReaderLensRuntimeException("Le moteur local Lens a été fermé pendant son chargement")
        }
        currentCoroutineContext().ensureActive()
        return loadedHandle
    }

    private fun scheduleModelRelease() {
        releaseScope.launch {
            inferenceMutex.withLock {
                releaseRequestedModelIfNeeded(dispatchToBackground = false)
            }
        }
    }

    private suspend fun releaseRequestedModelIfNeeded(dispatchToBackground: Boolean) {
        val handleToRelease = synchronized(stateLock) {
            if (!releaseRequested) return
            releaseRequested = false
            detachModel()
        }
        if (dispatchToBackground) {
            releaseHandle(handleToRelease)
        } else if (handleToRelease != null) {
            withContext(NonCancellable) {
                backend.release(handleToRelease)
            }
        }
    }

    private suspend fun releaseHandle(handle: Any?) {
        if (handle == null) return
        withContext(NonCancellable + Dispatchers.Default) {
            backend.release(handle)
        }
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

    override suspend fun complete(handle: Any, prompt: String, systemPrompt: String, maxTokens: Int): String =
        Llama.complete(
            model = handle as LlamaModel,
            prompt = prompt,
            systemPrompt = systemPrompt,
            maxTokens = maxTokens,
        ).text

    override fun release(handle: Any) {
        Llama.releaseModel(handle as LlamaModel)
    }
}

private const val READER_LENS_SUPPORTED_ABI = "arm64-v8a"
internal const val READER_LENS_SYSTEM_PROMPT = ""
private const val READER_LENS_MIN_SDK = 28
private const val READER_LENS_MIN_TOTAL_MEMORY_BYTES = 8L * 1024L * 1024L * 1024L
private const val READER_LENS_CONTEXT_SIZE = 4096
private const val MIN_READER_LENS_THREADS = 2
private const val MAX_READER_LENS_THREADS = 6
