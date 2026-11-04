package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.kanade.presentation.components.NovaLiquidTabLabel
import eu.kanade.presentation.components.novaHollowPacket
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.util.isLikelyLightNovel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.viewer.lnreader.LnReaderWebViewViewer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun ReaderSettingsDialog(
    onDismissRequest: () -> Unit,
    onShowMenus: () -> Unit,
    onHideMenus: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
) {
    val manga by screenModel.mangaFlow.collectAsState()
    val sourceName by screenModel.sourceNameFlow.collectAsState()
    val viewer by screenModel.viewerFlow.collectAsState()
    val imageReader = (viewer as? LnReaderWebViewViewer)?.isImageChapter == true
    val textReader = remember(manga, sourceName, viewer, imageReader) {
        (viewer is LnReaderWebViewViewer && !imageReader) || manga.isLikelyLightNovel(sourceName)
    }
    val tabTitles = when {
        imageReader -> persistentListOf("Webtoon", "Général")
        textReader -> persistentListOf("Lecture", "Général", "Voix", "Avancé")
        else -> persistentListOf("Général")
    }
    val pagerState = rememberPagerState { tabTitles.size }

    DisposableEffect(Unit) {
        onHideMenus()
        onDispose { onShowMenus() }
    }

    LaunchedEffect(textReader, imageReader) {
        if (pagerState.currentPage >= tabTitles.size) {
            pagerState.scrollToPage(0)
        }
    }

    BoxWithConstraints {
        val scope = rememberCoroutineScope()
        val expandedLayout = maxWidth >= 600.dp
        val sheetHeight = when {
            maxHeight < 420.dp -> maxHeight
            expandedLayout -> minOf(680.dp, maxHeight)
            maxHeight < 700.dp -> maxHeight * 0.86f
            else -> 620.dp
        }
        val sheetColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        val shape = if (expandedLayout) {
            RoundedCornerShape(NovaVikiDimensions.ControlCorner)
        } else {
            RoundedCornerShape(
                topStart = NovaVikiDimensions.ControlCorner,
                topEnd = NovaVikiDimensions.ControlCorner,
            )
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
                        .align(if (expandedLayout) Alignment.Center else Alignment.BottomCenter)
                        .widthIn(max = if (expandedLayout) 720.dp else 640.dp)
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .navigationBarsPadding()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    shape = shape,
                    color = sheetColor,
                    shadowElevation = if (expandedLayout) 12.dp else 0.dp,
                ) {
                    Column {
                        if (!expandedLayout) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(4.dp)
                                        .background(
                                            color = NovaVikiColors.Outline,
                                            shape = RoundedCornerShape(50),
                                        ),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(start = 20.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Réglages de lecture",
                                    color = NovaVikiColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (imageReader) "Manhua et webtoon" else "Light novel",
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

                        if (tabTitles.size > 1) {
                            PrimaryScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                edgePadding = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .novaHollowPacket(
                                        background = NovaVikiColors.Surface,
                                        outline = NovaVikiColors.OutlineStrong,
                                        accent = NovaVikiColors.Accent,
                                        secondary = NovaVikiColors.AccentWarm,
                                    ),
                                containerColor = Color.Transparent,
                                divider = {},
                                indicator = {},
                            ) {
                                tabTitles.forEachIndexed { index, title ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        },
                                        modifier = Modifier.height(48.dp),
                                        text = {
                                            NovaLiquidTabLabel(
                                                selected = pagerState.currentPage == index,
                                                accent = NovaVikiColors.Accent,
                                                outline = NovaVikiColors.OutlineStrong,
                                            ) {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                )
                                            }
                                        },
                                        selectedContentColor = NovaVikiColors.Accent,
                                        unselectedContentColor = NovaVikiColors.TextSecondary,
                                    )
                                }
                            }
                            HorizontalDivider(color = NovaVikiColors.Outline)
                        }

                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = pagerState,
                            verticalAlignment = Alignment.Top,
                        ) { page ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 8.dp, bottom = 24.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                if (imageReader) {
                                    when (page) {
                                        0 -> LnReaderImagePage(screenModel)
                                        1 -> LnReaderGeneralPage(screenModel, textReader = false)
                                    }
                                } else if (textReader) {
                                    when (page) {
                                        0 -> LnReaderPage(screenModel)
                                        1 -> LnReaderGeneralPage(screenModel, textReader = true)
                                        2 -> LnReaderTtsPage(screenModel)
                                        3 -> LnReaderAdvancedPage(screenModel)
                                    }
                                } else {
                                    LnReaderGeneralPage(screenModel, textReader = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
