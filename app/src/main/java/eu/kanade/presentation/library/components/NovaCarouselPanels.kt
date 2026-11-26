package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.home.novaPanelReveal
import eu.kanade.presentation.util.novaMangaSharedArtwork
import eu.kanade.presentation.util.rememberHighQualityImageRequest

@Composable
internal fun NovaCarouselPanels(
    items: List<NovaVikiManga>,
    pager: PagerState,
    height: Dp,
    reducedMotion: Boolean,
    isRtl: Boolean,
    selectedMangaId: Long?,
) {
    val pageKeyCorrection by remember(pager, items) {
        derivedStateOf {
            val currentPage = pager.currentPage
            val visibleKey = pager.layoutInfo.visiblePagesInfo.firstOrNull { it.index == currentPage }?.key
            val locatedPage = items.indexOfFirst { it.id == visibleKey }
            if (locatedPage < 0) 0 else locatedPage - currentPage
        }
    }
    val lowerPage by remember(pager, items.size, reducedMotion, pageKeyCorrection) {
        derivedStateOf {
            val position = (if (reducedMotion) pager.settledPage.toFloat() else pager.panelPosition()) + pageKeyCorrection
            position.toInt().coerceIn(items.indices)
        }
    }
    val upperPage = (lowerPage + 1).takeIf { !reducedMotion && it < items.size }
    val settled = (pager.settledPage + pageKeyCorrection).coerceIn(items.indices)
    val firstLayer = if (reducedMotion) lowerPage else minOf(lowerPage, settled - 1).coerceAtLeast(0)
    val lastLayer = if (reducedMotion) lowerPage else maxOf(upperPage ?: lowerPage, settled + 1).coerceAtMost(items.lastIndex)
    val background = MaterialTheme.colorScheme.background
    val shade = remember(background) {
        Brush.verticalGradient(
            0.12f to Color.Transparent,
            0.46f to background.copy(alpha = 0.08f),
            0.7f to background.copy(alpha = 0.88f),
            0.95f to background,
        )
    }
    Box(Modifier.fillMaxWidth().height(height).clipToBounds()) {
        for (page in firstLayer..lastLayer) {
            val item = items[page]
            key(item.id) {
                val reveal = when (page) {
                    lowerPage -> Modifier
                    upperPage -> Modifier.novaPanelReveal(isRtl) {
                        pager.panelPosition() + pageKeyCorrection - lowerPage
                    }
                    // Keep both neighbours measured for Coil, but draw only the two visible covers.
                    else -> Modifier.drawWithContent {}
                }
                Box(
                    Modifier
                        .novaMangaSharedArtwork(
                            item.id,
                            enabled = page == lowerPage && !pager.isScrollInProgress && item.id == selectedMangaId,
                        )
                        .then(reveal)
                        .fillMaxSize()
                        .background(item.colors.firstOrNull() ?: MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    AsyncImage(
                        model = rememberHighQualityImageRequest(item.coverData),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(Modifier.fillMaxSize().background(shade))
                }
            }
        }
    }
}

private fun PagerState.panelPosition(): Float = currentPage + currentPageOffsetFraction
