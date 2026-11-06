package eu.kanade.presentation.more

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.home.NovaMeCategory
import eu.kanade.presentation.home.NovaMeSettingDestination
import eu.kanade.presentation.home.novaMeCategories
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaSpatialBackdrop
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import exh.assets.EhAssets
import exh.assets.ehassets.EhLogo
import exh.assets.ehassets.MangadexLogo
import exh.pref.DelegateSourcePreferences
import exh.source.ExhPreferences
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickSetting: (NovaMeSettingDestination) -> Unit,
    onClickSettingsSearch: () -> Unit,
    showEhSettings: Boolean,
    showMangadexSettings: Boolean,
    onClickBatchAdd: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    // SY -->
    val showBatchAdd = remember {
        runCatching {
            Injekt.get<ExhPreferences>().isHentaiEnabled().get() ||
                Injekt.get<DelegateSourcePreferences>().delegateSources().get()
        }.getOrDefault(false)
    }
    // SY <--

    val downloadQueueState = runCatching(downloadQueueStateProvider).getOrDefault(DownloadQueueState.Stopped)
    val downloadQueueSubtitle = when (downloadQueueState) {
        DownloadQueueState.Stopped -> null
        is DownloadQueueState.Paused -> {
            val pending = downloadQueueState.pending
            if (pending == 0) {
                stringResource(MR.strings.paused)
            } else {
                "${stringResource(MR.strings.paused)} - ${
                    pluralStringResource(
                        MR.plurals.download_queue_summary,
                        count = pending,
                        pending,
                    )
                }"
            }
        }
        is DownloadQueueState.Downloading -> {
            val pending = downloadQueueState.pending
            pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
        }
    }
    val meCategories = remember { novaMeCategories() }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedCategory = meCategories[selectedCategoryIndex]

    Scaffold(contentWindowInsets = WindowInsets(0)) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .novaSpatialBackdrop(
                    background = NovaVikiColors.Background,
                    primary = NovaVikiColors.Accent,
                    secondary = NovaVikiColors.AccentWarm,
                    tertiary = NovaVikiColors.AccentGold,
                    wireColor = NovaVikiColors.OutlineStrong,
                    wireAlpha = 0.08f,
                    planeAlpha = 0.035f,
                ),
        ) {
            NovaMeTopBar(onClickSettingsSearch = onClickSettingsSearch)
            NovaMeCategoryTabs(
                categories = meCategories,
                selectedIndex = selectedCategoryIndex,
                onSelect = { selectedCategoryIndex = it },
            )
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = NovaVikiDimensions.WideContentMaxWidth)
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally),
            ) {
                val expandedLayout = maxWidth >= 720.dp
                val horizontalPadding = when {
                    maxWidth < 360.dp -> NovaVikiDimensions.CompactHorizontalPadding
                    expandedLayout -> NovaVikiDimensions.ExpandedHorizontalPadding
                    else -> NovaVikiDimensions.HorizontalPadding
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 12.dp,
                        bottom = contentPadding.calculateBottomPadding() + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(NovaVikiDimensions.SectionSpacing),
                ) {
                    when (selectedCategory) {
                        NovaMeCategory.Overview -> {
                            item(contentType = "me_identity") {
                                NovaMeIdentity(
                                    downloadedOnly = downloadedOnly,
                                    incognitoMode = incognitoMode,
                                )
                            }
                            item(contentType = "me_overview_actions") {
                                NovaQuickActions(
                                    downloadQueueState = downloadQueueState,
                                    downloadQueueSubtitle = downloadQueueSubtitle,
                                    onClickDownloadQueue = onClickDownloadQueue,
                                    onClickStats = onClickStats,
                                )
                            }
                        }
                        NovaMeCategory.Reading -> item(contentType = "me_reading") {
                            NovaReadingModesSection(
                                downloadedOnly = downloadedOnly,
                                onDownloadedOnlyChange = onDownloadedOnlyChange,
                                incognitoMode = incognitoMode,
                                onIncognitoModeChange = onIncognitoModeChange,
                            )
                        }
                        NovaMeCategory.Library -> item(contentType = "me_library") {
                            NovaLibraryToolsSection(
                                showBatchAdd = showBatchAdd,
                                onClickCategories = onClickCategories,
                                onClickBatchAdd = onClickBatchAdd,
                            )
                        }
                        NovaMeCategory.Application -> item(contentType = "me_application") {
                            NovaSettingsSections(
                                showEhSettings = showEhSettings,
                                showMangadexSettings = showMangadexSettings,
                                onClickSetting = onClickSetting,
                                onClickHelp = { uriHandler.openUri(Constants.URL_HELP) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaMeTopBar(
    onClickSettingsSearch: () -> Unit,
) {
    MediumTopAppBar(
        title = {
            Column {
                Text(
                    text = "Param\u00e8tres",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = "Profil, activit\u00e9 et param\u00e8tres",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        },
        actions = {
            IconButton(onClick = onClickSettingsSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Rechercher dans les param\u00e8tres",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun NovaMeCategoryTabs(
    categories: List<NovaMeCategory>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = NovaVikiDimensions.HorizontalPadding,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                modifier = Modifier.height(NovaVikiDimensions.MinimumTouchTarget),
                text = {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun NovaMeIdentity(
    downloadedOnly: Boolean,
    incognitoMode: Boolean,
) {
    val motionScheme = MaterialTheme.motionScheme
    val status = when {
        downloadedOnly && incognitoMode -> "T\u00e9l\u00e9charg\u00e9s uniquement \u2022 Historique suspendu"
        downloadedOnly -> "T\u00e9l\u00e9charg\u00e9s uniquement"
        incognitoMode -> "Historique suspendu"
        else -> "Manhwa & Light Novel"
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .animateContentSize(animationSpec = motionScheme.defaultSpatialSpec()),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "N",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nova",
                    color = NovaVikiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = status,
                    color = NovaVikiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NovaQuickActions(
    downloadQueueState: DownloadQueueState,
    downloadQueueSubtitle: String?,
    onClickDownloadQueue: () -> Unit,
    onClickStats: () -> Unit,
) {
    val downloadBadge = when (downloadQueueState) {
        DownloadQueueState.Stopped -> null
        is DownloadQueueState.Paused -> downloadQueueState.pending
        is DownloadQueueState.Downloading -> downloadQueueState.pending
    }?.takeIf { it > 0 }?.let { count -> if (count > 99) "99+" else count.toString() }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Acc\u00e8s rapides",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NovaQuickAction(
                modifier = Modifier.weight(1f),
                onClick = onClickDownloadQueue,
                label = "T\u00e9l\u00e9chargements",
                icon = {
                    BadgedBox(
                        badge = {
                            downloadBadge?.let {
                                Badge {
                                    Text(text = it)
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GetApp,
                            contentDescription = null,
                            tint = when (downloadQueueState) {
                                DownloadQueueState.Stopped -> NovaMoreAccent
                                is DownloadQueueState.Paused -> NovaVikiColors.AccentGold
                                is DownloadQueueState.Downloading -> NovaVikiColors.Success
                            },
                        )
                    }
                },
            )
            NovaQuickAction(
                modifier = Modifier.weight(1f),
                onClick = onClickStats,
                label = "Statistiques",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.QueryStats,
                        contentDescription = null,
                        tint = NovaVikiColors.AccentWarm,
                    )
                },
            )
        }
        if (!downloadQueueSubtitle.isNullOrBlank()) {
            Text(
                text = "T\u00e9l\u00e9chargements : $downloadQueueSubtitle",
                color = when (downloadQueueState) {
                    DownloadQueueState.Stopped -> MaterialTheme.colorScheme.onSurfaceVariant
                    is DownloadQueueState.Paused -> NovaVikiColors.AccentGold
                    is DownloadQueueState.Downloading -> NovaVikiColors.Success
                },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.animateContentSize(
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                ),
            )
        }
    }
}

@Composable
private fun NovaQuickAction(
    modifier: Modifier,
    onClick: () -> Unit,
    label: String,
    icon: @Composable () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon()
            Text(
                text = label,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NovaReadingModesSection(
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
) {
    NovaMoreSection(title = "Lecture") {
        NovaMoreToggleRow(
            title = stringResource(MR.strings.label_downloaded_only),
            subtitle = stringResource(MR.strings.downloaded_only_summary),
            icon = Icons.Outlined.CloudOff,
            checked = downloadedOnly,
            onCheckedChanged = onDownloadedOnlyChange,
        )
        NovaMoreToggleRow(
            title = stringResource(MR.strings.pref_incognito_mode),
            subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
            icon = rememberAnimatedVectorPainter(
                AnimatedImageVector.animatedVectorResource(R.drawable.anim_incognito),
                incognitoMode,
            ),
            checked = incognitoMode,
            onCheckedChanged = onIncognitoModeChange,
        )
    }
}

@Composable
private fun NovaLibraryToolsSection(
    showBatchAdd: Boolean,
    onClickCategories: () -> Unit,
    onClickBatchAdd: () -> Unit,
) {
    NovaMoreSection(title = stringResource(MR.strings.label_library)) {
        NovaMoreActionRow(
            title = "Cat\u00e9gories",
            icon = Icons.AutoMirrored.Outlined.Label,
            onClick = onClickCategories,
            accent = NovaVikiColors.AccentGold,
        )
        if (showBatchAdd) {
            NovaMoreActionRow(
                title = stringResource(SYMR.strings.eh_batch_add),
                icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                onClick = onClickBatchAdd,
            )
        }
    }
}

@Composable
private fun NovaSettingsSections(
    showEhSettings: Boolean,
    showMangadexSettings: Boolean,
    onClickSetting: (NovaMeSettingDestination) -> Unit,
    onClickHelp: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NovaVikiDimensions.SectionSpacing)) {
        NovaMoreSection(title = "Lecture et apparence") {
            NovaMoreActionRow(
                title = stringResource(MR.strings.pref_category_appearance),
                subtitle = stringResource(MR.strings.pref_appearance_summary),
                icon = Icons.Outlined.Palette,
                onClick = { onClickSetting(NovaMeSettingDestination.Appearance) },
            )
            NovaMoreActionRow(
                title = stringResource(MR.strings.pref_category_reader),
                subtitle = stringResource(MR.strings.pref_reader_summary),
                icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
                onClick = { onClickSetting(NovaMeSettingDestination.Reader) },
            )
        }
        NovaMoreSection(title = "Biblioth\u00e8que et contenu") {
            NovaMoreActionRow(
                title = stringResource(MR.strings.pref_category_library),
                subtitle = stringResource(MR.strings.pref_library_summary),
                icon = Icons.Outlined.CollectionsBookmark,
                onClick = { onClickSetting(NovaMeSettingDestination.Library) },
            )
            NovaMoreActionRow(
                title = stringResource(MR.strings.pref_category_downloads),
                subtitle = stringResource(MR.strings.pref_downloads_summary),
                icon = Icons.Outlined.GetApp,
                onClick = { onClickSetting(NovaMeSettingDestination.Downloads) },
            )
            NovaMoreActionRow(
                title = stringResource(MR.strings.browse),
                subtitle = stringResource(MR.strings.pref_browse_summary),
                icon = Icons.Outlined.Explore,
                onClick = { onClickSetting(NovaMeSettingDestination.Browse) },
            )
        }
        NovaMoreSection(title = "Services") {
            NovaMoreActionRow(
                title = stringResource(MR.strings.pref_category_tracking),
                subtitle = stringResource(MR.strings.pref_tracking_summary),
                icon = Icons.Outlined.Sync,
                onClick = { onClickSetting(NovaMeSettingDestination.Tracking) },
            )
            NovaMoreActionRow(
                title = stringResource(KMR.strings.pref_category_connections),
                subtitle = stringResource(KMR.strings.pref_connections_summary),
                icon = Icons.Outlined.Link,
                onClick = { onClickSetting(NovaMeSettingDestination.Connections) },
            )
        }
        NovaMoreSection(title = "Donn\u00e9es et confidentialit\u00e9") {
            NovaMoreActionRow(
                title = stringResource(MR.strings.label_data_storage),
                subtitle = stringResource(MR.strings.pref_backup_summary),
                icon = Icons.Outlined.Storage,
                onClick = { onClickSetting(NovaMeSettingDestination.DataAndStorage) },
            )
            NovaMoreActionRow(
                title = stringResource(MR.strings.pref_category_security),
                subtitle = stringResource(MR.strings.pref_security_summary),
                icon = Icons.Outlined.Security,
                onClick = { onClickSetting(NovaMeSettingDestination.Security) },
            )
        }
        NovaMoreSection(title = "Avanc\u00e9") {
            if (showEhSettings) {
                NovaMoreActionRow(
                    title = stringResource(SYMR.strings.pref_category_eh),
                    subtitle = stringResource(SYMR.strings.pref_ehentai_summary),
                    icon = EhAssets.EhLogo,
                    onClick = { onClickSetting(NovaMeSettingDestination.Ehentai) },
                )
            }
            if (showMangadexSettings) {
                NovaMoreActionRow(
                    title = stringResource(SYMR.strings.pref_category_mangadex),
                    subtitle = stringResource(SYMR.strings.pref_mangadex_summary),
                    icon = EhAssets.MangadexLogo,
                    onClick = { onClickSetting(NovaMeSettingDestination.Mangadex) },
                )
            }
            NovaMoreActionRow(
                title = stringResource(MR.strings.pref_category_advanced),
                subtitle = stringResource(MR.strings.pref_advanced_summary),
                icon = Icons.Outlined.Code,
                onClick = { onClickSetting(NovaMeSettingDestination.Advanced) },
            )
        }
        NovaMoreSection(title = "Application") {
            NovaMoreActionRow(
                title = stringResource(MR.strings.pref_category_about),
                subtitle = "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}",
                icon = Icons.Outlined.Info,
                onClick = { onClickSetting(NovaMeSettingDestination.About) },
                accent = NovaVikiColors.Success,
            )
            NovaMoreActionRow(
                title = stringResource(MR.strings.label_help),
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = onClickHelp,
            )
        }
    }
}

@Composable
private fun NovaMoreSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun NovaMoreActionRow(
    title: String,
    icon: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = NovaMoreAccent,
) {
    NovaMoreBaseRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        accent = accent,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = NovaVikiColors.TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        },
    )
}

@Composable
private fun NovaMoreToggleRow(
    title: String,
    subtitle: String,
    icon: Any,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
) {
    NovaMoreBaseRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        accent = if (checked) NovaMoreAccent else NovaVikiColors.TextSecondary,
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChanged,
        ),
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
    )
}

@Composable
private fun NovaMoreBaseRow(
    title: String,
    subtitle: String?,
    icon: Any,
    accent: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = motionScheme.fastSpatialSpec()),
    ) {
        ListItem(
            modifier = Modifier.heightIn(min = NovaVikiDimensions.ListRowMinHeight),
            headlineContent = {
                Text(
                    text = title,
                    color = NovaVikiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = if (!subtitle.isNullOrBlank()) {
                {
                    Text(
                        text = subtitle,
                        color = NovaVikiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                null
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = accent.copy(alpha = 0.17f),
                    contentColor = accent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NovaMoreIcon(
                            icon = icon,
                            tint = accent,
                        )
                    }
                }
            },
            trailingContent = trailing,
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 64.dp),
            color = NovaVikiColors.Outline.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun NovaMoreIcon(
    icon: Any,
    tint: Color,
) {
    when (icon) {
        is ImageVector -> Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        is Painter -> Icon(
            painter = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

private val NovaMoreAccent: Color
    @Composable get() = NovaVikiColors.Accent
