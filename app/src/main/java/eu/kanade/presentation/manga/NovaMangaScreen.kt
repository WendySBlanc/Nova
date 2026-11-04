package eu.kanade.presentation.manga

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.presentation.components.DownloadDropdownMenu
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadIndicator
import eu.kanade.presentation.manga.components.getSwipeAction
import eu.kanade.presentation.manga.components.swipeActionThreshold
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaGlassSurface
import eu.kanade.presentation.theme.novaSpatialBackdrop
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.presentation.util.isLikelyLightNovel
import eu.kanade.presentation.util.novaMangaSharedArtwork
import eu.kanade.presentation.util.rememberHighQualityImageRequest
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.ui.manga.PagePreviewState
import me.saket.swipe.SwipeableActionsBox
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovaMangaScreen(
    state: MangaScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    sourceName: String,
    navigateUp: () -> Unit,
    onChapterClicked: (Chapter) -> Unit,
    onContinueReading: () -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onRefresh: () -> Unit,
    onFilterClicked: () -> Unit,
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
    onMarkChaptersRead: (List<Chapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousChaptersRead: (Chapter) -> Unit,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onChapterSwipe: (ChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
) {
    val allChapters = state.chapters
    val chapters = remember(state) { state.processedChapters }
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
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { contentPadding ->
        PullRefresh(
            refreshing = state.isRefreshingData,
            enabled = true,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .novaSpatialBackdrop(
                    background = NovaVikiColors.Background,
                    primary = NovaVikiColors.Accent,
                    secondary = NovaVikiColors.AccentWarm,
                    tertiary = NovaVikiColors.AccentGold,
                    wireColor = NovaVikiColors.OutlineStrong,
                    wireAlpha = 0.05f,
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
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 20.dp),
                ) {
                    item(contentType = "manga_hero") {
                        NovaMangaHero(
                            state = state,
                            sourceName = sourceName,
                            isLightNovel = isLightNovel,
                            navigateUp = navigateUp,
                            onContinueReading = onContinueReading,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onWebViewClicked = onWebViewClicked,
                            onShareClicked = onShareClicked,
                            onTrackingClicked = onTrackingClicked,
                            onMigrateClicked = onMigrateClicked,
                            onEditCategoryClicked = onEditCategoryClicked,
                            onEditNotesClicked = onEditNotesClicked,
                            onSourceSettingsClicked = onSourceSettingsClicked,
                            onTitleSearch = onTitleSearch,
                            onSourceSearch = onSourceSearch,
                        )
                    }
                    stickyHeader(
                        key = "manga_tabs",
                        contentType = "manga_tabs",
                    ) {
                        NovaMangaTabs(
                            chapterCount = allChapters.size,
                            visibleChapterCount = chapters.size,
                            unreadCount = unreadCount,
                            downloadActionCounts = downloadActionCounts,
                            filterActive = state.filterActive,
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
                    }
                    if (chapters.isEmpty()) {
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
                                    text = if (state.filterActive) {
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
                    itemsIndexed(
                        items = chapters,
                        key = { _, item -> item.id },
                        contentType = { _, _ -> "episode_row" },
                    ) { index, item ->
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaMangaHero(
    state: MangaScreenModel.State.Success,
    sourceName: String,
    isLightNovel: Boolean,
    navigateUp: () -> Unit,
    onContinueReading: () -> Unit,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onShareClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onMigrateClicked: (() -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,
    onSourceSettingsClicked: (() -> Unit)?,
    onTitleSearch: (String) -> Unit,
    onSourceSearch: (String) -> Unit,
) {
    val manga = state.manga
    val coverRequest = rememberHighQualityImageRequest(manga)
    val hasStartedReading = remember(state.chapters) {
        state.chapters.any { item ->
            item.chapter.read || item.chapter.lastPageRead > 0L
        }
    }
    val primaryReadAction = remember(state.chapters) {
        resolvePrimaryReadAction(
            totalChapterCount = state.chapters.size,
            unreadChapterCount = state.chapters.count { !it.chapter.read },
            hasPageProgress = hasStartedReading,
        )
    }
    var moreActionsExpanded by remember { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val compact = maxWidth < 360.dp
        val tablet = maxWidth >= 600.dp
        val showPoster = true
        val heroHeight = when {
            compact -> (maxWidth * 1.08f).coerceIn(360.dp, 414.dp)
            tablet -> (maxWidth * 0.62f).coerceIn(460.dp, 520.dp)
            else -> (maxWidth * 1.12f).coerceIn(414.dp, 462.dp)
        }
        val backdropHeight = when {
            compact -> heroHeight * 0.67f
            tablet -> heroHeight * 0.69f
            else -> heroHeight * 0.69f
        }
        val posterWidth = when {
            compact -> (maxWidth * 0.22f).coerceIn(70.dp, 82.dp)
            tablet -> (maxWidth * 0.16f).coerceIn(108.dp, 126.dp)
            else -> (maxWidth * 0.25f).coerceIn(88.dp, 98.dp)
        }
        val posterHeight = posterWidth * 1.5f
        val topBarHeight = if (compact) 50.dp else 56.dp
        val topButtonSize = NovaVikiDimensions.MinimumTouchTarget
        val topButtonModifier = Modifier
            .size(topButtonSize)
            .clip(CircleShape)
            .background(NovaVikiColors.Surface.copy(alpha = 0.78f))
            .border(1.dp, NovaVikiColors.OutlineStrong.copy(alpha = 0.8f), CircleShape)
        val titleStyle = if (tablet) {
            MaterialTheme.typography.headlineMedium
        } else {
            MaterialTheme.typography.headlineSmall
        }
        val infoMaxLines = if (compact) 1 else 2
        val descriptionMaxLines = if (compact) 2 else 3

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(NovaVikiColors.Background),
        ) {
            AsyncImage(
                model = coverRequest,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .novaMangaSharedArtwork(manga.id)
                    .fillMaxWidth()
                    .height(backdropHeight),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backdropHeight)
                    .background(
                        Brush.horizontalGradient(
                            0f to NovaVikiColors.Scrim.copy(alpha = 0.82f),
                            0.58f to NovaVikiColors.Scrim.copy(alpha = 0.24f),
                            1f to Color.Transparent,
                        ),
                    )
                    .background(
                        Brush.verticalGradient(
                            0f to NovaVikiColors.Scrim.copy(alpha = 0.16f),
                            0.48f to NovaVikiColors.Scrim.copy(alpha = 0.28f),
                            0.82f to NovaVikiColors.Background.copy(alpha = 0.82f),
                            1f to NovaVikiColors.Background,
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(topBarHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = navigateUp,
                    modifier = topButtonModifier,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                onWebViewClicked?.let { openWebView ->
                    IconButton(
                        onClick = openWebView,
                        modifier = topButtonModifier,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = stringResource(MR.strings.action_open_in_web_view),
                            tint = Color.White,
                        )
                    }
                }
                onShareClicked?.let { share ->
                    IconButton(
                        onClick = share,
                        modifier = topButtonModifier,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(MR.strings.action_share),
                            tint = Color.White,
                        )
                    }
                }
                Box {
                    DropdownMenu(
                        expanded = moreActionsExpanded,
                        onDismissRequest = { moreActionsExpanded = false },
                    ) {
                        NovaMangaActionItem("Suivi", onTrackingClicked) { moreActionsExpanded = false }
                        onEditCategoryClicked?.let { action ->
                            NovaMangaActionItem("Catégories", action) { moreActionsExpanded = false }
                        }
                        onMigrateClicked?.let { action ->
                            NovaMangaActionItem(stringResource(MR.strings.action_migrate), action) {
                                moreActionsExpanded = false
                            }
                        }
                        NovaMangaActionItem(stringResource(MR.strings.action_edit_notes), onEditNotesClicked) {
                            moreActionsExpanded = false
                        }
                        onSourceSettingsClicked?.let { action ->
                            NovaMangaActionItem(stringResource(MR.strings.source_settings), action) {
                                moreActionsExpanded = false
                            }
                        }
                    }
                    IconButton(
                        onClick = { moreActionsExpanded = true },
                        modifier = topButtonModifier,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Plus d'actions",
                            tint = Color.White,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = if (compact) 12.dp else 16.dp)
                    .novaGlassSurface(
                        color = NovaVikiColors.SurfaceRaised,
                        outline = NovaVikiColors.OutlineStrong,
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            ) {
                if (showPoster) {
                    Box(
                        modifier = Modifier
                            .width(posterWidth)
                            .height(posterHeight)
                            .clip(RoundedCornerShape(NovaVikiDimensions.ControlCorner))
                            .background(NovaVikiColors.SurfaceStrong)
                            .border(
                                width = 1.dp,
                                color = NovaVikiColors.OutlineStrong,
                                shape = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
                            ),
                    ) {
                        AsyncImage(
                            model = coverRequest,
                            contentDescription = manga.title,
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .align(Alignment.CenterEnd)
                                .background(Color.White.copy(alpha = 0.18f)),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (isLightNovel) "LIGHT NOVEL" else "MANHWA",
                        color = NovaVikiColors.Accent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Text(
                        text = manga.title,
                        color = Color.White,
                        style = titleStyle,
                        fontWeight = FontWeight.Black,
                        maxLines = if (compact) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onTitleSearch(manga.title) },
                    )
                    Spacer(Modifier.height(6.dp))
                    NovaMangaInfoLine(
                        sourceName = sourceName,
                        episodeCount = state.chapters.size,
                        author = manga.author,
                        maxLines = infoMaxLines,
                        onSourceSearch = onSourceSearch,
                    )
                    Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
                    Text(
                        text = manga.description.orEmpty().ifBlank { "Description indisponible" },
                        color = NovaVikiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = descriptionMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(if (compact) 10.dp else 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onContinueReading,
                            shape = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
                            modifier = Modifier
                                .weight(1f)
                                .height(NovaVikiDimensions.MinimumTouchTarget),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            AnimatedContent(
                                targetState = primaryReadAction,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "primary-read-action",
                            ) { action ->
                                Text(
                                    text = when (action) {
                                        NovaPrimaryReadAction.Read -> "Lire"
                                        NovaPrimaryReadAction.Resume -> "Reprendre"
                                        NovaPrimaryReadAction.Reread -> "Relire"
                                    },
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .animateContentSize(),
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onAddToLibraryClicked,
                            shape = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
                            modifier = Modifier
                                .weight(1f)
                                .height(NovaVikiDimensions.MinimumTouchTarget),
                        ) {
                            Icon(
                                imageVector = if (manga.favorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                                contentDescription = null,
                                tint = NovaVikiColors.TextPrimary,
                            )
                            Text(
                                text = if (manga.favorite) "Ajoute" else "Ajouter",
                                modifier = Modifier.padding(start = 6.dp),
                                color = NovaVikiColors.TextPrimary,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaMangaActionItem(
    label: String,
    action: () -> Unit,
    dismiss: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            dismiss()
            action()
        },
    )
}

@Composable
private fun NovaMangaInfoLine(
    sourceName: String,
    episodeCount: Int,
    author: String?,
    maxLines: Int,
    onSourceSearch: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = sourceName,
            color = NovaVikiColors.Accent,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable { onSourceSearch(sourceName) }
                .weight(0.36f, fill = false),
        )
        DotSeparator()
        Text(
            text = "$episodeCount chapitres",
            color = NovaVikiColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        author?.takeIf { it.isNotBlank() }?.let {
            DotSeparator()
            Text(
                text = it,
                color = NovaVikiColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DotSeparator() {
    Text(
        text = "|",
        color = NovaVikiColors.TextSecondary,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
    )
}

@Composable
private fun NovaMangaTabs(
    chapterCount: Int,
    visibleChapterCount: Int,
    unreadCount: Int,
    downloadActionCounts: Map<DownloadAction, Int>,
    filterActive: Boolean,
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
            .height(64.dp)
            .novaSpatialBackdrop(
                background = NovaVikiColors.SurfaceRaised,
                primary = NovaVikiColors.Accent,
                secondary = NovaVikiColors.AccentWarm,
                tertiary = NovaVikiColors.AccentGold,
                wireColor = NovaVikiColors.OutlineStrong,
                wireAlpha = 0.08f,
            )
            .border(
                width = 1.dp,
                color = NovaVikiColors.Outline,
                shape = RoundedCornerShape(0.dp),
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chapitres",
                color = NovaVikiColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = if (filterActive) {
                    "$visibleChapterCount affichés sur $chapterCount"
                } else {
                    "$chapterCount au total | $unreadCount à lire"
                },
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                IconButton(
                    onClick = { downloadExpanded = true },
                    modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Télécharger des chapitres",
                        tint = NovaVikiColors.TextPrimary,
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
                        DropdownMenuItem(
                            text = { Text("Tout marquer comme lu") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DoneAll,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                readStatusExpanded = false
                                onMarkAllReadClicked()
                            },
                        )
                    }
                    if (onMarkAllUnreadClicked != null) {
                        DropdownMenuItem(
                            text = { Text("Tout marquer comme non lu") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.RemoveDone,
                                    contentDescription = null,
                                )
                            },
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
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = "Modifier l'état de lecture",
                        tint = NovaVikiColors.TextPrimary,
                    )
                }
            }
        }
        IconButton(
            onClick = onFilterClicked,
            modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = "Filtrer les chapitres",
                tint = if (filterActive) NovaVikiColors.Accent else NovaVikiColors.TextPrimary,
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
            .background(NovaVikiColors.Background),
    ) {
        val compact = maxWidth < 380.dp
        val previewWidth = when {
            isLightNovel -> 56.dp
            compact -> (maxWidth * 0.25f).coerceIn(82.dp, 96.dp)
            maxWidth >= 600.dp -> (maxWidth * 0.18f).coerceIn(112.dp, 136.dp)
            else -> 118.dp
        }
        val previewHeight = if (isLightNovel) 68.dp else previewWidth * 0.58f
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
                                .fillMaxHeight()
                                .width(3.dp)
                                .background(readStateColor),
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "LN",
                                color = readStateColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = item.chapter.chapterNumber
                                    .takeIf { it > 0.0 }
                                    ?.let(::formatChapterNumber)
                                    ?: "TXT",
                                color = NovaVikiColors.TextPrimary,
                                style = MaterialTheme.typography.labelMedium,
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
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (item.chapter.read) {
                                        "Marquer comme non lu"
                                    } else {
                                        "Marquer comme lu"
                                    },
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (item.chapter.read) {
                                        Icons.Outlined.RemoveDone
                                    } else {
                                        Icons.Outlined.Done
                                    },
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                chapterMenuExpanded = false
                                onToggleRead(!item.chapter.read)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Marquer les chapitres précédents comme lus") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DoneAll,
                                    contentDescription = null,
                                )
                            },
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
                .background(NovaVikiColors.Outline.copy(alpha = 0.5f)),
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
