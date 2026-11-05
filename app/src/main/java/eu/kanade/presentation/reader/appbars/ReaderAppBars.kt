package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.reader.MangaTranslationProgress
import eu.kanade.presentation.reader.MangaTranslationUiModel
import eu.kanade.presentation.theme.NovaSignalMotion
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaSpatialBackdrop
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.lnreader.LnReaderWebViewViewer
import kotlinx.collections.immutable.ImmutableSet

private val readerBarsSlideAnimationSpec = tween<IntOffset>(NovaSignalMotion.Control)
private val readerBarsFadeAnimationSpec = tween<Float>(NovaSignalMotion.Feedback)

// SY -->
enum class NavBarType {
    VerticalRight,
    VerticalLeft,
    Bottom,
}
// SY <--

@Composable
@Suppress("UNUSED_PARAMETER")
fun ReaderAppBars(
    visible: Boolean,

    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    onClickTopAppBar: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onShare: (() -> Unit)?,
    mangaTranslation: MangaTranslationUiModel?,
    onMangaTranslationAction: () -> Unit,
    onRetranslateCurrentPage: (() -> Unit)?,

    viewer: Viewer?,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    onClickScrollToTop: () -> Unit,

    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    // SY -->
    isExhToolsVisible: Boolean,
    onSetExhUtilsVisibility: (Boolean) -> Unit,
    isAutoScroll: Boolean,
    isAutoScrollEnabled: Boolean,
    onToggleAutoscroll: (Boolean) -> Unit,
    autoScrollFrequency: String,
    onSetAutoScrollFrequency: (String) -> Unit,
    onClickAutoScrollHelp: () -> Unit,
    onClickRetryAll: () -> Unit,
    onClickRetryAllHelp: () -> Unit,
    onClickBoostPage: () -> Unit,
    onClickBoostPageHelp: () -> Unit,
    navBarType: NavBarType,
    currentPageText: String,
    enabledButtons: ImmutableSet<String>,
    currentReadingMode: ReadingMode,
    dualPageSplitEnabled: Boolean,
    doublePages: Boolean,
    onClickChapterList: () -> Unit,
    onClickPageLayout: () -> Unit,
    onClickShiftPage: () -> Unit,
    // SY <--
) {
    val backgroundColor = NovaVikiColors.Surface.copy(alpha = 0.97f)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxHeight()) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { -it }, animationSpec = readerBarsSlideAnimationSpec) +
                    fadeIn(animationSpec = readerBarsFadeAnimationSpec),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = readerBarsSlideAnimationSpec) +
                    fadeOut(animationSpec = readerBarsFadeAnimationSpec),
            ) {
                Column {
                    ReaderTopBar(
                        modifier = Modifier
                            .novaSpatialBackdrop(
                                background = backgroundColor,
                                primary = NovaVikiColors.Accent,
                                secondary = NovaVikiColors.AccentWarm,
                                tertiary = NovaVikiColors.AccentGold,
                                wireColor = NovaVikiColors.OutlineStrong,
                                wireAlpha = 0.055f,
                                planeAlpha = 0.02f,
                                showMark = false,
                            ),
                        mangaTitle = mangaTitle,
                        chapterTitle = chapterTitle,
                        navigateUp = navigateUp,
                        onClickTitle = onClickTopAppBar,
                        bookmarked = bookmarked,
                        onToggleBookmarked = onToggleBookmarked,
                        onOpenInWebView = onOpenInWebView,
                        onOpenInBrowser = onOpenInBrowser,
                        onShare = onShare,
                        mangaTranslation = mangaTranslation,
                        onMangaTranslationAction = onMangaTranslationAction,
                        onRetranslateCurrentPage = onRetranslateCurrentPage,
                    )
                    mangaTranslation?.let { translation ->
                        MangaTranslationProgress(
                            model = translation,
                            onCancel = onMangaTranslationAction,
                        )
                    }
                    HorizontalDivider(color = NovaVikiColors.Outline)
                    if (isExhToolsVisible) {
                        ExhUtils(
                            isVisible = true,
                            onSetExhUtilsVisibility = onSetExhUtilsVisibility,
                            backgroundColor = backgroundColor,
                            isAutoScroll = isAutoScroll,
                            isAutoScrollEnabled = isAutoScrollEnabled,
                            onToggleAutoscroll = onToggleAutoscroll,
                            autoScrollFrequency = autoScrollFrequency,
                            onSetAutoScrollFrequency = onSetAutoScrollFrequency,
                            onClickAutoScrollHelp = onClickAutoScrollHelp,
                            onClickRetryAll = onClickRetryAll,
                            onClickRetryAllHelp = onClickRetryAllHelp,
                            onClickBoostPage = onClickBoostPage,
                            onClickBoostPageHelp = onClickBoostPageHelp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = readerBarsSlideAnimationSpec) +
                    fadeIn(animationSpec = readerBarsFadeAnimationSpec),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = readerBarsSlideAnimationSpec) +
                    fadeOut(animationSpec = readerBarsFadeAnimationSpec),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = NovaVikiColors.Accent.copy(alpha = 0.38f),
                                shape = RoundedCornerShape(NovaVikiDimensions.CardCorner),
                            ),
                        shape = RoundedCornerShape(NovaVikiDimensions.CardCorner),
                        color = backgroundColor,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                    ) {
                        ReaderBottomBar(
                            onPreviousChapter = onPreviousChapter,
                            enabledPrevious = enabledPrevious,
                            onClickScrollToTop = onClickScrollToTop,
                            onClickChapterList = onClickChapterList,
                            onClickSettings = onClickSettings,
                            onNextChapter = onNextChapter,
                            enabledNext = enabledNext,
                            currentPage = currentPage,
                            currentPageText = currentPageText,
                            totalPages = totalPages,
                            isTextReader = viewer is LnReaderWebViewViewer,
                            showProgressSlider = true,
                            onPageIndexChange = onPageIndexChange,
                        )
                    }
                }
            }
        }
    }
}
