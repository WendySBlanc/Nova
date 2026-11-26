package eu.kanade.presentation.manga

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.SwapCalls
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.presentation.components.DownloadDropdownMenu
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.NovaAnimatedNumber
import eu.kanade.presentation.components.NovaCountBadge
import eu.kanade.presentation.components.NovaMenuItem
import eu.kanade.presentation.components.NovaSnackbarHost
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadIndicator
import eu.kanade.presentation.manga.components.getSwipeAction
import eu.kanade.presentation.manga.components.swipeActionThreshold
import eu.kanade.presentation.theme.NovaCoverGradientPalette
import eu.kanade.presentation.theme.NovaIcons
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaCoverGradientPalette
import eu.kanade.presentation.theme.novaSpatialBackdrop
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.presentation.util.isLikelyLightNovel
import eu.kanade.presentation.util.novaMangaSharedArtwork
import eu.kanade.presentation.util.rememberHighQualityImageRequest
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.ui.manga.PagePreviewState
import me.saket.swipe.SwipeableActionsBox
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovaMangaScreen(
    state: MangaScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    sourceName: String,
    useCoverGradient: Boolean,
    navigateUp: () -> Unit,
    onChapterClicked: (Chapter) -> Unit,
    onContinueReading: () -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onRefresh: () -> Unit,
    onFilterClicked: () -> Unit,
    onReverseChapterOrder: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onShareClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onMigrateClicked: (() -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,
    onSourceSettingsClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onTitleSearch: (String) -> Unit,
    onSourceSearch: (String) -> Unit,
    onTagSearch: (String) -> Unit,
    onMarkChaptersRead: (List<Chapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousChaptersRead: (Chapter) -> Unit,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onChapterSwipe: (ChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
    onCoverLoaded: (MangaCover) -> Unit,
) {
    val allChapters = state.chapters
    val chapters = remember(state) { state.processedChapters }
    var chapterQuery by rememberSaveable(state.manga.id) { mutableStateOf("") }
    val chapterSearch = remember(chapterQuery) { NovaChapterSearch(chapterQuery) }
    val indexedChapters = remember(chapters) { chapters.withIndex().toList() }
    val matchingChapters = remember(indexedChapters, chapterSearch) {
        if (chapterSearch.isActive) {
            indexedChapters.filter { (_, item) -> chapterSearch.matches(item.chapter.name, item.chapter.chapterNumber) }
        } else {
            indexedChapters
        }
    }
    val chapterPreviews = remember(state.pagePreviewsState) {
        (state.pagePreviewsState as? PagePreviewState.Success)?.pagePreviews.orEmpty()
    }
    val isLightNovel = remember(state.manga, sourceName) {
        state.manga.isLikelyLightNovel(sourceName)
    }
    val unreadCount = remember(allChapters) {
        allChapters.count { !it.chapter.read }
    }
    val downloadActionCounts = remember(allChapters) {
        val downloadable = allChapters.filter {
            it.downloadState == Download.State.NOT_DOWNLOADED ||
                it.downloadState == Download.State.ERROR
        }
        val unreadDownloadableCount = downloadable.count { !it.chapter.read }
        mapOf(
            DownloadAction.NEXT_1_CHAPTER to unreadDownloadableCount.coerceAtMost(1),
            DownloadAction.NEXT_5_CHAPTERS to unreadDownloadableCount.coerceAtMost(5),
            DownloadAction.UNREAD_CHAPTERS to unreadDownloadableCount,
            DownloadAction.ALL_CHAPTERS to downloadable.size,
        )
    }
    val background = NovaVikiColors.Background
    val fallbackAccent = NovaVikiColors.Accent
    val secondaryAccent = NovaVikiColors.AccentWarm
    val tertiaryAccent = NovaVikiColors.AccentGold
    val coverGradient = remember(
        state.seedColor,
        useCoverGradient,
        background,
        fallbackAccent,
        secondaryAccent,
        tertiaryAccent,
    ) {
        novaCoverGradientPalette(
            seedColor = state.seedColor.takeIf { useCoverGradient },
            background = background,
            fallbackAccent = fallbackAccent,
            secondaryAccent = secondaryAccent,
            tertiaryAccent = tertiaryAccent,
        )
    }
    var fabMenuExpanded by rememberSaveable(state.manga.id) { mutableStateOf(false) }
    BackHandler(enabled = fabMenuExpanded) {
        fabMenuExpanded = false
    }
    Scaffold(
        snackbarHost = { NovaSnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            NovaMangaFabMenu(
                expanded = fabMenuExpanded,
                onExpandedChange = { fabMenuExpanded = it },
                onWebViewClicked = onWebViewClicked,
                onTrackingClicked = onTrackingClicked,
                onEditCategoryClicked = onEditCategoryClicked,
                onMigrateClicked = onMigrateClicked,
                onEditNotesClicked = onEditNotesClicked,
                onSourceSettingsClicked = onSourceSettingsClicked,
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { contentPadding ->
        PullRefresh(
            refreshing = state.isRefreshingData,
            enabled = true,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .novaSpatialBackdrop(
                    background = coverGradient.background,
                    primary = coverGradient.primary,
                    secondary = coverGradient.secondary,
                    tertiary = coverGradient.tertiary,
                    wireColor = coverGradient.outline,
                    wireAlpha = 0.05f,
                    planeAlpha = 0.13f,
                    colorIntensity = 3.8f,
                ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = NovaVikiDimensions.WideContentMaxWidth)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 112.dp),
                ) {
                    item(contentType = "manga_hero") {
                        NovaMangaHero(
                            state = state,
                            sourceName = sourceName,
                            isLightNovel = isLightNovel,
                            navigateUp = navigateUp,
                            onContinueReading = onContinueReading,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onShareClicked = onShareClicked,
                            onTitleSearch = onTitleSearch,
                            onSourceSearch = onSourceSearch,
                            onTagSearch = onTagSearch,
                            coverGradient = coverGradient,
                            onCoverLoaded = onCoverLoaded,
                        )
                    }
                    stickyHeader(
                        key = "manga_tabs",
                        contentType = "manga_tabs",
                    ) {
                        Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                            NovaMangaTabs(
                                chapterCount = allChapters.size,
                                visibleChapterCount = matchingChapters.size,
                                unreadCount = unreadCount,
                                downloadActionCounts = downloadActionCounts,
                                filterActive = state.filterActive,
                                searchActive = chapterSearch.isActive,
                                onFilterClicked = onFilterClicked,
                                onDownloadActionClicked = onDownloadActionClicked,
                                onMarkAllReadClicked = {
                                    onMarkChaptersRead(allChapters.map { it.chapter }, true)
                                }.takeIf {
                                    allChapters.any { !it.chapter.read }
                                },
                                onMarkAllUnreadClicked = {
                                    onMarkChaptersRead(allChapters.map { it.chapter }, false)
                                }.takeIf {
                                    allChapters.any { it.chapter.read || it.chapter.lastPageRead > 0L }
                                },
                            )
                            if (allChapters.isNotEmpty()) {
                                NovaChapterLookup(
                                    query = chapterQuery,
                                    onQueryChange = { chapterQuery = it },
                                    newestFirst = state.manga.sortDescending(),
                                    onReverseOrder = onReverseChapterOrder,
                                )
                            }
                        }
                    }
                    if (matchingChapters.isEmpty()) {
                        item(
                            key = "empty_chapters",
                            contentType = "empty_chapters",
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (state.filterActive || chapterSearch.isActive) {
                                        "Aucun chapitre ne correspond aux filtres"
                                    } else {
                                        "Aucun chapitre disponible"
                                    },
                                    color = NovaVikiColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                    items(
                        items = matchingChapters,
                        key = { (_, item) -> item.id },
                        contentType = { "episode_row" },
                    ) { (index, item) ->
                        val preview = chapterPreviews.getOrNull(index)
                        NovaEpisodeRow(
                            manga = state.manga,
                            item = item,
                            preview = preview,
                            isLightNovel = isLightNovel,
                            onClick = { onChapterClicked(item.chapter) },
                            onDownloadChapter = onDownloadChapter,
                            onToggleRead = { markAsRead ->
                                onMarkChaptersRead(listOf(item.chapter), markAsRead)
                            },
                            onMarkPreviousRead = {
                                onMarkPreviousChaptersRead(item.chapter)
                            },
                            chapterSwipeStartAction = chapterSwipeStartAction,
                            chapterSwipeEndAction = chapterSwipeEndAction,
                            onChapterSwipe = { action -> onChapterSwipe(item, action) },
                            coverGradient = coverGradient,
                        )
                    }
                }
            }
        }
    }
}

private val NovaDetailReadColor = Color(0xFFC72442)

@Composable
private fun NovaMangaHero(
    state: MangaScreenModel.State.Success,
    sourceName: String,
    isLightNovel: Boolean,
    navigateUp: () -> Unit,
    onContinueReading: () -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onTitleSearch: (String) -> Unit,
    onSourceSearch: (String) -> Unit,
    onTagSearch: (String) -> Unit,
    coverGradient: NovaCoverGradientPalette,
    onCoverLoaded: (MangaCover) -> Unit,
) {
    val manga = state.manga
    val coverRequest = rememberHighQualityImageRequest(manga)
    var paletteRequested by remember(manga.id, manga.thumbnailUrl, manga.coverLastModified) {
        mutableStateOf(false)
    }
    val primaryReadAction = remember(state.chapters) {
        resolvePrimaryReadAction(
            totalChapterCount = state.chapters.size,
            unreadChapterCount = state.chapters.count { !it.chapter.read },
            hasPageProgress = state.chapters.any { it.chapter.lastPageRead > 0L },
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(coverGradient.primary.copy(alpha = 0.11f), Color.Transparent),
                ),
            ),
    ) {
        val wide = maxWidth >= 640.dp
        val posterWidth = if (wide) 208.dp else (maxWidth * 0.46f).coerceAtMost(196.dp)
        val poster: @Composable () -> Unit = {
            Box(
                modifier = Modifier.width(posterWidth),
            ) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    onSuccess = {
                        if (!paletteRequested) {
                            paletteRequested = true
                            onCoverLoaded(manga.asMangaCover())
                        }
                    },
                    modifier = Modifier
                        .novaMangaSharedArtwork(manga.id)
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NovaVikiColors.SurfaceStrong),
                )
                val status = when (manga.status) {
                    SManga.ONGOING.toLong() -> stringResource(MR.strings.ongoing)
                    SManga.COMPLETED.toLong() -> stringResource(MR.strings.completed)
                    else -> null
                }
                status?.let {
                    Text(
                        text = it,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xE818181A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        val details: @Composable (Modifier) -> Unit = { modifier ->
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally,
            ) {
                val alignment = if (wide) TextAlign.Start else TextAlign.Center
                Text(
                    text = if (isLightNovel) "LIGHT NOVEL" else "MANHWA",
                    color = if (coverGradient.background.luminance() < 0.35f) {
                        Color(0xFFFF7185)
                    } else {
                        NovaDetailReadColor
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = manga.title,
                    color = NovaVikiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = alignment,
                    modifier = Modifier
                        .semantics { heading() }
                        .clickable { onTitleSearch(manga.title) },
                )
                manga.author?.takeIf(String::isNotBlank)?.let { author ->
                    Text(
                        text = "Par $author",
                        color = NovaVikiColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = alignment,
                    )
                }
                if (sourceName.isNotBlank()) {
                    TextButton(onClick = { onSourceSearch(sourceName) }) {
                        Text(sourceName, textAlign = alignment, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                val genres = manga.genre.orEmpty()
                if (genres.isNotEmpty()) {
                    var showAll by rememberSaveable(manga.id) { mutableStateOf(false) }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            6.dp,
                            if (wide) Alignment.Start else Alignment.CenterHorizontally,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        (if (showAll) genres else genres.take(5)).forEach { genre ->
                            AssistChip(
                                onClick = { onTagSearch(genre) },
                                label = { Text(genre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                    if (genres.size > 5) {
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(if (showAll) "Afficher moins" else "Afficher plus")
                        }
                    }
                }
                NovaMangaSynopsis(
                    description = manga.description.orEmpty(),
                    mangaId = manga.id,
                    textAlign = alignment,
                )
                if (state.chapters.isNotEmpty()) {
                    Text(
                        text = NumberFormat.getIntegerInstance().format(state.chapters.size) + " chapitres",
                        color = NovaVikiColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onContinueReading,
                        enabled = state.chapters.isNotEmpty(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NovaDetailReadColor,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (primaryReadAction) {
                                NovaPrimaryReadAction.Read -> "Commencer la lecture"
                                NovaPrimaryReadAction.Resume -> "Reprendre la lecture"
                                NovaPrimaryReadAction.Reread -> "Relire"
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FilledTonalIconButton(onClick = onAddToLibraryClicked, shape = RoundedCornerShape(8.dp)) {
                        Icon(
                            imageVector = if (manga.favorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                            contentDescription = if (manga.favorite) "Retirer de la bibliothèque" else "Ajouter à la bibliothèque",
                        )
                    }
                    onShareClicked?.let { share ->
                        FilledTonalIconButton(onClick = share, shape = RoundedCornerShape(8.dp)) {
                            Icon(NovaIcons.Share, contentDescription = stringResource(MR.strings.action_share))
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            IconButton(onClick = navigateUp) {
                Icon(NovaIcons.Back, contentDescription = "Retour")
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    poster()
                    details(Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    poster()
                    Spacer(Modifier.height(18.dp))
                    details(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun NovaMangaSynopsis(
    description: String,
    mangaId: Long,
    textAlign: TextAlign,
) {
    if (description.isBlank()) return

    var expanded by rememberSaveable(mangaId) { mutableStateOf(false) }
    var canExpand by remember(mangaId, description) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
        horizontalAlignment = if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = description,
            color = NovaVikiColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult ->
                if (!expanded) canExpand = layoutResult.hasVisualOverflow
            },
        )
        if (expanded || canExpand) {
            val arrowAngle by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                label = "synopsis-chevron",
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Réduire" else "Lire la suite")
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = arrowAngle },
                )
            }
        }
    }
}

@Composable
private fun NovaMangaFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onEditCategoryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,
    onSourceSettingsClicked: (() -> Unit)?,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    PlainTooltip {
                        Text("Actions de l'œuvre")
                    }
                },
                state = rememberTooltipState(),
                focusable = false,
            ) {
                ToggleFloatingActionButton(
                    checked = expanded,
                    onCheckedChange = onExpandedChange,
                    modifier = Modifier.semantics {
                        contentDescription = "Actions de l'œuvre"
                        stateDescription = if (expanded) "Menu ouvert" else "Menu fermé"
                    },
                ) {
                    Icon(
                        imageVector = if (expanded) NovaIcons.Close else NovaIcons.More,
                        contentDescription = null,
                        modifier = with(ToggleFloatingActionButtonDefaults) {
                            Modifier.animateIcon(checkedProgress = { checkedProgress })
                        },
                    )
                }
            }
        },
    ) {
        onWebViewClicked?.let { action ->
            NovaMangaFabMenuItem(
                label = stringResource(MR.strings.action_open_in_web_view),
                icon = NovaIcons.Web,
                action = action,
                dismiss = { onExpandedChange(false) },
            )
        }
        NovaMangaFabMenuItem(
            label = "Suivi",
            icon = Icons.Outlined.Sync,
            action = onTrackingClicked,
            dismiss = { onExpandedChange(false) },
        )
        onEditCategoryClicked?.let { action ->
            NovaMangaFabMenuItem(
                label = "Catégories",
                icon = Icons.AutoMirrored.Outlined.Label,
                action = action,
                dismiss = { onExpandedChange(false) },
            )
        }
        onMigrateClicked?.let { action ->
            NovaMangaFabMenuItem(
                label = stringResource(MR.strings.action_migrate),
                icon = Icons.Outlined.SwapCalls,
                action = action,
                dismiss = { onExpandedChange(false) },
            )
        }
        NovaMangaFabMenuItem(
            label = stringResource(MR.strings.action_edit_notes),
            icon = Icons.Filled.EditNote,
            action = onEditNotesClicked,
            dismiss = { onExpandedChange(false) },
        )
        onSourceSettingsClicked?.let { action ->
            NovaMangaFabMenuItem(
                label = stringResource(MR.strings.source_settings),
                icon = NovaIcons.Settings,
                action = action,
                dismiss = { onExpandedChange(false) },
            )
        }
    }
}

@Composable
private fun FloatingActionButtonMenuScope.NovaMangaFabMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: () -> Unit,
    dismiss: () -> Unit,
) {
    FloatingActionButtonMenuItem(
        text = { Text(label) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        onClick = {
            dismiss()
            action()
        },
    )
}

@Composable
private fun NovaChapterLookup(
    query: String,
    onQueryChange: (String) -> Unit,
    newestFirst: Boolean,
    onReverseOrder: () -> Unit,
) {
    val orderLabel = if (newestFirst) "Du dernier" else "Du premier"
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
    ) {
        val compact = maxWidth < 330.dp
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Rechercher un chapitre" },
                singleLine = true,
                placeholder = { Text("N° ou titre", maxLines = 1) },
                leadingIcon = { Icon(NovaIcons.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(NovaIcons.Close, contentDescription = "Effacer la recherche")
                        }
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
            if (compact) {
                FilledTonalIconButton(
                    onClick = onReverseOrder,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = "Inverser l'ordre : $orderLabel")
                }
            } else {
                FilledTonalButton(onClick = onReverseOrder, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(orderLabel, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun NovaMangaTabs(
    chapterCount: Int,
    visibleChapterCount: Int,
    unreadCount: Int,
    downloadActionCounts: Map<DownloadAction, Int>,
    filterActive: Boolean,
    searchActive: Boolean,
    onFilterClicked: () -> Unit,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onMarkAllReadClicked: (() -> Unit)?,
    onMarkAllUnreadClicked: (() -> Unit)?,
) {
    var downloadExpanded by remember { mutableStateOf(false) }
    var readStatusExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Chapitres",
                    color = NovaVikiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                NovaCountBadge(
                    value = chapterCount,
                    contentDescription = "$chapterCount chapitres",
                )
            }
            NovaChapterSummary(
                chapterCount = chapterCount,
                visibleChapterCount = visibleChapterCount,
                unreadCount = unreadCount,
                filterActive = filterActive || searchActive,
            )
        }
        if (onDownloadActionClicked != null) {
            Box {
                DownloadDropdownMenu(
                    expanded = downloadExpanded,
                    onDismissRequest = { downloadExpanded = false },
                    onDownloadClicked = onDownloadActionClicked,
                    actionCounts = downloadActionCounts,
                )
                FilledTonalIconButton(
                    onClick = { downloadExpanded = true },
                    modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = NovaIcons.Download,
                        contentDescription = "Télécharger des chapitres",
                    )
                }
            }
        }
        if (onMarkAllReadClicked != null || onMarkAllUnreadClicked != null) {
            Box {
                DropdownMenu(
                    expanded = readStatusExpanded,
                    onDismissRequest = { readStatusExpanded = false },
                ) {
                    if (onMarkAllReadClicked != null) {
                        NovaMenuItem(
                            label = "Tout marquer comme lu",
                            icon = Icons.Outlined.DoneAll,
                            onClick = {
                                readStatusExpanded = false
                                onMarkAllReadClicked()
                            },
                        )
                    }
                    if (onMarkAllUnreadClicked != null) {
                        NovaMenuItem(
                            label = "Tout marquer comme non lu",
                            icon = Icons.Outlined.RemoveDone,
                            onClick = {
                                readStatusExpanded = false
                                onMarkAllUnreadClicked()
                            },
                        )
                    }
                }
                IconButton(
                    onClick = { readStatusExpanded = true },
                    modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
                ) {
                    Icon(
                        imageVector = NovaIcons.More,
                        contentDescription = "Modifier l'état de lecture",
                        tint = NovaVikiColors.TextPrimary,
                    )
                }
            }
        }
        IconButton(
            onClick = onFilterClicked,
            modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (filterActive) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
                contentColor = if (filterActive) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    NovaVikiColors.TextPrimary
                },
            ),
        ) {
            Icon(
                imageVector = NovaIcons.Filter,
                contentDescription = "Filtrer les chapitres",
            )
        }
    }
}

@Composable
private fun NovaChapterSummary(
    chapterCount: Int,
    visibleChapterCount: Int,
    unreadCount: Int,
    filterActive: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (filterActive) {
            NovaAnimatedNumber(
                value = visibleChapterCount,
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                contentDescription = visibleChapterCount.toString(),
            )
            Text(
                text = "sur",
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            NovaAnimatedNumber(
                value = chapterCount,
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                contentDescription = chapterCount.toString(),
            )
            Text(
                text = "affichés",
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        } else if (unreadCount > 0) {
            NovaAnimatedNumber(
                value = unreadCount,
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                contentDescription = unreadCount.toString(),
            )
            Text(
                text = "à lire",
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        } else {
            Text(
                text = "Tout est lu",
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NovaEpisodeRow(
    manga: Manga,
    item: ChapterList.Item,
    preview: PagePreview?,
    isLightNovel: Boolean,
    onClick: () -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onToggleRead: (Boolean) -> Unit,
    onMarkPreviousRead: () -> Unit,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onChapterSwipe: (LibraryPreferences.ChapterSwipeAction) -> Unit,
    coverGradient: NovaCoverGradientPalette,
) {
    var chapterMenuExpanded by remember { mutableStateOf(false) }
    val title = if (manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER) {
        stringResource(MR.strings.display_mode_chapter, formatChapterNumber(item.chapter.chapterNumber))
    } else {
        item.chapter.name
    }
    val previewRequest = rememberHighQualityImageRequest(preview ?: manga)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        val compact = maxWidth < 380.dp
        val previewWidth = when {
            isLightNovel -> 56.dp
            compact -> (maxWidth * 0.25f).coerceIn(82.dp, 96.dp)
            maxWidth >= 600.dp -> (maxWidth * 0.18f).coerceIn(112.dp, 136.dp)
            else -> 118.dp
        }
        val previewHeight = if (isLightNovel) 48.dp else previewWidth * 0.58f
        val readingState = resolveChapterReadingState(
            isRead = item.chapter.read,
            lastPageRead = item.chapter.lastPageRead,
        )
        val isInProgress = readingState == NovaChapterReadingState.InProgress
        val remainingFraction = if (isInProgress && item.pageCount > 0) {
            val pagesRead = (item.chapter.lastPageRead + 1L)
                .coerceAtMost(item.pageCount.toLong())
            ((item.pageCount - pagesRead).toFloat() / item.pageCount).coerceIn(0f, 1f)
        } else {
            null
        }
        val remainingPercent = remainingFraction?.let { (it * 100f).toInt() }
        val grayscaleFilter = remember {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        }
        val readStateColor = when (readingState) {
            NovaChapterReadingState.Read -> NovaVikiColors.Success
            NovaChapterReadingState.InProgress -> NovaVikiColors.AccentGold
            NovaChapterReadingState.Unread -> NovaVikiColors.Accent
        }
        val readStateLabel = when (readingState) {
            NovaChapterReadingState.Read -> "Lu"
            NovaChapterReadingState.InProgress -> "En cours"
            NovaChapterReadingState.Unread -> "\u00c0 lire"
        }
        val metadata = listOfNotNull(
            item.chapter.dateUpload.takeIf { it > 0L }?.let { relativeDateText(it) },
            item.chapter.scanlator,
            remainingPercent?.let { "$it% restant" }
                ?: item.chapter.lastPageRead
                    .takeIf { !item.chapter.read && it > 0L }
                    ?.let { "Page ${it + 1}" },
        ).joinToString(" | ")
        val swipeBackground = MaterialTheme.colorScheme.primaryContainer
        val swipeStart = remember(
            chapterSwipeStartAction,
            item.chapter.read,
            item.chapter.bookmark,
            item.downloadState,
        ) {
            getSwipeAction(
                action = chapterSwipeStartAction,
                read = item.chapter.read,
                bookmark = item.chapter.bookmark,
                downloadState = item.downloadState,
                background = swipeBackground,
                onSwipe = { onChapterSwipe(chapterSwipeStartAction) },
            )
        }
        val swipeEnd = remember(
            chapterSwipeEndAction,
            item.chapter.read,
            item.chapter.bookmark,
            item.downloadState,
        ) {
            getSwipeAction(
                action = chapterSwipeEndAction,
                read = item.chapter.read,
                bookmark = item.chapter.bookmark,
                downloadState = item.downloadState,
                background = swipeBackground,
                onSwipe = { onChapterSwipe(chapterSwipeEndAction) },
            )
        }
        SwipeableActionsBox(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            startActions = listOfNotNull(swipeStart),
            endActions = listOfNotNull(swipeEnd),
            swipeThreshold = swipeActionThreshold,
            backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { chapterMenuExpanded = true },
                    )
                    .semantics { stateDescription = readStateLabel }
                    .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(previewWidth)
                        .height(previewHeight)
                        .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
                        .background(NovaVikiColors.SurfaceStrong),
                ) {
                    if (isLightNovel) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item.chapter.chapterNumber
                                    .takeIf { it >= 0.0 }
                                    ?.let(::formatChapterNumber)
                                    ?: "TXT",
                                color = NovaVikiColors.TextPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        AsyncImage(
                            model = previewRequest,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            colorFilter = if (item.chapter.read) grayscaleFilter else null,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (remainingFraction != null && remainingFraction > 0f) {
                            AsyncImage(
                                model = previewRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = grayscaleFilter,
                                filterQuality = FilterQuality.High,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RemainingProgressShape(remainingFraction)),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                                    ),
                                ),
                        )
                        if (!item.chapter.read) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(if (compact) 26.dp else 32.dp),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (item.chapter.read) NovaVikiColors.TextSecondary else NovaVikiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.chapter.read) FontWeight.Medium else FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NovaChapterReadStatus(
                            isRead = item.chapter.read,
                            label = readStateLabel,
                            color = readStateColor,
                        )
                        if (metadata.isNotBlank()) {
                            Text(
                                text = metadata,
                                color = NovaVikiColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (onDownloadChapter != null) {
                    ChapterDownloadIndicator(
                        enabled = true,
                        downloadStateProvider = { item.downloadState },
                        downloadProgressProvider = { item.downloadProgress },
                        onClick = { action -> onDownloadChapter(listOf(item), action) },
                    )
                }
                Box {
                    DropdownMenu(
                        expanded = chapterMenuExpanded,
                        onDismissRequest = { chapterMenuExpanded = false },
                    ) {
                        NovaMenuItem(
                            label = if (item.chapter.read) {
                                "Marquer comme non lu"
                            } else {
                                "Marquer comme lu"
                            },
                            icon = if (item.chapter.read) {
                                Icons.Outlined.RemoveDone
                            } else {
                                Icons.Outlined.Done
                            },
                            onClick = {
                                chapterMenuExpanded = false
                                onToggleRead(!item.chapter.read)
                            },
                        )
                        NovaMenuItem(
                            label = "Marquer les chapitres précédents comme lus",
                            icon = Icons.Outlined.DoneAll,
                            onClick = {
                                chapterMenuExpanded = false
                                onMarkPreviousRead()
                            },
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .height(1.dp)
                .background(coverGradient.outline.copy(alpha = 0.28f)),
        )
    }
}

private data class RemainingProgressShape(
    val remainingFraction: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val left = size.width * (1f - remainingFraction.coerceIn(0f, 1f))
        return Outline.Rectangle(Rect(left, 0f, size.width, size.height))
    }
}

@Composable
private fun NovaChapterReadStatus(
    isRead: Boolean,
    label: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isRead) {
            Icon(
                imageVector = Icons.Outlined.Done,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
