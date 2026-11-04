package eu.kanade.presentation.theme

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
