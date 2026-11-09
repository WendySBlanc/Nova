package eu.kanade.tachiyomi.data.lens

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

class ReaderLensPageQueue {
    private val pending = ArrayDeque<Int>()
    private val completed = mutableSetOf<Int>()

    @Synchronized
    fun offerVisible(pageIndex: Int, nextPageIndex: Int?) {
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
