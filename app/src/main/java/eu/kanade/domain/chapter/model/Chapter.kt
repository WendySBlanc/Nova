package eu.kanade.domain.chapter.model

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import tachiyomi.domain.chapter.model.Chapter
import eu.kanade.tachiyomi.data.database.models.Chapter as DbChapter

// TODO: Remove when all deps are migrated
fun Chapter.toSChapter(): SChapter {
    return SChapter.create().also {
        it.url = url
        it.name = name
        it.date_upload = dateUpload
        it.chapter_number = chapterNumber.toFloat()
        it.scanlator = scanlator
        it.memo = memo.withoutNovaSourceReleaseTime()
    }
}

private const val NOVA_SOURCE_RELEASE_KEY = "com.nova.manhwa.sourceReleaseTime"

internal fun JsonObject.novaSourceReleaseTime(): Long? =
    (get(NOVA_SOURCE_RELEASE_KEY) as? JsonPrimitive)?.longOrNull

internal fun JsonObject.withNovaSourceReleaseTime(time: Long): JsonObject {
    val timestamp = time.coerceAtLeast(0L)
    return if (novaSourceReleaseTime() == timestamp) this else
        JsonObject(this + (NOVA_SOURCE_RELEASE_KEY to JsonPrimitive(timestamp)))
}

internal fun JsonObject.withoutNovaSourceReleaseTime(): JsonObject =
    if (NOVA_SOURCE_RELEASE_KEY in this) JsonObject(this - NOVA_SOURCE_RELEASE_KEY) else this

fun Chapter.copyFromSChapter(sChapter: SChapter): Chapter {
    return this.copy(
        name = sChapter.name,
        url = sChapter.url,
        dateUpload = sChapter.date_upload,
        chapterNumber = sChapter.chapter_number.toDouble(),
        scanlator = sChapter.scanlator?.ifBlank { null }?.trim(),
        memo = sChapter.memo,
    )
}

fun Chapter.toDbChapter(): DbChapter = ChapterImpl().also {
    it.id = id
    it.manga_id = mangaId
    it.url = url
    it.name = name
    it.scanlator = scanlator
    it.read = read
    it.bookmark = bookmark
    it.last_page_read = lastPageRead.toInt()
    it.date_fetch = dateFetch
    it.date_upload = dateUpload
    it.chapter_number = chapterNumber.toFloat()
    it.source_order = sourceOrder.toInt()
    it.memo = memo.withoutNovaSourceReleaseTime()
    it.last_modified = lastModifiedAt
}
