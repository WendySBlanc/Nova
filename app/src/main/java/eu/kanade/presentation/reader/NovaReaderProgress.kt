package eu.kanade.presentation.reader

data class NovaReaderProgress(
    val sliderValue: Int,
    val sliderRange: IntRange,
    val label: String,
    val fraction: Float,
)

fun resolveNovaReaderProgress(
    currentPage: Int,
    totalPages: Int,
    isTextReader: Boolean,
): NovaReaderProgress {
    if (isTextReader) {
        val progress = currentPage.coerceIn(0, 100)
        return NovaReaderProgress(
            sliderValue = progress,
            sliderRange = 0..100,
            label = "$progress%",
            fraction = progress / 100f,
        )
    }

    val pageCount = totalPages.coerceAtLeast(1)
    val page = currentPage.coerceIn(1, pageCount)
    return NovaReaderProgress(
        sliderValue = page,
        sliderRange = 0..pageCount,
        label = "$page / $pageCount",
        fraction = page.toFloat() / pageCount,
    )
}

fun readerDestinationFromSlider(value: Int, isTextReader: Boolean): Int =
    if (isTextReader) value.coerceIn(0, 100) else (value - 1).coerceAtLeast(0)
