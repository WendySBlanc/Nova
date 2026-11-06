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
    widthDp < 340 -> 2
    widthDp < 600 -> 3
    widthDp < 840 -> 4
    widthDp < 1200 -> 5
    else -> 6
}

fun novaSpatialHomeColumns(widthDp: Int): Int = when {
    widthDp < 600 -> 2
    widthDp < 960 -> 3
    else -> 4
}

fun novaSpatialCarouselHeightDp(widthDp: Int): Int = when {
    widthDp < 360 -> 220
    widthDp < 600 -> 232
    else -> 248
}

fun novaSpatialCarouselPreferredWidthDp(
    availableWidthDp: Int,
    itemCount: Int,
): Int {
    val safeWidth = availableWidthDp.coerceAtLeast(1)
    val horizontalPadding = if (safeWidth < 600) 24 else 32
    val usableWidth = (safeWidth - horizontalPadding).coerceAtLeast(1)
    if (itemCount <= 1) return usableWidth

    val expansion = ((safeWidth - 320f) / 880f).coerceIn(0f, 1f)
    val visibleItemTarget = 1.18f + (1.82f * expansion)
    val preferredWidth = (usableWidth / visibleItemTarget).roundToInt()
    val minimumWidth = minOf(168, usableWidth)
    val maximumWidth = minOf(360, usableWidth)
    return preferredWidth.coerceIn(minimumWidth, maximumWidth)
}
