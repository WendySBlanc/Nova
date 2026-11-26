package eu.kanade.presentation.library.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.home.nextNovaCarouselPage
import eu.kanade.presentation.home.novaPagerMotionOffset
import eu.kanade.presentation.theme.novaReducedMotion
import eu.kanade.presentation.util.LocalNovaSharedTransitionState
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs
import kotlin.math.round

@Composable
internal fun NovaRevResumeCarousel(
    items: List<NovaVikiManga>,
    autoAdvanceEnabled: Boolean,
    onOpenManga: (Long) -> Unit,
    onResumeManga: (Long) -> Unit,
    onSearch: () -> Unit,
) {
    if (items.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(MR.strings.nova_home_empty),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = onSearch) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Text(stringResource(MR.strings.browse), modifier = Modifier.padding(start = 8.dp))
            }
        }
        return
    }

    val pagerState = rememberPagerState { items.size }
    val interactions = remember { MutableInteractionSource() }
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val isPressed by interactions.collectIsPressedAsState()
    val reducedMotion = novaReducedMotion()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val autoAdvanceSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val sharedTransition = LocalNovaSharedTransitionState.current
    val minimumTimeout = LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
        originalTimeoutMillis = AUTO_ADVANCE_DELAY_MILLIS,
        containsText = true,
        containsControls = true,
    ) ?: AUTO_ADVANCE_DELAY_MILLIS
    val autoAdvance = autoAdvanceEnabled && !reducedMotion && items.size > 1 && !isDragged && !isPressed
    LaunchedEffect(pagerState, items.size, autoAdvance, minimumTimeout, autoAdvanceSpec) {
        if (!autoAdvance) return@LaunchedEffect
        var direction = 1
        while (true) {
            delay(minimumTimeout)
            if (pagerState.isScrollInProgress) continue
            val settled = pagerState.settledPage
            val next = nextNovaCarouselPage(settled, items.size, direction)
            direction = next.compareTo(settled)
            pagerState.animateScrollToPage(next, animationSpec = autoAdvanceSpec)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val imageHeight = (maxWidth * 0.96f).coerceIn(264.dp, 466.dp)
            val type = MaterialTheme.typography
            val captionHeight = with(LocalDensity.current) {
                val label = type.labelLarge.lineHeight.toDp()
                val title = type.headlineMedium.lineHeight.toDp() * 2
                val metadata = type.bodyMedium.lineHeight.toDp()
                val button = (label * 2 + 16.dp).coerceAtLeast(48.dp)
                label + title + metadata + button + 26.dp
            }
            val captionTop = imageHeight * 0.58f
            val stageHeight = imageHeight.coerceAtLeast(captionTop + captionHeight)
            NovaCarouselPanels(
                items = items,
                pager = pagerState,
                height = imageHeight,
                reducedMotion = reducedMotion,
                isRtl = isRtl,
                selectedMangaId = sharedTransition?.selectedMangaId,
            )
            HorizontalPager(
                state = pagerState,
                key = { items[it].id },
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth().height(stageHeight).clipToBounds(),
            ) { index ->
                val item = items[index]
                val canResume = item.lastRead > 0L && item.remainingChapters > 0L
                NovaFeaturedStory(
                    item = item,
                    captionTop = captionTop,
                    canResume = canResume,
                    pageOffset = {
                        novaPagerMotionOffset(
                            page = index,
                            currentPage = pagerState.currentPage,
                            currentPageOffset = pagerState.currentPageOffsetFraction,
                            isRtl = isRtl,
                            reducedMotion = reducedMotion,
                        )
                    },
                    interactions = interactions,
                    onOpenManga = { onOpenManga(item.id) },
                    onResumeManga = { onResumeManga(item.id) },
                )
            }
        }
        if (items.size > 1) {
            val current = pagerState.settledPage.coerceIn(items.indices)
            val position = stringResource(MR.strings.nova_home_feature_position, current + 1, items.size)
            NovaCarouselIndicator(
                count = items.size,
                label = position,
                isRtl = isRtl,
                pagePosition = {
                    if (reducedMotion) pagerState.settledPage.toFloat()
                    else pagerState.currentPage + pagerState.currentPageOffsetFraction
                },
            )
        }
    }
}

@Composable
private fun NovaCarouselIndicator(count: Int, label: String, isRtl: Boolean, pagePosition: () -> Float) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        Modifier.padding(top = 8.dp, bottom = 4.dp)
            .size((count * 16).dp, 6.dp)
            .semantics { contentDescription = label },
    ) {
        val step = 16.dp.toPx()
        val radius = 2.5.dp.toPx()
        val last = count - 1f
        fun centerAt(page: Float): Float = step * (0.5f + if (isRtl) last - page else page)
        repeat(count) { index ->
            drawCircle(inactiveColor, radius, Offset(centerAt(index.toFloat()), center.y))
        }
        val position = pagePosition().coerceIn(0f, last)
        val halfWidth = (6.dp.toPx() + abs(position - round(position)) * 3.dp.toPx())
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(centerAt(position) - halfWidth, center.y - radius),
            size = Size(halfWidth * 2f, radius * 2f),
            cornerRadius = CornerRadius(radius),
        )
    }
}

@Composable
private fun NovaFeaturedStory(
    item: NovaVikiManga,
    captionTop: Dp,
    canResume: Boolean,
    pageOffset: () -> Float,
    interactions: MutableInteractionSource,
    onOpenManga: () -> Unit,
    onResumeManga: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(top = captionTop)
                .widthIn(max = 650.dp)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                .graphicsLayer {
                    val offset = pageOffset()
                    translationX = -offset * 20.dp.toPx()
                    alpha = 1f - abs(offset) * 0.40f
                },
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = stringResource(
                    if (canResume) MR.strings.nova_home_continue_reading else MR.strings.nova_home_featured,
                ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            val metadata = item.subtitle.ifBlank { item.tag }
            if (metadata.isNotBlank()) {
                Text(
                    text = metadata,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = if (canResume) onResumeManga else onOpenManga,
                    modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(8.dp),
                    interactionSource = interactions,
                ) {
                    Icon(
                        if (canResume) Icons.Filled.PlayArrow else Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(if (canResume) MR.strings.action_resume else MR.strings.nova_home_details),
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (canResume) {
                    OutlinedIconButton(
                        onClick = onOpenManga,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        interactionSource = interactions,
                    ) {
                        Icon(Icons.Outlined.Info, stringResource(MR.strings.nova_home_details))
                    }
                }
            }
        }
    }
}

@Composable
internal fun NovaEditorialCardHeading(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontSize = if (compact) 11.sp else 13.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = if (compact) 20.sp else 30.sp,
            lineHeight = if (compact) 22.sp else 32.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val AUTO_ADVANCE_DELAY_MILLIS = 5_500L
