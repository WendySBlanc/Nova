package eu.kanade.presentation.library

import eu.kanade.domain.ui.model.NovaLibraryPresentation
import kotlin.math.roundToInt

internal data class NovaCatalogMetrics(
    val columns: Int,
    val sidePadding: Int,
    val columnGap: Int,
    val rowGap: Int,
)

internal fun novaCatalogMetrics(width: Int, layout: NovaLibraryPresentation): NovaCatalogMetrics {
    if (layout == NovaLibraryPresentation.Grid) {
        val gap = (width / 40f).roundToInt().coerceIn(6, 12)
        return NovaCatalogMetrics(
            columns = 3,
            sidePadding = (width / 26f).roundToInt().coerceIn(12, 20),
            columnGap = gap,
            rowGap = gap,
        )
    }
    val columns = if (layout == NovaLibraryPresentation.Cards && width >= 720) 2 else 1
    val sidePadding = (width / 16f).roundToInt().coerceIn(12, 32)
    val gap = (width * 0.043f).roundToInt().coerceIn(12, 22)
    return NovaCatalogMetrics(
        columns = columns,
        sidePadding = sidePadding + if (layout == NovaLibraryPresentation.Cards && width >= 380) 8 else 0,
        columnGap = gap,
        rowGap = 16,
    )
}
