package eu.kanade.presentation.library.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.NovaFluidFixedTabRow
import eu.kanade.presentation.components.NovaToolbarIconButton
import eu.kanade.presentation.home.LocalNovaDockBottomInset
import eu.kanade.presentation.home.NovaHomeRow
import eu.kanade.presentation.home.NovaHomeSections
import eu.kanade.presentation.home.novaInkEntrance
import eu.kanade.presentation.home.novaInkHold
import eu.kanade.presentation.library.NovaLibraryEntry
import eu.kanade.presentation.theme.NovaIcons
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaGlassSurface
import eu.kanade.presentation.theme.novaNeonProgress
import eu.kanade.presentation.theme.novaReducedMotion
import eu.kanade.presentation.util.LocalNovaAnimatedVisibilityScope
import eu.kanade.presentation.util.LocalNovaSharedTransitionState
import eu.kanade.presentation.util.LocalNovaTransitionScreenKey
import eu.kanade.presentation.util.novaMangaSharedArtwork
import eu.kanade.presentation.util.rememberHighQualityImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource

typealias NovaVikiManga = NovaLibraryEntry

@Composable
fun NovaVikiHomeScreen(
    sections: NovaHomeSections,
    autoAdvance: Boolean,
    showUpdates: Boolean,
    showRecent: Boolean,
    recentFirst: Boolean,
    libraryReady: Boolean,
    isRefreshing: Boolean,
    onSearch: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onResumeManga: (Long) -> Unit,
    onRefresh: () -> Unit,
    resetToTopKey: Int,
) {
    val listState = rememberLazyListState()
    val dockInset = LocalNovaDockBottomInset.current
    val heroItems = remember(sections) { sections.featuredItems() }
    var selectedRow by remember { mutableStateOf<NovaHomeRow?>(null) }
    val visibleRows = remember(sections, showUpdates, showRecent, recentFirst) {
        sections.visibleRows(showUpdates, showRecent, recentFirst)
    }

    LaunchedEffect(resetToTopKey) {
        if (resetToTopKey > 0) {
            selectedRow = null
            listState.scrollToItem(0)
        }
    }

    val sharedTransition = LocalNovaSharedTransitionState.current
    val sourceScreenKey = LocalNovaTransitionScreenKey.current
    val screenVisible = LocalNovaAnimatedVisibilityScope.current?.transition?.targetState != EnterExitState.PostExit
    val activeRail = selectedRow?.takeIf { row -> visibleRows.any { it.first == row } }
    val motionScheme = MaterialTheme.motionScheme
    val reducedMotion = novaReducedMotion()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    fun openWithoutHero(mangaId: Long) {
        sharedTransition?.clearSelection()
        onOpenManga(mangaId)
    }

    BackHandler(enabled = activeRail != null) { selectedRow = null }

    val collections = updateTransition(activeRail, label = "home-collection")
    collections.AnimatedContent(
        modifier = Modifier.fillMaxSize().clipToBounds(),
        transitionSpec = {
            val effects = motionScheme.fastEffectsSpec<Float>()
            if (reducedMotion) {
                fadeIn(effects) togetherWith fadeOut(effects)
            } else {
                novaInkHold()
            }
        },
    ) { rail ->
        val fromLeft = (collections.targetState == null) != rtl
        val reveal = if (reducedMotion) Modifier else Modifier.novaInkEntrance(this, fromLeft)
        if (rail != null) {
            val entries = visibleRows.firstOrNull { it.first == rail }?.second
            val lastEntries = remember(rail) { mutableStateOf(entries.orEmpty()) }
            SideEffect {
                if (entries != null) lastEntries.value = entries
            }
            NovaRailListScreen(
                section = NovaRail(title = stringResource(rail.titleRes), items = entries ?: lastEntries.value),
                onBack = { selectedRow = null },
                onOpenManga = ::openWithoutHero,
                modifier = reveal,
            )
            return@AnimatedContent
        }
        Column(
            modifier = reveal.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) {
            NovaEditorialHomeHeader(onSearch = onSearch)
            if (!libraryReady) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
                return@Column
            }
            PullRefresh(
                refreshing = isRefreshing,
                enabled = true,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp + dockInset),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "home_resume_stage", contentType = "resume_stage") {
                        NovaRevResumeCarousel(
                            items = heroItems,
                            autoAdvanceEnabled = autoAdvance && activeRail == null && screenVisible,
                            onOpenManga = { mangaId ->
                                heroItems.firstOrNull { it.id == mangaId }?.let { entry ->
                                    sharedTransition?.selectManga(entry.id, entry.coverData, entry.title, sourceScreenKey)
                                }
                                onOpenManga(mangaId)
                            },
                            onResumeManga = onResumeManga,
                            onSearch = onSearch,
                        )
                    }
                    items(
                        items = visibleRows,
                        key = { (row, _) -> row.name },
                        contentType = { "home_row" },
                    ) { (row, entries) ->
                        NovaRailSection(
                            section = NovaRail(title = stringResource(row.titleRes), items = entries),
                            onOpenManga = ::openWithoutHero,
                            onOpenSection = { selectedRow = row },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaEditorialHomeHeader(
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(MR.strings.label_home_nova),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        NovaToolbarIconButton(NovaIcons.Search, stringResource(MR.strings.action_search), onSearch)
    }
}

private val NovaHomeRow.titleRes
    get() = when (this) {
        NovaHomeRow.NewChapters -> MR.strings.nova_home_new_chapters
        NovaHomeRow.RecentlyAdded -> MR.strings.nova_home_recently_added
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
                    imageVector = NovaIcons.Search,
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
private fun NovaRailSection(
    section: NovaRail,
    onOpenManga: (Long) -> Unit,
    onOpenSection: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(MR.strings.nova_home_see_all, section.title),
                    onClick = onOpenSection,
                )
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Icon(NovaIcons.Next, contentDescription = null)
        }
        BoxWithConstraints {
            val posterWidth = (maxWidth / 3.3f).coerceIn(88.dp, 154.dp)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(section.items, key = { it.id }, contentType = { "poster" }) { item ->
                    NovaHomePoster(
                        item = item,
                        modifier = Modifier.width(posterWidth),
                        onClick = { onOpenManga(item.id) },
                    )
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
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(section.title, modifier = Modifier.semantics { heading() }) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(NovaIcons.Back, stringResource(MR.strings.nova_home_back))
                }
            },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(112.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 20.dp + LocalNovaDockBottomInset.current,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            gridItems(section.items, key = { it.id }, contentType = { "poster_grid" }) { item ->
                NovaHomePoster(
                    item = item,
                    onClick = { onOpenManga(item.id) },
                )
            }
        }
    }
}

@Composable
private fun NovaHomePoster(
    item: NovaVikiManga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverRequest = rememberHighQualityImageRequest(item.coverData)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            AsyncImage(
                model = coverRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            NovaChapterBadges(
                item = item,
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            minLines = 2,
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
)
