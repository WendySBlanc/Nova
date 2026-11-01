package eu.kanade.presentation.util

import tachiyomi.domain.manga.model.Manga

fun Manga?.isLikelyLightNovel(
    sourceName: String? = null,
    categoryNames: Iterable<String?> = emptyList(),
): Boolean {
    val manga = this ?: return false
    return manga.title.isLightNovelLabel() ||
        sourceName.isLightNovelLabel() ||
        manga.genre.orEmpty().any { it.isLightNovelLabel() } ||
        categoryNames.any { it.isLightNovelLabel() }
}

private fun String?.isLightNovelLabel(): Boolean {
    val normalized = this?.lowercase().orEmpty()
    return normalized == "ln" ||
        "light novel" in normalized ||
        "web novel" in normalized ||
        "novel" in normalized ||
        "lnreader" in normalized
}
