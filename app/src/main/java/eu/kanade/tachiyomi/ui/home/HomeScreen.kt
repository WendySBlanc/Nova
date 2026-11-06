package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.core.preference.asState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.home.NovaNavigationLayout
import eu.kanade.presentation.home.resolveNovaNavigationDirection
import eu.kanade.presentation.home.resolveNovaNavigationLayout
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.activity.NovaActivitySection
import eu.kanade.tachiyomi.ui.activity.NovaActivityTab
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object HomeScreen : Screen() {
    private fun readResolve(): Any = HomeScreen

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TAB_NAVIGATOR_KEY = "HomeTabs"

    private val TABS = listOf(
        NovaHomeTab,
        BrowseTab,
        NovaLibraryTab,
        NovaActivityTab,
        MoreTab,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val windowWidthDp = LocalConfiguration.current.screenWidthDp
        val navigationLayout = resolveNovaNavigationLayout(windowWidthDp)
        val motionScheme = MaterialTheme.motionScheme

        // SY -->
        val scope = rememberCoroutineScope()
        val alwaysShowLabel by remember {
            Injekt.get<UiPreferences>().bottomBarLabels().asState(scope)
        }
        // SY <--

        TabNavigator(
            tab = NovaHomeTab,
            key = TAB_NAVIGATOR_KEY,
        ) { tabNavigator ->
            val enabledTabs = TABS.fastFilter { it.isEnabled() }
            val selectedNavigationIndex = enabledTabs.indexOfFirst { tab ->
                tabNavigator.current::class == tab::class
            }.coerceAtLeast(0)
            var previousNavigationIndex by remember { mutableIntStateOf(selectedNavigationIndex) }
            var navigationDirection by remember { mutableIntStateOf(0) }
            LaunchedEffect(selectedNavigationIndex) {
                navigationDirection = resolveNovaNavigationDirection(
                    previousIndex = previousNavigationIndex,
                    currentIndex = selectedNavigationIndex,
                )
                previousNavigationIndex = selectedNavigationIndex
            }

            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    startBar = {
                        if (navigationLayout == NovaNavigationLayout.Rail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ) {
                                enabledTabs.fastForEach {
                                    NavigationRailItem(
                                        tab = it,
                                        alwaysShowLabel = alwaysShowLabel,
                                        waveDirection = navigationDirection,
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (navigationLayout == NovaNavigationLayout.BottomBar) {
                            val bottomNavVisible by produceState(initialValue = true) {
                                showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                            }
                            AnimatedVisibility(
                                visible = bottomNavVisible,
                                enter = expandVertically(
                                    animationSpec = motionScheme.fastSpatialSpec(),
                                ) + fadeIn(
                                    animationSpec = motionScheme.fastEffectsSpec(),
                                ),
                                exit = shrinkVertically(
                                    animationSpec = motionScheme.fastSpatialSpec(),
                                ) + fadeOut(
                                    animationSpec = motionScheme.fastEffectsSpec(),
                                ),
                            ) {
                                NovaNavigationBar(
                                    alwaysShowLabel = alwaysShowLabel,
                                    enabledTabs = enabledTabs,
                                    waveDirection = navigationDirection,
                                )
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .consumeWindowInsets(contentPadding),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                fadeIn(
                                    animationSpec = motionScheme.fastEffectsSpec(),
                                ) togetherWith fadeOut(
                                    animationSpec = motionScheme.fastEffectsSpec(),
                                )
                            },
                            label = "tabContent",
                            contentKey = { it.key },
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }
                    }
                }
            }

            val goToHomeTab = { tabNavigator.current = NovaHomeTab }
            BackHandler(
                enabled = tabNavigator.current != NovaHomeTab,
                onBack = goToHomeTab,
            )

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = NovaLibraryTab
                        navigator.push(GlobalSearchScreen(it))
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Library -> NovaLibraryTab
                            Tab.Updates -> {
                                NovaActivityTab.showSection(NovaActivitySection.Updates)
                                NovaActivityTab
                            }
                            Tab.History -> {
                                NovaActivityTab.showSection(NovaActivitySection.History)
                                NovaActivityTab
                            }
                            is Tab.Browse -> {
                                if (it.toExtensions) {
                                    BrowseTab.showExtension()
                                }
                                BrowseTab
                            }
                            is Tab.More -> {
                                if (it.toDownloads) {
                                    NovaActivityTab.showSection(NovaActivitySection.Downloads)
                                    NovaActivityTab
                                } else {
                                    MoreTab
                                }
                            }
                            Tab.Problems -> {
                                NovaActivityTab.showSection(NovaActivitySection.Problems)
                                NovaActivityTab
                            }
                        }

                        if (it is Tab.Library && it.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(it.mangaIdToOpen))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.NavigationBarItem(
        tab: eu.kanade.presentation.util.Tab,
        // SY -->
        alwaysShowLabel: Boolean,
        // SY <--
        waveDirection: Int,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationBarItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = {
                NovaWaveNavigationIcon(
                    selected = selected,
                    direction = waveDirection,
                ) {
                    NavigationIconItem(tab)
                }
            },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = /* SY --> */alwaysShowLabel, /* SY <-- */
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }

    @Composable
    fun NavigationRailItem(
        tab: eu.kanade.presentation.util.Tab,
        // SY -->
        alwaysShowLabel: Boolean,
        // SY <--
        waveDirection: Int,
    ) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationRailItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = {
                NovaWaveNavigationIcon(
                    selected = selected,
                    direction = waveDirection,
                ) {
                    NavigationIconItem(tab)
                }
            },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = /* SY --> */alwaysShowLabel, /* SY <-- */
            colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }

    @Composable
    private fun NovaNavigationBar(
        alwaysShowLabel: Boolean,
        enabledTabs: List<eu.kanade.presentation.util.Tab>,
        waveDirection: Int,
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            enabledTabs.fastForEach {
                NavigationBarItem(
                    tab = it,
                    alwaysShowLabel = alwaysShowLabel,
                    waveDirection = waveDirection,
                )
            }
        }
    }

    @Composable
    private fun NovaWaveNavigationIcon(
        selected: Boolean,
        direction: Int,
        content: @Composable () -> Unit,
    ) {
        val motionScheme = MaterialTheme.motionScheme
        val waveProgress = remember { Animatable(0f) }
        val indicatorAlpha by animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec = motionScheme.fastEffectsSpec(),
            label = "navigation_indicator_alpha",
        )
        val density = LocalDensity.current
        val mainTravelPx = with(density) { 6.dp.toPx() }
        val trailingTravelPx = with(density) { 18.dp.toPx() }
        val indicatorColor = MaterialTheme.colorScheme.primaryContainer
        val waveSpec = motionScheme.defaultSpatialSpec<Float>()

        LaunchedEffect(selected, direction, waveSpec) {
            if (selected && direction != 0) {
                waveProgress.snapTo(1f)
                waveProgress.animateTo(0f, waveSpec)
            } else {
                waveProgress.snapTo(0f)
            }
        }

        Box(
            modifier = Modifier.size(width = 64.dp, height = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .graphicsLayer {
                        alpha = indicatorAlpha
                        scaleX = 1f + (waveProgress.value * 0.16f)
                        translationX = -direction * waveProgress.value * mainTravelPx
                    }
                    .clip(RoundedCornerShape(50))
                    .background(indicatorColor),
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        alpha = indicatorAlpha * waveProgress.value * 0.72f
                        scaleX = 1f + (waveProgress.value * 0.24f)
                        translationX = -direction * waveProgress.value * trailingTravelPx
                    }
                    .clip(RoundedCornerShape(50))
                    .background(indicatorColor),
            )
            content()
        }
    }

    @Composable
    private fun NavigationIconItem(
        tab: eu.kanade.presentation.util.Tab,
        modifier: Modifier = Modifier,
    ) {
        BadgedBox(
            badge = {
                when {
                    tab is UpdatesTab -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<LibraryPreferences>()
                            combine(
                                pref.newShowUpdatesCount().changes(),
                                pref.newUpdatesCount().changes(),
                            ) { show, count -> if (show) count else 0 }
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.notification_chapters_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                    BrowseTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            Injekt.get<SourcePreferences>().extensionUpdatesCount().changes()
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
                // TODO: https://issuetracker.google.com/u/0/issues/316327367
                tint = LocalContentColor.current,
                modifier = modifier,
            )
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data object Problems : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(
            val toDownloads: Boolean,
        ) : Tab
    }
}
