package eu.kanade.tachiyomi.ui.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.NovaLiquidTabLabel
import eu.kanade.presentation.components.novaHollowPacket
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.novaSpatialBackdrop
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.libraryUpdateError.LibraryUpdateErrorScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.flow.MutableStateFlow
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object NovaActivityTab : Tab {
    private fun readResolve(): Any = NovaActivityTab

    private val selectedSection = MutableStateFlow(NovaActivitySection.Updates)

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = stringResource(MR.strings.nova_activity),
            icon = rememberVectorPainter(Icons.Outlined.Notifications),
        )

    override suspend fun onReselect(navigator: Navigator) {
        selectedSection.emit(NovaActivitySection.Updates)
    }

    suspend fun showSection(section: NovaActivitySection) {
        selectedSection.emit(section)
    }

    @Composable
    override fun Content() {
        val section by selectedSection.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .novaSpatialBackdrop(
                    background = NovaVikiColors.Background,
                    primary = NovaVikiColors.Accent,
                    secondary = NovaVikiColors.AccentWarm,
                    tertiary = NovaVikiColors.AccentGold,
                    wireColor = NovaVikiColors.OutlineStrong,
                    wireAlpha = 0.06f,
                    planeAlpha = 0.025f,
                ),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                tonalElevation = androidx.compose.ui.unit.Dp.Hairline,
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = section.ordinal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .novaHollowPacket(
                            background = NovaVikiColors.Surface,
                            outline = NovaVikiColors.OutlineStrong,
                            accent = NovaVikiColors.Accent,
                            secondary = NovaVikiColors.AccentWarm,
                        ),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = androidx.compose.ui.unit.Dp.Hairline,
                    indicator = {},
                ) {
                    NovaActivitySection.entries.forEach { item ->
                        Tab(
                            selected = section == item,
                            onClick = { selectedSection.value = item },
                            text = {
                                NovaLiquidTabLabel(
                                    selected = section == item,
                                    accent = NovaVikiColors.Accent,
                                    outline = NovaVikiColors.OutlineStrong,
                                ) {
                                    Text(
                                        text = "0${item.ordinal + 1} / " + when (item) {
                                            NovaActivitySection.Updates -> stringResource(MR.strings.label_recent_updates)
                                            NovaActivitySection.Downloads -> stringResource(MR.strings.label_download_queue)
                                            NovaActivitySection.History -> stringResource(MR.strings.label_recent_manga)
                                            NovaActivitySection.Problems -> "Problèmes"
                                        }.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            },
                        )
                    }
                }
            }
            HorizontalDivider(color = NovaVikiColors.Outline)

            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    materialFadeThroughIn(initialScale = 1f) togetherWith materialFadeThroughOut()
                },
                label = "activitySection",
            ) { selected ->
                when (selected) {
                    NovaActivitySection.Updates -> UpdatesTab.Content()
                    NovaActivitySection.Downloads -> DownloadQueueScreen.Content()
                    NovaActivitySection.History -> HistoryTab.Content()
                    NovaActivitySection.Problems -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Button(onClick = { navigator.push(LibraryUpdateErrorScreen()) }) {
                                Text("Voir les problèmes de mise à jour")
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class NovaActivitySection {
    Updates,
    Downloads,
    History,
    Problems,
}
