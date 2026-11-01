package eu.kanade.presentation.reader

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadIndicator
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.util.lang.toRelativeString
import exh.metadata.MetadataUtil
import exh.source.isEhBasedManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun ChapterListDialog(
    onDismissRequest: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
    chapters: ImmutableList<ReaderChapterItem>,
    onClickChapter: (Chapter) -> Unit,
    onBookmark: (Chapter) -> Unit,
    dateRelativeTime: Boolean,
    // KMK -->
    onDownloadAction: ((Chapter, ChapterDownloadAction) -> Unit)? = null,
    // KMK <--
) {
    val manga by screenModel.mangaFlow.collectAsState()
    val context = LocalContext.current
    val state = rememberLazyListState(chapters.indexOfFirst { it.isCurrent }.coerceAtLeast(0))
    val downloadManager: DownloadManager = remember { Injekt.get() }
    val downloadQueueState by downloadManager.queueState.collectAsState()
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val drawerWidth = when {
        configuration.screenWidthDp < 360 -> configuration.screenWidthDp.dp
        configuration.screenWidthDp < 600 -> configuration.screenWidthDp.dp * 0.92f
        configuration.screenWidthDp < 840 -> 400.dp
        else -> 440.dp
    }
    val listVerticalPadding = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        6.dp
    } else {
        12.dp
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(drawerWidth)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                shape = RoundedCornerShape(
                    topEnd = NovaVikiDimensions.ControlCorner,
                    bottomEnd = NovaVikiDimensions.ControlCorner,
                ),
                color = NovaVikiColors.Surface,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(MR.strings.chapters),
                                color = NovaVikiColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${chapters.size} chapitres",
                                color = NovaVikiColors.TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Fermer",
                                tint = NovaVikiColors.TextPrimary,
                            )
                        }
                    }
                    HorizontalDivider(color = NovaVikiColors.Outline)
                    LazyColumn(
                        state = state,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 200.dp),
                        contentPadding = PaddingValues(vertical = listVerticalPadding),
                    ) {
                        items(
                            items = chapters,
                            key = { "chapter-list-${it.chapter.id}" },
                        ) { chapterItem ->
                            val activeDownload = downloadQueueState.find { it.chapter.id == chapterItem.chapter.id }
                            val progress = activeDownload?.let {
                                downloadManager.progressFlow()
                                    .filter { it.chapter.id == chapterItem.chapter.id }
                                    .map { it.progress }
                                    .collectAsState(0).value
                            } ?: 0
                            val downloaded = if (chapterItem.manga.isLocal()) {
                                true
                            } else {
                                downloadManager.isChapterDownloaded(
                                    chapterItem.chapter.name,
                                    chapterItem.chapter.scanlator,
                                    chapterItem.chapter.url,
                                    chapterItem.manga.ogTitle,
                                    chapterItem.manga.source,
                                )
                            }
                            val downloadState = when {
                                activeDownload != null -> activeDownload.status
                                downloaded -> Download.State.DOWNLOADED
                                else -> Download.State.NOT_DOWNLOADED
                            }
                            LnReaderChapterListItem(
                                title = chapterItem.chapter.name,
                                date = chapterItem.chapter.dateUpload
                                    .takeIf { it > 0L }
                                    ?.let {
                                        // SY -->
                                        if (manga?.isEhBasedManga() == true) {
                                            MetadataUtil.EX_DATE_FORMAT
                                                .format(
                                                    ZonedDateTime.ofInstant(
                                                        Instant.ofEpochMilli(it),
                                                        ZoneId.systemDefault(),
                                                    ),
                                                )
                                        } else {
                                            LocalDate.ofInstant(
                                                Instant.ofEpochMilli(it),
                                                ZoneId.systemDefault(),
                                            ).toRelativeString(context, dateRelativeTime, chapterItem.dateFormat)
                                        }
                                        // SY <--
                                    },
                                read = chapterItem.chapter.read,
                                bookmark = chapterItem.chapter.bookmark,
                                selected = chapterItem.isCurrent,
                                downloadIndicatorEnabled = onDownloadAction != null,
                                downloadStateProvider = { downloadState },
                                downloadProgressProvider = { progress },
                                onLongClick = { onBookmark(chapterItem.chapter) },
                                onClick = { onClickChapter(chapterItem.chapter) },
                                onDownloadClick = if (onDownloadAction != null) {
                                    { action -> onDownloadAction(chapterItem.chapter, action) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                    HorizontalDivider(color = NovaVikiColors.Outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { scope.launch { state.animateScrollToItem(0) } },
                            modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = "Premier chapitre",
                                tint = NovaVikiColors.TextPrimary,
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    state.animateScrollToItem((chapters.size - 1).coerceAtLeast(0))
                                }
                            },
                            modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Dernier chapitre",
                                tint = NovaVikiColors.TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LnReaderChapterListItem(
    title: String,
    date: String?,
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
) {
    val titleColor = when {
        selected -> NovaVikiColors.Accent
        read -> NovaVikiColors.TextSecondary.copy(alpha = 0.64f)
        else -> NovaVikiColors.TextPrimary
    }
    val subtitleColor = if (read) {
        NovaVikiColors.TextSecondary.copy(alpha = 0.56f)
    } else {
        NovaVikiColors.TextSecondary
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
            .background(
                if (selected) {
                    NovaVikiColors.Accent.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .heightIn(min = 56.dp)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 36.dp)
                .background(
                    color = if (selected) NovaVikiColors.Accent else Color.Transparent,
                    shape = RoundedCornerShape(2.dp),
                ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                        modifier = Modifier.size(16.dp),
                        tint = NovaVikiColors.Accent,
                    )
                }
                Text(
                    text = title,
                    color = titleColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        letterSpacing = 0.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (date != null) {
                Text(
                    text = date,
                    color = subtitleColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        letterSpacing = 0.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onDownloadClick != null) {
            ChapterDownloadIndicator(
                enabled = downloadIndicatorEnabled,
                modifier = Modifier.padding(start = 4.dp),
                downloadStateProvider = downloadStateProvider,
                downloadProgressProvider = downloadProgressProvider,
                onClick = onDownloadClick,
            )
        }
    }
}
