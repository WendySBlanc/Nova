package eu.kanade.presentation.library.components

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.util.rememberHighQualityImageRequest
import nova.cardmotion.CardCarousel
import nova.cardmotion.CardMotionMode
import nova.cardmotion.CardMotionSpec

@Composable
internal fun NovaRevResumeCarousel(
    items: List<NovaVikiManga>,
    onOpenManga: (Long) -> Unit,
    onSearch: () -> Unit,
) {
    val context = LocalContext.current
    val motionMode = remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        if (scale == 0f) CardMotionMode.Reduced else CardMotionMode.Spatial
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val carouselHeight = if (maxWidth < 380.dp) 232.dp else 276.dp
        CardCarousel(
            items = items,
            itemKey = NovaVikiManga::id,
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight),
            motion = CardMotionSpec(
                durationMillis = 260,
                blurRadius = 18.dp,
            ).resolved(motionMode),
            onItemExpanded = { item ->
                item.id.takeIf { id -> id >= 0L }?.let(onOpenManga)
            },
            emptyContent = {
                NovaEmptyResumeCard(onSearch = onSearch)
            },
            card = { item ->
                NovaResumeCard(item = item)
            },
        )
    }
}

@Composable
private fun NovaResumeCard(item: NovaVikiManga) {
    val coverRequest = rememberHighQualityImageRequest(item.coverData)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(item.colors)),
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
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.08f),
                        0.5f to Color.Black.copy(alpha = 0.3f),
                        1f to Color.Black.copy(alpha = 0.88f),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.tag.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
            )
            if (item.downloadedChapters > 0L) {
                Text(
                    text = "Hors ligne",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        NovaChapterBadges(
            item = item,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                color = Color.White.copy(alpha = 0.84f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(NovaVikiColors.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = if ((item.progress ?: 0f) > 0f) "Reprendre" else "Lire",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            item.progress?.let { progress ->
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(NovaVikiColors.Accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaEmptyResumeCard(onSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
