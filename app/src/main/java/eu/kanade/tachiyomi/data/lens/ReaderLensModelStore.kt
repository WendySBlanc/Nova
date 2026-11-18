package eu.kanade.tachiyomi.data.lens

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

internal data class ReaderLensModelSpec(
    val id: String,
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
)

internal sealed interface ReaderLensModelState {
    data object NotInstalled : ReaderLensModelState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : ReaderLensModelState {
        val percent: Int = ((downloadedBytes * 100L) / totalBytes.coerceAtLeast(1L)).toInt().coerceIn(0, 100)
    }
    data class Ready(val file: File) : ReaderLensModelState
    data class Failed(val message: String) : ReaderLensModelState
}

internal class ReaderLensModelException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal class ReaderLensModelStore(
    private val rootDirectory: File,
    client: OkHttpClient,
    internal val spec: ReaderLensModelSpec = INDEX_READER_LENS_MODEL,
    private val usableSpace: () -> Long = { rootDirectory.usableSpace },
    private val safetyReserveBytes: Long = DEFAULT_SAFETY_RESERVE_BYTES,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AutoCloseable {
    private val client = client.newBuilder()
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val requestMutex = Mutex()
    private val activeLock = Any()
    private var activeDownload: Deferred<File>? = null
    private val mutableState = MutableStateFlow<ReaderLensModelState>(ReaderLensModelState.NotInstalled)
    val state: StateFlow<ReaderLensModelState> = mutableState.asStateFlow()

    private val modelFile = File(rootDirectory, spec.fileName)
    private val partialFile = File(rootDirectory, "${spec.fileName}.part")
    private val verifiedFile = File(rootDirectory, "${spec.fileName}.verified")

    init {
        rootDirectory.mkdirs()
        if (hasVerifiedModel()) {
            mutableState.value = ReaderLensModelState.Ready(modelFile)
        }
    }

    suspend fun ensureModel(): File {
        val deferred = synchronized(activeLock) {
            activeDownload
                ?.takeUnless { it.isCancelled }
                ?: scope.async { downloadOrReuseModel() }.also { created ->
                    activeDownload = created
                    created.invokeOnCompletion { error ->
                        if (error != null) {
                            synchronized(activeLock) {
                                if (activeDownload === created) activeDownload = null
                            }
                        }
                    }
                }
        }
        return deferred.await()
    }

    fun cancelDownload() {
        synchronized(activeLock) {
            activeDownload?.cancel(CancellationException("Téléchargement Lens annulé"))
            activeDownload = null
        }
    }

    override fun close() {
        cancelDownload()
    }

    private suspend fun downloadOrReuseModel(): File = requestMutex.withLock {
        try {
            validateExistingModel()?.let { return@withLock it }
            preparePartialFile()
            downloadPartialFile()
            validatePartialFile()
            promotePartialFile()
            mutableState.value = ReaderLensModelState.Ready(modelFile)
            modelFile
        } catch (error: CancellationException) {
            mutableState.value = ReaderLensModelState.NotInstalled
            throw error
        } catch (error: ReaderLensModelException) {
            mutableState.value = ReaderLensModelState.Failed(error.message ?: "Le modèle Lens n'a pas pu être préparé")
            throw error
        } catch (error: Throwable) {
            val wrapped = ReaderLensModelException("Le modèle Lens n'a pas pu être préparé", error)
            mutableState.value = ReaderLensModelState.Failed(wrapped.message.orEmpty())
            throw wrapped
        }
    }

    private suspend fun validateExistingModel(): File? {
        if (hasVerifiedModel()) {
            mutableState.value = ReaderLensModelState.Ready(modelFile)
            return modelFile
        }
        if (!modelFile.isFile) return null
        if (modelFile.length() == spec.sizeBytes && modelFile.sha256() == spec.sha256) {
            writeVerificationMarker()
            mutableState.value = ReaderLensModelState.Ready(modelFile)
            return modelFile
        }
        modelFile.delete()
        verifiedFile.delete()
        return null
    }

    private fun preparePartialFile() {
        if (partialFile.length() > spec.sizeBytes) partialFile.delete()
        val remaining = spec.sizeBytes - partialFile.length()
        if (usableSpace() < remaining + safetyReserveBytes) {
            throw ReaderLensModelException("Espace insuffisant pour télécharger le modèle Lens")
        }
        partialFile.parentFile?.mkdirs()
    }

    private suspend fun downloadPartialFile() = withContext(Dispatchers.IO) {
        val offset = partialFile.length()
        val request = Request.Builder()
            .url(spec.url)
            .apply { if (offset > 0L) header("Range", "bytes=$offset-") }
            .build()
        val call = client.newCall(request)
        val cancellationHandle = currentCoroutineContext().job.invokeOnCompletion { error ->
            if (error is CancellationException) call.cancel()
        }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw ReaderLensModelException("Téléchargement Lens refusé (${response.code})")
                }
                val append = response.code == 206 && offset > 0L
                if (append) {
                    val contentRange = response.header("Content-Range").orEmpty()
                    if (!contentRange.startsWith("bytes $offset-")) {
                        throw ReaderLensModelException("La reprise du modèle Lens est invalide")
                    }
                }
                val start = if (append) offset else 0L
                RandomAccessFile(partialFile, "rw").use { output ->
                    if (!append) output.setLength(0L)
                    output.seek(start)
                    response.body.byteStream().use { input ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                        var downloaded = start
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloaded += count
                            mutableState.value = ReaderLensModelState.Downloading(downloaded, spec.sizeBytes)
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            throw error
        } finally {
            cancellationHandle.dispose()
        }
        if (partialFile.length() != spec.sizeBytes) {
            throw ReaderLensModelException("Le téléchargement Lens est incomplet")
        }
    }

    private suspend fun validatePartialFile() {
        if (partialFile.sha256() != spec.sha256) {
            partialFile.delete()
            throw ReaderLensModelException("Le modèle Lens téléchargé est corrompu")
        }
    }

    private fun promotePartialFile() {
        modelFile.delete()
        if (!partialFile.renameTo(modelFile)) {
            throw ReaderLensModelException("Le modèle Lens n'a pas pu être installé")
        }
        writeVerificationMarker()
    }

    private fun writeVerificationMarker() {
        val temporary = File(rootDirectory, ".${verifiedFile.name}.part")
        temporary.writeText(spec.sha256)
        verifiedFile.delete()
        if (!temporary.renameTo(verifiedFile)) {
            temporary.delete()
            throw ReaderLensModelException("La vérification du modèle Lens n'a pas pu être enregistrée")
        }
    }

    private fun hasVerifiedModel(): Boolean =
        modelFile.isFile &&
            modelFile.length() == spec.sizeBytes &&
            verifiedFile.isFile &&
            verifiedFile.readText().trim() == spec.sha256
}

private suspend fun File.sha256(): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { raw ->
        DigestInputStream(raw, digest).use { input ->
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            while (input.read(buffer) >= 0) {
                currentCoroutineContext().ensureActive()
            }
        }
    }
    digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

internal val INDEX_READER_LENS_MODEL = ReaderLensModelSpec(
    id = "index-translate-9b-q3-k-m@645939fbab839a656f1bc39975a872bc8ad749f8",
    fileName = "Index-Translate-9B.Q3_K_M.gguf",
    url = "https://huggingface.co/IndexTeam/Index-Translate-9B-GGUF/resolve/" +
        "645939fbab839a656f1bc39975a872bc8ad749f8/Index-Translate-9B.Q3_K_M.gguf?download=true",
    sizeBytes = 4_737_609_152L,
    sha256 = "7144b4aeb76febd2d6295bbade0f8fa5eb047bb20c11f94bc03ad5fa9a5071c0",
)

private const val DOWNLOAD_BUFFER_SIZE = 128 * 1024
private const val DEFAULT_SAFETY_RESERVE_BYTES = 512L * 1024L * 1024L
