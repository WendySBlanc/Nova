package eu.kanade.presentation.theme

import kotlin.math.roundToInt

enum class NovaSpatialLibraryLayout {
    Compact,
    Split,
}

fun novaSpatialLibraryLayout(widthDp: Int): NovaSpatialLibraryLayout {
    return if (widthDp >= 840) NovaSpatialLibraryLayout.Split else NovaSpatialLibraryLayout.Compact
}

fun novaSpatialLibraryColumns(widthDp: Int): Int = when {
    widthDp < 600 -> 2
    widthDp < 840 -> 3
    widthDp < 1200 -> 4
    else -> 5
}

internal fun novaSpatialCarouselCardWidthDp(availableWidthDp: Int): Int =
    (availableWidthDp * (327f / 375f)).roundToInt().coerceIn(1, 420)

internal fun novaSpatialCarouselHeightDp(availableWidthDp: Int): Int =
    (novaSpatialCarouselCardWidthDp(availableWidthDp) * (370f / 327f)).roundToInt()
