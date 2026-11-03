@file:OptIn(ExperimentalSharedTransitionApi::class)

package eu.kanade.presentation.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

interface NovaTitleDetailScreen

@Stable
class NovaSharedTransitionState {
    var selectedMangaId by mutableStateOf<Long?>(null)
        private set

    var selectedCoverData by mutableStateOf<Any?>(null)
        private set

    var selectedTitle by mutableStateOf<String?>(null)
        private set

    fun selectManga(
        mangaId: Long,
        coverData: Any?,
        title: String,
    ) {
        selectedMangaId = mangaId
        selectedCoverData = coverData
        selectedTitle = title
    }
}

val LocalNovaSharedTransitionState = compositionLocalOf<NovaSharedTransitionState?> { null }

internal val LocalNovaSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
internal val LocalNovaAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

private data class NovaMangaSharedKey(
    val mangaId: Long,
    val element: String,
)

private val MangaBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

@Composable
fun Modifier.novaMangaSharedSurface(
    mangaId: Long,
    enabled: Boolean = true,
): Modifier = novaMangaSharedBounds(
    mangaId = mangaId,
    element = "surface",
    enabled = enabled,
)

@Composable
fun Modifier.novaMangaSharedArtwork(
    mangaId: Long,
    enabled: Boolean = true,
): Modifier = novaMangaSharedBounds(
    mangaId = mangaId,
    element = "artwork",
    enabled = enabled,
)

@Composable
private fun Modifier.novaMangaSharedBounds(
    mangaId: Long,
    element: String,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    val sharedScope = LocalNovaSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNovaAnimatedVisibilityScope.current ?: return this

    return with(sharedScope) {
        this@novaMangaSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(
                key = NovaMangaSharedKey(mangaId, element),
            ),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = MangaBoundsTransform,
        )
    }
}
