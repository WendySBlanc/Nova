package eu.kanade.presentation.home

internal fun novaPanelEnergy(progress: Float): Float {
    val reveal = progress.coerceIn(0f, 1f)
    return 4f * reveal * (1f - reveal)
}

internal fun novaPanelEdge(progress: Float, heightFraction: Float, isRtl: Boolean): Float {
    val reveal = progress.coerceIn(0f, 1f)
    val skew = (1f - 2f * heightFraction) * 0.20f * novaPanelEnergy(reveal)
    val edge = 1f - reveal + skew
    return if (isRtl) 1f - edge else edge
}

internal fun novaDockWake(head: Float, tail: Float): Float = (head - tail).coerceIn(-0.92f, 0.92f)
