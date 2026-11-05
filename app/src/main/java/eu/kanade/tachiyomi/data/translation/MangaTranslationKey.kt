package eu.kanade.tachiyomi.data.translation

fun buildMangaTranslationChapterKey(sourceId: Long, mangaId: Long, chapterId: Long): String =
    "$sourceId:$mangaId:$chapterId"
