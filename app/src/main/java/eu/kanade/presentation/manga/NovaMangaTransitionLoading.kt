package eu.kanade.presentation.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.util.LocalNovaSharedTransitionState
import eu.kanade.presentation.util.novaMangaSharedArtwork

@Composable
fun NovaMangaTransitionLoading(mangaId: Long) {
    val transitionState = LocalNovaSharedTransitionState.current
    val transitionActive = transitionState?.selectedMangaId == mangaId
    val coverData = transitionState?.selectedCoverData

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaVikiColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        if (transitionActive && coverData != null) {
            AsyncImage(
                model = coverData,
                contentDescription = transitionState.selectedTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .novaMangaSharedArtwork(mangaId)
                    .fillMaxWidth()
                    .fillMaxHeight(0.58f)
                    .align(Alignment.TopCenter),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.58f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.68f to NovaVikiColors.Background.copy(alpha = 0.34f),
                            1f to NovaVikiColors.Background,
                        ),
                    ),
            )
        }
        CircularProgressIndicator(color = NovaVikiColors.Accent)
    }
}
