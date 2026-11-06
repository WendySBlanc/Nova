package eu.kanade.presentation.library.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.NovaFluidFixedTabRow
import eu.kanade.presentation.home.NovaHomeSections
import eu.kanade.presentation.library.NovaLibraryEntry
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaGlassSurface
import eu.kanade.presentation.theme.novaNeonProgress
import eu.kanade.presentation.theme.novaSpatialBackdrop
import eu.kanade.presentation.theme.novaSpatialHomeColumns
import eu.kanade.presentation.util.LocalNovaSharedTransitionState
import eu.kanade.presentation.util.novaMangaSharedArtwork
import eu.kanade.presentation.util.rememberHighQualityImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.foundation.lazy.grid.items as gridItems

typealias NovaVikiManga = NovaLibraryEntry

@Composable
fun NovaVikiHomeScreen(
    sections: NovaHomeSections,
    onSearch: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onRefresh: () -> Boolean,
    resetToTopKey: Int,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var isRefreshing by remember { mutableStateOf(false) }
    val heroItemsToShow = remember(sections) {
        sections.resume
            .ifEmpty { sections.newChapters }
            .ifEmpty { sections.recentlyAdded }
            .take(8)
    }
    var selectedRail by remember { mutableStateOf<NovaRail?>(null) }
    val rails = remember(sections) {
        if (sections.newChapters.isNotEmpty() || sections.recentlyAdded.isNotEmpty()) {
            listOf(
                NovaRail(
                    title = "Nouveaux chapitres",
                    items = sections.newChapters,
                ),
                NovaRail(
                    title = "Ajoutés récemment",
                    items = sections.recentlyAdded,
                ),
            ).filter { rail -> rail.items.isNotEmpty() }
        } else {
            fallbackSections
        }
    }

    LaunchedEffect(resetToTopKey) {
        if (resetToTopKey > 0) {
            selectedRail = null
            gridState.scrollToItem(0)
        }
    }

    BackHandler(enabled = selectedRail != null) {
        selectedRail = null
    }

    selectedRail?.let { section ->
        NovaRailListScreen(
            section = section,
            onBack = { selectedRail = null },
            onOpenManga = onOpenManga,
        )
        return
    }

    PullRefresh(
        refreshing = isRefreshing,
        enabled = selectedRail == null,
        onRefresh = {
            val started = onRefresh()
            if (!started) return@PullRefresh
            scope.launch {
                isRefreshing = true
                delay(1000)
                isRefreshing = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .novaSpatialBackdrop(
                background = NovaVikiColors.Background,
                primary = NovaVikiColors.Accent,
                secondary = NovaVikiColors.AccentWarm,
                tertiary = NovaVikiColors.AccentGold,
                wireColor = NovaVikiColors.OutlineStrong,
                wireAlpha = 0.07f,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            NovaVikiSearchHeader(
                title = stringResource(MR.strings.label_home_nova),
                onSearch = onSearch,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val columnCount = novaSpatialHomeColumns(maxWidth.value.toInt())
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(
                        key = "home_resume_stage",
                        contentType = "resume_stage",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        NovaRevResumeCarousel(
                            items = heroItemsToShow,
                            onOpenManga = onOpenManga,
                            onSearch = onSearch,
                        )
                    }
                    rails.forEach { section ->
                        item(
                            key = "home_section_${section.title}",
                            contentType = "section_header",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            NovaSpatialSectionHeader(
                                section = section,
                                onClick = { selectedRail = section },
                            )
                        }
                        gridItems(
                            items = section.items.take(columnCount * 2),
                            key = { item -> "${section.title}_${item.id}" },
                            contentType = { "spatial_poster" },
                        ) { item ->
                            NovaSpatialPosterCard(
                                item = item,
                                onClick = {
                                    if (item.id >= 0L) {
                                        onOpenManga(item.id)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NovaVikiSearchHeader(
    title: String,
    subtitle: String? = null,
    onSearch: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(MR.strings.action_search),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
internal fun NovaVikiLibraryTabs(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovaVikiColors.Background),
    ) {
        val compact = maxWidth < 360.dp
        NovaFluidFixedTabRow(
            selectedIndex = selectedIndex,
            itemCount = 2,
            onSelect = onSelect,
            background = NovaVikiColors.Surface,
            outline = NovaVikiColors.OutlineStrong,
            accent = NovaVikiColors.Accent,
            secondary = NovaVikiColors.AccentWarm,
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = NovaVikiDimensions.WideContentMaxWidth)
                .fillMaxWidth()
                .height(if (compact) 48.dp else 52.dp)
                .padding(horizontal = if (compact) 10.dp else 14.dp),
        ) { index, selected ->
            Text(
                text = if (index == 0) "All Series" else "Light Novel",
                color = if (selected) NovaVikiColors.TextPrimary else NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun NovaVikiHero(
    heroes: List<NovaVikiManga>,
    onPlay: (Long?) -> Unit,
    onOpenManga: (Long) -> Unit,
    compactLayout: Boolean = false,
) {
    val selectedTransitionMangaId = LocalNovaSharedTransitionState.current?.selectedMangaId
    val pageCount = heroes.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val pagerIsDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val pageInteractionSource = remember { MutableInteractionSource() }
    val pageIsPressed by pageInteractionSource.collectIsPressedAsState()
    val autoAdvance = heroes.size > 1 && !pagerIsDragged && !pageIsPressed

    LaunchedEffect(heroes.size, autoAdvance) {
        if (!autoAdvance) return@LaunchedEffect
        while (true) {
            delay(4500)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % heroes.size)
        }
    }

    Column {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val slideWidth = maxWidth
            val heroHeight = when {
                compactLayout && maxWidth < 360.dp -> (maxWidth * 0.78f).coerceIn(246.dp, 270.dp)
                compactLayout && maxWidth < 600.dp -> (maxWidth * 0.76f).coerceIn(270.dp, 304.dp)
                compactLayout && maxWidth < 840.dp -> 320.dp
                compactLayout -> 350.dp
                maxWidth < 360.dp -> (maxWidth * 0.92f).coerceIn(300.dp, 342.dp)
                maxWidth >= 840.dp -> 430.dp
                maxWidth >= 600.dp -> (maxWidth * 0.58f).coerceIn(380.dp, 430.dp)
                else -> (maxWidth * 1.02f).coerceIn(342.dp, 386.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight),
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize(),
                ) { index ->
                    NovaHeroSlide(
                        hero = heroes.getOrNull(index),
                        index = index,
                        count = heroes.size,
                        interactionSource = pageInteractionSource,
                        modifier = Modifier.width(slideWidth),
                        onPlay = onPlay,
                        onOpenManga = onOpenManga,
                        sharedTransitionActive = heroes.getOrNull(index)?.id == selectedTransitionMangaId,
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaHeroSlide(
    hero: NovaVikiManga?,
    index: Int,
    count: Int,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    onPlay: (Long?) -> Unit,
    onOpenManga: (Long) -> Unit,
    sharedTransitionActive: Boolean,
) {
    val title = hero?.title ?: "Your Manhwa"
    val subtitle = hero?.subtitle ?: "Find your next chapter"
    val tag = hero?.tag ?: "NOVA"
    val coverRequest = rememberHighQualityImageRequest(hero?.coverData)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = hero?.id?.let { it >= 0L } == true,
                role = Role.Button,
                onClick = {
                    val id = hero?.id ?: return@clickable
                    if (id >= 0L) {
                        onOpenManga(id)
                    }
                },
            )
            .background(
                Brush.verticalGradient(
                    hero?.colors ?: listOf(NovaVikiColors.SurfaceStrong, NovaVikiColors.AccentWarm, NovaVikiColors.Background),
                ),
            ),
    ) {
        val compact = maxWidth < 380.dp
        val wide = maxWidth >= 600.dp
        val posterWidth = when {
            compact -> (maxWidth * 0.22f).coerceIn(70.dp, 82.dp)
            wide -> (maxWidth * 0.16f).coerceIn(112.dp, 140.dp)
            else -> (maxWidth * 0.27f).coerceIn(92.dp, 108.dp)
        }
        val posterHeight = posterWidth * 1.49f
        val contentMaxWidth = if (compact) maxWidth - 28.dp else maxWidth * (if (wide) 0.56f else 0.62f)
        val bottomPadding = if (compact) 16.dp else 22.dp
        val titleStyle = if (compact) {
            MaterialTheme.typography.headlineSmall
        } else {
            MaterialTheme.typography.headlineMedium
        }

        if (hero?.coverData != null) {
            AsyncImage(
                model = coverRequest,
                contentDescription = hero.title,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .novaMangaSharedArtwork(hero.id, enabled = sharedTransitionActive)
                    .fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.86f),
                            0.52f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        } else {
            HeroArtwork(hero)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.48f to Color.Transparent,
                        0.78f to NovaVikiColors.Background.copy(alpha = 0.76f),
                        1f to NovaVikiColors.Background,
                    ),
                ),
        )
        if (hero?.coverData != null && !compact) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = if (wide) 34.dp else 18.dp, top = 18.dp)
                    .width(posterWidth)
                    .height(posterHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF202020)),
            ) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = hero.title,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )
                NovaChapterBadges(
                    item = hero,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp),
                )
            }
        }
        if (hero != null && compact) {
            NovaChapterBadges(
                item = hero,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(contentMaxWidth)
                .padding(horizontal = 14.dp)
                .padding(bottom = bottomPadding),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.13f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = tag.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                style = titleStyle,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (count > 1) {
                    RatingBadge(text = "${index + 1}/$count")
                } else {
                    RatingBadge(text = "READ")
                }
                Text(
                    text = subtitle,
                    color = Color(0xFFE6E6E6),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hero?.progress != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(hero.progress.coerceIn(0f, 1f))
                            .background(NovaVikiColors.Accent),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onPlay(hero?.id?.takeIf { it >= 0L }) },
                interactionSource = interactionSource,
                shape = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
                modifier = Modifier
                    .height(NovaVikiDimensions.MinimumTouchTarget)
                    .fillMaxWidth(if (compact) 1f else 0.62f),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    text = when {
                        hero == null -> "Rechercher"
                        hero.progress != null && hero.progress > 0f -> "Reprendre"
                        else -> "Lire"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
        }
        if (count > 1) {
            NovaHeroDots(
                count = count,
                index = index,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 26.dp),
            )
        }
    }
}

@Composable
private fun NovaHeroDots(
    count: Int,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { dot ->
            Box(
                modifier = Modifier
                    .width(if (dot == index) 16.dp else 6.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (dot == index) {
                            NovaVikiColors.Accent
                        } else {
                            NovaVikiColors.TextSecondary.copy(alpha = 0.48f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun HeroArtwork(hero: NovaVikiManga?) {
    val coverRequest = rememberHighQualityImageRequest(hero?.coverData)
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 20.dp)
                .width(125.dp)
                .height(179.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        hero?.colors ?: listOf(Color(0xFFFFD66E), Color(0xFFFF4D6D), Color(0xFF27111B)),
                    ),
                ),
        ) {
            if (hero?.coverData != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = hero.title,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PosterFace(
                    title = "NOVA",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 142.dp)
                .width(82.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0B1A27), Color(0xFF1F8B8A), Color(0xFF7CF6D5)))),
        ) {
            PosterFace(
                title = "READ",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun PosterFace(title: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.7f)),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun NovaSpatialSectionHeader(
    section: NovaRail,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NovaVikiDimensions.MinimumTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 2.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(24.dp)
                .background(NovaVikiColors.Accent),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "// ${section.title.uppercase()}",
                color = NovaVikiColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${section.items.size} titres",
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "Afficher ${section.title}",
            tint = NovaVikiColors.TextSecondary,
        )
    }
}

@Composable
private fun NovaSpatialPosterCard(
    item: NovaVikiManga,
    onClick: () -> Unit,
) {
    val coverRequest = rememberHighQualityImageRequest(item.coverData)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "home_spatial_card_scale",
    )
    val readingState = when {
        item.remainingChapters > 1L -> "${item.remainingChapters} chapitres à lire"
        item.remainingChapters == 1L -> "1 chapitre à lire"
        item.progress != null -> "Lecture en cours"
        else -> "À jour"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.title}, $readingState"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.58f)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
                .background(Brush.linearGradient(item.colors))
                .border(
                    width = 1.dp,
                    color = NovaVikiColors.Outline.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(NovaVikiDimensions.CardCorner),
                )
                .novaNeonProgress(
                    progress = item.progress ?: 0f,
                    trackColor = Color.White.copy(alpha = 0.16f),
                    progressColor = NovaVikiColors.Accent,
                ),
        ) {
            if (item.coverData != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = null,
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
                            0f to Color.Black.copy(alpha = 0.04f),
                            1f to Color.Black.copy(alpha = 0.68f),
                        ),
                    ),
            )
            Text(
                text = item.tag,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            NovaChapterBadges(
                item = item,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp),
            )
            Text(
                text = readingState,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = item.title,
            color = NovaVikiColors.TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            color = NovaVikiColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NovaRailSection(
    section: NovaRail,
    onOpenManga: (Long) -> Unit,
    onOpenSection: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(NovaVikiColors.Background)
            .padding(top = 16.dp),
    ) {
        Text(
            text = "${section.title} >",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clickable(onClick = onOpenSection)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            maxLines = 1,
        )
        Spacer(Modifier.height(9.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = section.railItems,
                key = { item -> item.id },
                contentType = { "poster" },
            ) { item ->
                NovaPosterCard(
                    item = item,
                    onClick = {
                        if (item.id >= 0L) {
                            onOpenManga(item.id)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NovaInlineGridSection(
    section: NovaRail,
    onOpenManga: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .background(NovaVikiColors.Background)
            .padding(top = 18.dp),
    ) {
        Text(
            text = section.title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            section.items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { item ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            NovaPosterCard(
                                item = item,
                                onClick = {
                                    if (item.id >= 0L) {
                                        onOpenManga(item.id)
                                    }
                                },
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaRailListScreen(
    section: NovaRail,
    onBack: () -> Unit,
    onOpenManga: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaVikiColors.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(
                onClick = onBack,
                modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Retour",
                    tint = NovaVikiColors.TextPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    text = section.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    text = "${section.items.size} titles",
                    color = NovaVikiColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(123.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            gridItems(
                items = section.items,
                key = { item -> item.id },
                contentType = { "poster_grid" },
            ) { item ->
                Box(contentAlignment = Alignment.TopCenter) {
                    NovaPosterCard(
                        item = item,
                        onClick = {
                            if (item.id >= 0L) {
                                onOpenManga(item.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaPosterCard(
    item: NovaVikiManga,
    onClick: () -> Unit,
) {
    val coverRequest = rememberHighQualityImageRequest(item.coverData)
    Column(
        modifier = Modifier
            .width(123.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(5.dp))
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f)),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = item.tag,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
            NovaChapterBadges(
                item = item,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            val progress = item.progress
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.22f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(NovaVikiColors.Accent),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = item.subtitle,
            color = NovaVikiColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.title,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RatingBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Black.copy(alpha = 0.48f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Immutable
private data class NovaRail(
    val title: String,
    val items: List<NovaVikiManga>,
    val railItems: List<NovaVikiManga> = items.take(12),
    val inlineGrid: Boolean = false,
)

private val fallbackSections = listOf(
    NovaRail(
        title = "Continue Reading",
        items = listOf(
            NovaVikiManga(-1, "Your first manhwa", "Library empty", "NOVA", colors = listOf(Color(0xFF111B33), Color(0xFF235BB9), Color(0xFF58E3D2))),
        ),
    ),
)
