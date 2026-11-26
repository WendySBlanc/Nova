package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.home.novaInkEntrance
import eu.kanade.presentation.theme.novaReducedMotion
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.lnreader.LnReaderWebViewViewer
import kotlinx.collections.immutable.ImmutableSet

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
    readerLensAvailable: Boolean,
    readerLensEnabled: Boolean,
    readerLensWorking: Boolean,
    readerLensRuntimeSupported: Boolean,
    readerLensDownloadProgress: Int?,
    readerLensFailed: Boolean,
    onReaderLensToggle: () -> Unit,

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
    val motionScheme = MaterialTheme.motionScheme
    val travelPx = with(LocalDensity.current) { 20.dp.roundToPx() }
    val reducedMotion = novaReducedMotion()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxHeight()) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { -minOf(it, travelPx) },
                    animationSpec = motionScheme.fastSpatialSpec(),
                ) + fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                exit = slideOutVertically(
                    targetOffsetY = { -minOf(it, travelPx) },
                    animationSpec = motionScheme.fastSpatialSpec(),
                ) + fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
            ) {
                val reveal = if (reducedMotion) Modifier else Modifier.novaInkEntrance(this, !rtl, withSurface = false)
                Column(modifier = reveal) {
                    ReaderTopBar(
                        mangaTitle = mangaTitle,
                        chapterTitle = chapterTitle,
                        navigateUp = navigateUp,
                        onClickTitle = onClickTopAppBar,
                        bookmarked = bookmarked,
                        onToggleBookmarked = onToggleBookmarked,
                        onOpenInWebView = onOpenInWebView,
                        onOpenInBrowser = onOpenInBrowser,
                        onShare = onShare,
                    )
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
                enter = slideInVertically(
                    initialOffsetY = { minOf(it, travelPx) },
                    animationSpec = motionScheme.fastSpatialSpec(),
                ) + fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                exit = slideOutVertically(
                    targetOffsetY = { minOf(it, travelPx) },
                    animationSpec = motionScheme.fastSpatialSpec(),
                ) + fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
            ) {
                val reveal = if (reducedMotion) Modifier else Modifier.novaInkEntrance(this, rtl, withSurface = false)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .then(reveal),
                    contentAlignment = Alignment.Center,
                ) {
                    ReaderBottomBar(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth(),
                        onPreviousChapter = onPreviousChapter,
                        enabledPrevious = enabledPrevious,
                        onClickScrollToTop = onClickScrollToTop,
                        onClickChapterList = onClickChapterList,
                        readerLensAvailable = readerLensAvailable,
                        readerLensEnabled = readerLensEnabled,
                        readerLensWorking = readerLensWorking,
                        readerLensRuntimeSupported = readerLensRuntimeSupported,
                        readerLensDownloadProgress = readerLensDownloadProgress,
                        readerLensFailed = readerLensFailed,
                        onReaderLensToggle = onReaderLensToggle,
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
