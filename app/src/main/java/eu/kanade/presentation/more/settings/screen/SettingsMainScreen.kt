package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import exh.assets.EhAssets
import exh.assets.ehassets.EhLogo
import exh.assets.ehassets.MangadexLogo
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.reflect.KClass
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

object SettingsMainScreen : Screen() {
    @Suppress("unused")
    private fun readResolve(): Any = SettingsMainScreen

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(3)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - 0.05f
            } else {
                arr[2] + 0.02f
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val topBarState = rememberTopAppBarState()

        Scaffold(
            topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState),
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_settings),
                    navigateUp = backPress::invoke,
                    actions = {
                        AppBarActions(
                            persistentListOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_search),
                                    icon = Icons.Outlined.Search,
                                    onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = containerColor,
            content = { contentPadding ->
                // SY -->
                val visibleItems = items.filter { it.screen !is SearchableSettings || it.screen.isEnabled() }
                // SY <--
                val groups = SettingsSection.entries.mapNotNull { section ->
                    visibleItems
                        .filter { item -> item.section == section }
                        .takeIf { sectionItems -> sectionItems.isNotEmpty() }
                        ?.let { sectionItems -> SettingsGroup(section, sectionItems) }
                }
                val selectedScreenClass = navigator.items.firstOrNull()?.let { it::class }
                val selectedGroupIndex = if (twoPane && selectedScreenClass != null) {
                    groups.indexOfFirst { group ->
                        group.items.any { item -> item.screen::class == selectedScreenClass }
                    }
                } else {
                    -1
                }
                val listState = rememberLazyListState()
                val gridState = rememberLazyGridState()
                val layoutDirection = LocalLayoutDirection.current

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val expandedLayout = !twoPane && maxWidth >= 840.dp
                    val horizontalPadding = if (expandedLayout) {
                        NovaVikiDimensions.ExpandedHorizontalPadding
                    } else {
                        NovaVikiDimensions.HorizontalPadding
                    }
                    val overviewPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection) + horizontalPadding,
                        top = contentPadding.calculateTopPadding() + 12.dp,
                        end = contentPadding.calculateEndPadding(layoutDirection) + horizontalPadding,
                        bottom = contentPadding.calculateBottomPadding() + 24.dp,
                    )

                    LaunchedEffect(selectedGroupIndex) {
                        if (selectedGroupIndex >= 0) {
                            listState.animateScrollToItem(selectedGroupIndex)
                            if (selectedGroupIndex > 0) {
                                topBarState.contentOffset = topBarState.heightOffsetLimit
                            }
                        }
                    }

                    if (expandedLayout) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .widthIn(max = NovaVikiDimensions.WideContentMaxWidth)
                                .fillMaxSize()
                                .align(Alignment.TopCenter),
                            state = gridState,
                            contentPadding = overviewPadding,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            gridItems(
                                items = groups,
                                key = { group -> group.section.name },
                            ) { group ->
                                SettingsOverviewGroup(
                                    group = group,
                                    selectedScreenClass = null,
                                    onClick = { item -> navigator.navigate(item.screen, twoPane = false) },
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .widthIn(max = NovaVikiDimensions.CompactContentMaxWidth)
                                .fillMaxSize()
                                .align(Alignment.TopCenter),
                            state = listState,
                            contentPadding = overviewPadding,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            items(
                                count = groups.size,
                                key = { index -> groups[index].section.name },
                            ) { index ->
                                SettingsOverviewGroup(
                                    group = groups[index],
                                    selectedScreenClass = selectedScreenClass,
                                    onClick = { item -> navigator.navigate(item.screen, twoPane) },
                                )
                            }
                        }
                    }
                }
            },
        )
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    private data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource,
        val formatSubtitle: @Composable () -> String = { stringResource(subtitleRes) },
        val icon: ImageVector,
        val screen: VoyagerScreen,
        val section: SettingsSection,
    )

    private data class SettingsGroup(
        val section: SettingsSection,
        val items: List<Item>,
    )

    private enum class SettingsSection(
        val title: String,
        val accent: Color,
    ) {
        Reading("Lecture et apparence", NovaVikiColors.Accent),
        Library("Biblioth\u00e8que et contenu", NovaVikiColors.AccentGold),
        Services("Services", NovaVikiColors.Success),
        Data("Donn\u00e9es et confidentialit\u00e9", NovaVikiColors.AccentWarm),
        Advanced("Avanc\u00e9", Color(0xFFB9A5FF)),
        Application("Application", NovaVikiColors.TextSecondary),
    }

    @Composable
    private fun SettingsOverviewGroup(
        group: SettingsGroup,
        selectedScreenClass: KClass<out VoyagerScreen>?,
        onClick: (Item) -> Unit,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = group.section.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NovaVikiDimensions.ControlCorner))
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
                    ),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                group.items.forEach { item ->
                    val selected = selectedScreenClass != null && item.screen::class == selectedScreenClass
                    SettingsOverviewItem(
                        item = item,
                        accent = group.section.accent,
                        selected = selected,
                        onClick = { onClick(item) },
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsOverviewItem(
        item: Item,
        accent: Color,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.titleRes),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.formatSubtitle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }

    private val items = listOf(
        Item(
            titleRes = MR.strings.pref_category_appearance,
            subtitleRes = MR.strings.pref_appearance_summary,
            icon = Icons.Outlined.Palette,
            screen = SettingsAppearanceScreen,
            section = SettingsSection.Reading,
        ),
        Item(
            titleRes = MR.strings.pref_category_library,
            subtitleRes = MR.strings.pref_library_summary,
            icon = Icons.Outlined.CollectionsBookmark,
            screen = SettingsLibraryScreen,
            section = SettingsSection.Library,
        ),
        Item(
            titleRes = MR.strings.pref_category_reader,
            subtitleRes = MR.strings.pref_reader_summary,
            icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
            screen = SettingsReaderScreen,
            section = SettingsSection.Reading,
        ),
        Item(
            titleRes = MR.strings.pref_category_downloads,
            subtitleRes = MR.strings.pref_downloads_summary,
            icon = Icons.Outlined.GetApp,
            screen = SettingsDownloadScreen,
            section = SettingsSection.Library,
        ),
        Item(
            titleRes = MR.strings.pref_category_tracking,
            subtitleRes = MR.strings.pref_tracking_summary,
            icon = Icons.Outlined.Sync,
            screen = SettingsTrackingScreen,
            section = SettingsSection.Services,
        ),
        // AM (CONNECTIONS) -->
        Item(
            titleRes = KMR.strings.pref_category_connections,
            subtitleRes = KMR.strings.pref_connections_summary,
            icon = Icons.Outlined.Link,
            screen = SettingsConnectionScreen,
            section = SettingsSection.Services,
        ),
        // <-- AM (CONNECTIONS)
        Item(
            titleRes = MR.strings.browse,
            subtitleRes = MR.strings.pref_browse_summary,
            icon = Icons.Outlined.Explore,
            screen = SettingsBrowseScreen,
            section = SettingsSection.Library,
        ),
        Item(
            titleRes = MR.strings.label_data_storage,
            subtitleRes = MR.strings.pref_backup_summary,
            icon = Icons.Outlined.Storage,
            screen = SettingsDataScreen,
            section = SettingsSection.Data,
        ),
        Item(
            titleRes = MR.strings.pref_category_security,
            subtitleRes = MR.strings.pref_security_summary,
            icon = Icons.Outlined.Security,
            screen = SettingsSecurityScreen,
            section = SettingsSection.Data,
        ),
        // SY -->
        Item(
            titleRes = SYMR.strings.pref_category_eh,
            subtitleRes = SYMR.strings.pref_ehentai_summary,
            icon = EhAssets.EhLogo,
            screen = SettingsEhScreen,
            section = SettingsSection.Advanced,
        ),
        Item(
            titleRes = SYMR.strings.pref_category_mangadex,
            subtitleRes = SYMR.strings.pref_mangadex_summary,
            icon = EhAssets.MangadexLogo,
            screen = SettingsMangadexScreen,
            section = SettingsSection.Advanced,
        ),
        // SY <--
        Item(
            titleRes = MR.strings.pref_category_advanced,
            subtitleRes = MR.strings.pref_advanced_summary,
            icon = Icons.Outlined.Code,
            screen = SettingsAdvancedScreen,
            section = SettingsSection.Advanced,
        ),
        Item(
            titleRes = MR.strings.pref_category_about,
            subtitleRes = StringResource(0),
            formatSubtitle = {
                "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
            },
            icon = Icons.Outlined.Info,
            screen = AboutScreen(),
            section = SettingsSection.Application,
        ),
    )
}
