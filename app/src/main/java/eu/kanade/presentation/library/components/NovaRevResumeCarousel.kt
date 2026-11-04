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
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaGlassSurface
import eu.kanade.presentation.theme.novaNeonProgress
import eu.kanade.presentation.theme.novaSpatialBackdrop
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
        val carouselHeight = if (maxWidth < 380.dp) 236.dp else 280.dp
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
            .novaSpatialBackdrop(
                background = NovaVikiColors.Surface,
                primary = item.colors.firstOrNull() ?: NovaVikiColors.Accent,
                secondary = NovaVikiColors.AccentWarm,
                tertiary = NovaVikiColors.AccentGold,
                wireColor = Color.White,
                wireAlpha = 0.12f,
            )
            .novaNeonProgress(
                progress = item.progress ?: 0f,
                trackColor = Color.White.copy(alpha = 0.16f),
                progressColor = NovaVikiColors.Accent,
                thickness = 4.dp,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(0.43f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
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
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.48f),
                            ),
                        ),
                )
                Text(
                    text = item.tag.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(9.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.57f)
                    .fillMaxHeight()
                    .novaGlassSurface(
                        color = NovaVikiColors.SurfaceRaised,
                        outline = NovaVikiColors.Outline,
                    )
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (item.downloadedChapters > 0L) "HORS LIGNE" else "À CONTINUER",
                        color = NovaVikiColors.Accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    NovaChapterBadges(item = item)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = item.title,
                    color = NovaVikiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.subtitle,
                    color = NovaVikiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
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
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text(
                            text = if ((item.progress ?: 0f) > 0f) "Reprendre" else "Lire",
                            color = NovaVikiColors.TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                        )
                        item.progress?.let { progress ->
                            Text(
                                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()} %",
                                color = NovaVikiColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
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
