package eu.kanade.tachiyomi.data.lens

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer

internal class OnnxProofbirdGectorRuntime(
    private val environment: OrtEnvironment = OrtEnvironment.getEnvironment(),
    private val releaseScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ProofbirdGectorRuntime {
    private val mutex = Mutex()
    private val stateLock = Any()
    private var loadedModelPath: String? = null
    private var session: OrtSession? = null
    private var releaseRequested = false
    private var closed = false

    override suspend fun predict(
        modelFile: File,
        encodings: List<ProofbirdEncoding>,
    ): List<IntArray> {
        if (encodings.isEmpty()) return emptyList()
        return withContext(Dispatchers.Default) {
            mutex.withLock {
                ensureOpen()
                releaseRequestedSessionIfNeeded()
                val activeSession = getOrCreateSession(modelFile)
                val maxTokens = encodings.maxOf { it.inputIds.size }
                val batchSize = encodings.size
                val inputIds = LongArray(batchSize * maxTokens)
                val attentionMask = LongArray(batchSize * maxTokens)
                encodings.forEachIndexed { batchIndex, encoding ->
                    encoding.inputIds.copyInto(inputIds, destinationOffset = batchIndex * maxTokens)
                    attentionMask.fill(
                        element = 1L,
                        fromIndex = batchIndex * maxTokens,
                        toIndex = batchIndex * maxTokens + encoding.inputIds.size,
                    )
                }
                val shape = longArrayOf(batchSize.toLong(), maxTokens.toLong())
                OnnxTensor.createTensor(environment, LongBuffer.wrap(inputIds), shape).use { idsTensor ->
                    OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), shape).use { maskTensor ->
                        activeSession.run(
                            mapOf(INPUT_IDS to idsTensor, ATTENTION_MASK to maskTensor),
                            setOf(LABEL_IDS_OUTPUT),
                        ).use { output ->
                            @Suppress("UNCHECKED_CAST")
                            val labelIds = output[0].value as Array<LongArray>
                            selectProofbirdWordLabels(labelIds, encodings)
                        }
                    }
                }
            }
        }
    }

    override fun releaseWarmModel() {
        synchronized(stateLock) {
            releaseRequested = true
        }
        scheduleSessionRelease()
    }

    override fun close() {
        synchronized(stateLock) {
            if (closed) return
            closed = true
            releaseRequested = true
        }
        scheduleSessionRelease()
    }

    private fun getOrCreateSession(modelFile: File): OrtSession {
        val path = modelFile.absolutePath
        if (loadedModelPath == path) session?.let { return it }
        releaseSession()
        val created = OrtSession.SessionOptions().use { options ->
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            environment.createSession(path, options)
        }
        session = created
        loadedModelPath = path
        return created
    }

    private fun ensureOpen() {
        check(!synchronized(stateLock) { closed }) { "Proofbird runtime is closed" }
    }

    private fun scheduleSessionRelease() {
        releaseScope.launch {
            mutex.withLock {
                releaseRequestedSessionIfNeeded()
            }
        }
    }

    private fun releaseRequestedSessionIfNeeded() {
        val shouldRelease = synchronized(stateLock) {
            releaseRequested.also { requested ->
                if (requested) releaseRequested = false
            }
        }
        if (shouldRelease) releaseSession()
    }

    private fun releaseSession() {
        session?.close()
        session = null
        loadedModelPath = null
    }
}

internal fun selectProofbirdWordLabels(
    labelIds: Array<LongArray>,
    encodings: List<ProofbirdEncoding>,
): List<IntArray> {
    require(labelIds.size == encodings.size) { "Proofbird returned an invalid batch" }
    return encodings.mapIndexed { batchIndex, encoding ->
        IntArray(encoding.firstTokenIndices.size) { wordIndex ->
            labelIds[batchIndex][encoding.firstTokenIndices[wordIndex]].toInt()
        }
    }
}

private const val INPUT_IDS = "input_ids"
private const val ATTENTION_MASK = "attention_mask"
private const val LABEL_IDS_OUTPUT = "label_ids"
