package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.novaSpatialCarouselHeightDp
import eu.kanade.presentation.theme.novaSpatialCarouselPreferredWidthDp
import eu.kanade.presentation.util.rememberHighQualityImageRequest
import kotlinx.coroutines.delay

@Composable
internal fun NovaRevResumeCarousel(
    items: List<NovaVikiManga>,
    onOpenManga: (Long) -> Unit,
    onSearch: () -> Unit,
) {
    if (items.isEmpty()) {
        NovaEmptyResumeCard(onSearch = onSearch)
        return
    }

    val carouselState = rememberCarouselState { items.size }
    val autoAdvanceSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(carouselState, items.size, autoAdvanceSpec) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(AUTO_ADVANCE_DELAY_MILLIS)
            if (carouselState.isScrollInProgress) continue

            val nextItem = (carouselState.currentItem + 1) % items.size
            if (nextItem == 0) {
                carouselState.scrollToItem(nextItem)
            } else {
                carouselState.animateScrollToItem(nextItem, autoAdvanceSpec)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidthDp = maxWidth.value.toInt()
        val carouselHeight = novaSpatialCarouselHeightDp(availableWidthDp).dp
        val preferredItemWidth = novaSpatialCarouselPreferredWidthDp(
            availableWidthDp = availableWidthDp,
            itemCount = items.size,
        ).dp
        val horizontalPadding = if (maxWidth < 600.dp) 12.dp else 16.dp

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = preferredItemWidth,
            itemSpacing = 10.dp,
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight),
        ) { index ->
            val item = items[index]
            NovaResumeCard(
                item = item,
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(RoundedCornerShape(20.dp))
                    .clickable(
                        enabled = item.id >= 0L,
                        role = Role.Button,
                        onClick = { onOpenManga(item.id) },
                    ),
            )
        }
    }
}

@Composable
private fun NovaResumeCard(
    item: NovaVikiManga,
    modifier: Modifier = Modifier,
) {
    val coverRequest = rememberHighQualityImageRequest(item.coverData)
    val fallbackAccent = NovaVikiColors.Accent
    val fallbackSurface = NovaVikiColors.Surface
    val palette = remember(item.colors, fallbackAccent, fallbackSurface) {
        when {
            item.colors.size >= 2 -> item.colors
            item.colors.size == 1 -> listOf(item.colors.first(), fallbackSurface)
            else -> listOf(fallbackAccent, fallbackSurface)
        }
    }
    Box(
        modifier = modifier.background(Brush.linearGradient(palette)),
    ) {
        if (item.coverData != null) {
            AsyncImage(
                model = coverRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.78f),
                        0.6f to Color.Black.copy(alpha = 0.2f),
                        1f to Color.Transparent,
                    ),
                )
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (item.downloadedChapters > 0L) "HORS LIGNE" else "À CONTINUER",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                NovaChapterBadges(item = item)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = item.subtitle,
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = item.progress
                        ?.let { progress -> "Reprendre - ${(progress.coerceIn(0f, 1f) * 100).toInt()} %" }
                        ?: "Lire",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NovaEmptyResumeCard(onSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Votre prochaine lecture commence ici",
            color = NovaVikiColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSearch) {
            Icon(Icons.Outlined.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Découvrir")
        }
    }
}

private const val AUTO_ADVANCE_DELAY_MILLIS = 5_500L
