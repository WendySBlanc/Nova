package nova.cardmotion

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CardMotionMode {
    Spatial,
    Reduced,
}

data class CardMotionSpec(
    val durationMillis: Int = 260,
    val blurRadius: Dp = 18.dp,
    val scrim: Color = Color.Black.copy(alpha = 0.48f),
) {
    init {
        require(durationMillis >= 0) { "durationMillis must be non-negative" }
        require(blurRadius.value.isFinite() && blurRadius >= 0.dp) { "blurRadius must be finite and non-negative" }
    }

    fun resolved(mode: CardMotionMode): CardMotionSpec = when (mode) {
        CardMotionMode.Spatial -> this
        CardMotionMode.Reduced -> copy(durationMillis = 0, blurRadius = 0.dp)
    }
}

private data class CardSelection<T>(
    val key: Any,
    val item: T,
    val bounds: Rect,
)

/**
 * Rev-style carousel whose selected card expands before opening canonical host content.
 * Keys must be stable and unique. Essential actions must also remain available outside gestures.
 */
@Composable
fun <T> CardCarousel(
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    motion: CardMotionSpec = CardMotionSpec(),
    onItemExpanded: ((T) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    emptyContent: @Composable BoxScope.() -> Unit = {},
    backdrop: @Composable BoxScope.() -> Unit = {},
    card: @Composable BoxScope.(T) -> Unit,
    detail: (@Composable BoxScope.(item: T, dismiss: () -> Unit) -> Unit)? = null,
) {
    val keys = remember(items) { items.map(itemKey) }
    require(keys.size == keys.distinct().size) { "CardCarousel keys must be unique" }

    val progress = remember { Animatable(0f) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var selection by remember { mutableStateOf<CardSelection<T>?>(null) }
    var dismissRequested by remember { mutableStateOf(false) }
    val currentOnItemExpanded by rememberUpdatedState(onItemExpanded)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val active = selection
    val isOpen = active != null
    val dismiss = { dismissRequested = true }

    BackHandler(enabled = isOpen, onBack = dismiss)

    LaunchedEffect(active?.key, dismissRequested) {
        val selected = active ?: return@LaunchedEffect
        if (dismissRequested) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(motion.durationMillis, easing = CardMotionEasing),
            )
            selection = null
            dismissRequested = false
            currentOnDismiss?.invoke()
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(motion.durationMillis, easing = CardMotionEasing),
            )
            currentOnItemExpanded?.let { activate ->
                activate(selected.item)
                progress.snapTo(0f)
                selection = null
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        if (items.isEmpty()) {
            emptyContent()
            return@BoxWithConstraints
        }

        val pagerState = rememberPagerState { items.size }
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        val carouselWidth = minOf(viewportWidth * 0.86f, 420.dp)
        val targetWidth = minOf(viewportWidth - 24.dp, 520.dp)
        val targetHeight = targetWidth / CardAspectRatio
        val targetTop = minOf(24.dp, viewportHeight * 0.05f)
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isOpen) {
                        Modifier.clearAndSetSemantics { hideFromAccessibility() }
                    } else {
                        Modifier
                    },
                )
                .blur(motion.blurRadius * progress.value),
        ) {
            backdrop()
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.align(Alignment.Center),
                contentPadding = PaddingValues(horizontal = (viewportWidth - carouselWidth) / 2),
                pageSpacing = 12.dp,
                userScrollEnabled = !isOpen,
                key = { index -> itemKey(items[index]) },
            ) { index ->
                val item = items[index]
                val key = itemKey(item)
                var bounds by remember(key) { mutableStateOf(Rect.Zero) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(CardAspectRatio)
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInRoot()
                            bounds = Rect(
                                left = position.x,
                                top = position.y,
                                right = position.x + coordinates.size.width,
                                bottom = position.y + coordinates.size.height,
                            )
                        }
                        .graphicsLayer { alpha = if (active?.key == key) 0f else 1f }
                        .clickable(enabled = !isOpen) {
                            if (!pagerState.isScrollInProgress && bounds.width > 0f) {
                                selection = CardSelection(key, item, bounds)
                            }
                        },
                ) {
                    card(item)
                }
            }
        }

        if (active != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(motion.scrim.copy(alpha = motion.scrim.alpha * progress.value))
                    .clickable(onClick = dismiss)
                    .clearAndSetSemantics { hideFromAccessibility() },
            )

            val targetX = with(density) { ((viewportWidth - targetWidth) / 2).toPx() }
            val targetY = with(density) { targetTop.toPx() }
            val targetWidthPx = with(density) { targetWidth.toPx() }
            Box(
                modifier = Modifier
                    .size(targetWidth, targetHeight)
                    .graphicsLayer {
                        val fraction = progress.value
                        val startX = active.bounds.left - origin.x
                        val startY = active.bounds.top - origin.y
                        translationX = startX + (targetX - startX) * fraction
                        translationY = startY + (targetY - startY) * fraction
                        val startScale = active.bounds.width / targetWidthPx
                        scaleX = startScale + (1f - startScale) * fraction
                        scaleY = scaleX
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
                card(active.item)
            }

            if (detail != null && progress.value >= 1f && currentOnItemExpanded == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = targetTop + targetHeight + 12.dp),
                ) {
                    detail(active.item, dismiss)
                }
            }
        }
    }
}

private const val CardAspectRatio = 1.586f

private val CardMotionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
