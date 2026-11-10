package eu.kanade.tachiyomi.data.lens

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class ReaderLensRegion(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val text: String,
)

fun buildReaderLensChapterKey(sourceId: Long, mangaId: Long, chapterId: Long): String =
    "$sourceId:$mangaId:$chapterId"

internal data class ReaderLensImageTile(
    val top: Int,
    val bottom: Int,
    val sampleSize: Int,
    val outputWidth: Int,
    val outputHeight: Int,
)

internal fun buildReaderLensImageTiles(width: Int, height: Int): List<ReaderLensImageTile> {
    require(width > 0 && height > 0) { "Image dimensions must be positive" }

    var sampleSize = 1
    while (ceilDiv(width, sampleSize) > READER_LENS_MAX_OUTPUT_WIDTH) {
        sampleSize *= 2
    }

    val outputWidth = ceilDiv(width, sampleSize)
    val maxOutputHeight = (READER_LENS_MAX_TILE_PIXELS / outputWidth).coerceAtLeast(1)
    val maxSourceTileHeight = maxOutputHeight * sampleSize
    return buildList {
        var top = 0
        while (top < height) {
            val bottom = (top + maxSourceTileHeight).coerceAtMost(height)
            add(
                ReaderLensImageTile(
                    top = top,
                    bottom = bottom,
                    sampleSize = sampleSize,
                    outputWidth = outputWidth,
                    outputHeight = ceilDiv(bottom - top, sampleSize),
                ),
            )
            top = bottom
        }
    }
}

internal enum class ReaderLensScript(val fallbackLanguage: String) {
    Latin("en"),
    Korean("ko"),
    Japanese("ja"),
    Chinese("zh"),
}

internal data class ReaderLensScriptCandidate(
    val script: ReaderLensScript,
    val text: String,
)

internal fun selectReaderLensScript(candidates: List<ReaderLensScriptCandidate>): ReaderLensScript? {
    if (candidates.isEmpty()) return null
    val scored = candidates.map { candidate ->
        val matching = candidate.text.countMatching(candidate.script)
        val visible = candidate.text.count { !it.isWhitespace() }
        ScriptScore(candidate.script, matching, visible)
    }
    val cjk = scored.filter { it.script != ReaderLensScript.Latin && it.matchingCharacters >= MIN_CJK_CHARACTERS }
    return (cjk.ifEmpty { scored })
        .maxWithOrNull(compareBy<ScriptScore> { it.matchingCharacters }.thenBy { it.visibleCharacters })
        ?.script
}

internal fun shouldRunReaderLens(pageIndex: Int, available: Boolean, enabled: Boolean): Boolean =
    pageIndex >= 0 && available && enabled

internal suspend fun <T> runReaderLensNativeInference(block: suspend () -> T): T {
    val result = withContext(NonCancellable) { block() }
    currentCoroutineContext().ensureActive()
    return result
}

internal class ReaderLensAnalysisGate {
    private val mutex = Mutex()

    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock { block() }
}

class ReaderLensPageQueue {
    private val pending = ArrayDeque<Int>()
    private val completed = mutableSetOf<Int>()

    @Synchronized
    fun offerVisible(pageIndex: Int, nextPageIndex: Int?) {
        pending.clear()
        offerFirst(nextPageIndex)
        offerFirst(pageIndex)
    }

    @Synchronized
    fun poll(): Int? = pending.removeFirstOrNull()

    @Synchronized
    fun markCompleted(pageIndex: Int) {
        completed += pageIndex
        pending.remove(pageIndex)
    }

    @Synchronized
    fun clear() {
        pending.clear()
        completed.clear()
    }

    @Synchronized
    fun isEmpty(): Boolean = pending.isEmpty()

    private fun offerFirst(pageIndex: Int?) {
        if (pageIndex == null || pageIndex < 0 || pageIndex in completed) return
        pending.remove(pageIndex)
        pending.addFirst(pageIndex)
    }
}

private data class ScriptScore(
    val script: ReaderLensScript,
    val matchingCharacters: Int,
    val visibleCharacters: Int,
)

private fun String.countMatching(script: ReaderLensScript): Int {
    var count = 0
    var offset = 0
    while (offset < length) {
        val codePoint = Character.codePointAt(this, offset)
        val matches = when (script) {
            ReaderLensScript.Latin ->
                codePoint in 0x0041..0x005A || codePoint in 0x0061..0x007A || codePoint in 0x00C0..0x024F
            ReaderLensScript.Korean ->
                codePoint in 0x1100..0x11FF || codePoint in 0x3130..0x318F || codePoint in 0xAC00..0xD7AF
            ReaderLensScript.Japanese ->
                codePoint in 0x3040..0x30FF || codePoint in 0xFF65..0xFF9F || codePoint.isHan()
            ReaderLensScript.Chinese -> codePoint.isHan()
        }
        if (matches) {
            count += when {
                script == ReaderLensScript.Korean -> 3
                script == ReaderLensScript.Japanese && !codePoint.isHan() -> 4
                script == ReaderLensScript.Chinese -> 2
                else -> 1
            }
        }
        offset += Character.charCount(codePoint)
    }
    return count
}

private fun Int.isHan(): Boolean =
    this in 0x3400..0x4DBF || this in 0x4E00..0x9FFF || this in 0xF900..0xFAFF

private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor

internal const val READER_LENS_MAX_TILE_PIXELS = 4_000_000
private const val READER_LENS_MAX_OUTPUT_WIDTH = 2048
private const val MIN_CJK_CHARACTERS = 3
