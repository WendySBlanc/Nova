@file:OptIn(ExperimentalSharedTransitionApi::class)

package eu.kanade.presentation.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

interface NovaTitleDetailScreen {
    val artworkMangaId: Long
}

@Stable
class NovaSharedTransitionState {
    var selectedMangaId by mutableStateOf<Long?>(null)
        private set

    var selectedCoverData by mutableStateOf<Any?>(null)
        private set

    var selectedTitle by mutableStateOf<String?>(null)
        private set

    private var selectedSourceKey by mutableStateOf<String?>(null)

    fun selectManga(
        mangaId: Long,
        coverData: Any?,
        title: String,
        sourceKey: String?,
    ) {
        selectedMangaId = mangaId
        selectedCoverData = coverData
        selectedTitle = title
        selectedSourceKey = sourceKey
    }

    internal fun hasArtworkFor(mangaId: Long, sourceKey: String): Boolean =
        selectedMangaId == mangaId && selectedSourceKey == sourceKey && selectedCoverData != null

    fun clearSelection() {
        selectedMangaId = null
        selectedCoverData = null
        selectedTitle = null
        selectedSourceKey = null
    }
}

val LocalNovaSharedTransitionState = compositionLocalOf<NovaSharedTransitionState?> { null }

internal val LocalNovaSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
internal val LocalNovaAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
internal val LocalNovaTransitionScreenKey = compositionLocalOf<String?> { null }

private data class NovaMangaArtworkKey(val mangaId: Long)

@Composable
fun Modifier.novaMangaSharedArtwork(
    mangaId: Long,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this
    val sharedScope = LocalNovaSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNovaAnimatedVisibilityScope.current ?: return this
    val motionScheme = MaterialTheme.motionScheme
    val artworkBoundsTransform = remember(motionScheme) {
        BoundsTransform { _, _ -> motionScheme.defaultSpatialSpec() }
    }

    return with(sharedScope) {
        this@novaMangaSharedArtwork.sharedBounds(
            sharedContentState = rememberSharedContentState(
                key = NovaMangaArtworkKey(mangaId),
            ),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = artworkBoundsTransform,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
        )
    }
}
