package eu.kanade.tachiyomi.ui.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
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
            index = 2u,
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

        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = androidx.compose.ui.unit.Dp.Unspecified,
            ) {
                PrimaryTabRow(
                    selectedTabIndex = section.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    NovaActivitySection.entries.forEach { item ->
                        Tab(
                            selected = section == item,
                            onClick = { selectedSection.value = item },
                            text = {
                                Text(
                                    text = when (item) {
                                        NovaActivitySection.Updates -> stringResource(MR.strings.label_recent_updates)
                                        NovaActivitySection.Downloads -> stringResource(MR.strings.label_download_queue)
                                        NovaActivitySection.History -> stringResource(MR.strings.label_recent_manga)
                                    },
                                )
                            },
                        )
                    }
                }
            }

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
                }
            }
        }
    }
}

enum class NovaActivitySection {
    Updates,
    Downloads,
    History,
}
