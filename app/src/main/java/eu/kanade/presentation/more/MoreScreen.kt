package eu.kanade.presentation.more

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.NovaToolbarIconButton
import eu.kanade.presentation.home.NovaMeSettingDestination
import eu.kanade.presentation.home.NovaSettingsGroup
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.theme.NovaIcons
import eu.kanade.presentation.theme.NovaVikiDimensions
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
    downloadQueueState: DownloadQueueState,
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
    val showBatchAdd = remember {
        runCatching {
            Injekt.get<ExhPreferences>().isHentaiEnabled().get() ||
                Injekt.get<DelegateSourcePreferences>().delegateSources().get()
        }.getOrDefault(false)
    }
    val downloadStatus = when (downloadQueueState) {
        DownloadQueueState.Stopped -> null
        is DownloadQueueState.Paused -> stringResource(MR.strings.paused)
        is DownloadQueueState.Downloading -> pluralStringResource(
            MR.plurals.download_queue_summary,
            downloadQueueState.pending,
            downloadQueueState.pending,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(MR.strings.label_settings),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    NovaToolbarIconButton(NovaIcons.Search, stringResource(MR.strings.action_search), onClickSettingsSearch)
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = NovaVikiDimensions.WideContentMaxWidth)
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                item(key = "quick_modes") {
                    NovaSettingsSection(stringResource(MR.strings.nova_settings_quick_modes)) {
                        NovaSettingsAction(
                            title = stringResource(MR.strings.label_download_queue),
                            icon = NovaIcons.Download,
                            subtitle = downloadStatus,
                            onClick = onClickDownloadQueue,
                        )
                        NovaSettingsSwitch(
                            title = stringResource(MR.strings.label_downloaded_only),
                            icon = Icons.Outlined.CloudOff,
                            checked = downloadedOnly,
                            onChange = onDownloadedOnlyChange,
                        )
                        NovaSettingsSwitch(
                            title = stringResource(MR.strings.pref_incognito_mode),
                            icon = Icons.Outlined.VisibilityOff,
                            checked = incognitoMode,
                            onChange = onIncognitoModeChange,
                        )
                    }
                }
                items(NovaSettingsGroup.entries, key = { it.name }, contentType = { "settings_group" }) { group ->
                    NovaSettingsSection(stringResource(group.label)) {
                        group.destinations(showEhSettings, showMangadexSettings).forEach { destination ->
                            NovaSettingsAction(
                                title = stringResource(destination.label),
                                icon = destination.icon,
                                subtitle = when (destination) {
                                    NovaMeSettingDestination.Appearance -> stringResource(MR.strings.nova_settings_home_layout)
                                    NovaMeSettingDestination.About -> AboutScreen.getVersionName(withBuildDate = false)
                                    else -> null
                                },
                                onClick = { onClickSetting(destination) },
                            )
                        }
                        when (group) {
                            NovaSettingsGroup.Reading -> {
                                NovaSettingsAction(
                                    title = stringResource(MR.strings.categories),
                                    icon = Icons.AutoMirrored.Outlined.Label,
                                    onClick = onClickCategories,
                                )
                                NovaSettingsAction(
                                    title = stringResource(MR.strings.label_stats),
                                    icon = Icons.Outlined.QueryStats,
                                    onClick = onClickStats,
                                )
                                if (showBatchAdd) {
                                    NovaSettingsAction(
                                        title = stringResource(SYMR.strings.eh_batch_add),
                                        icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                                        onClick = onClickBatchAdd,
                                    )
                                }
                            }
                            NovaSettingsGroup.About -> NovaSettingsAction(
                                title = stringResource(MR.strings.label_help),
                                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                onClick = { uriHandler.openUri(Constants.URL_HELP) },
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                .semantics { heading() },
        )
        content()
    }
}

@Composable
private fun NovaSettingsAction(title: String, icon: Any, onClick: () -> Unit, subtitle: String? = null) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val nudge by animateFloatAsState(
        targetValue = if (pressed) direction else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "setting-chevron",
    )
    NovaSettingsRow(
        title = title,
        icon = icon,
        subtitle = subtitle,
        modifier = Modifier.clickable(
            interactionSource = interactions,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onClick,
        ),
        trailing = {
            Icon(
                NovaIcons.Next,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { translationX = nudge * 4.dp.toPx() },
            )
        },
    )
}

@Composable
private fun NovaSettingsSwitch(title: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    NovaSettingsRow(
        title = title,
        icon = icon,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        trailing = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun NovaSettingsRow(
    title: String,
    icon: Any,
    modifier: Modifier,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    ListItem(
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            val tint = MaterialTheme.colorScheme.onSurfaceVariant
            when (icon) {
                is ImageVector -> Icon(icon, contentDescription = null, tint = tint)
                is Painter -> Icon(icon, contentDescription = null, tint = tint)
            }
        },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

private val NovaSettingsGroup.label
    get() = when (this) {
        NovaSettingsGroup.Interface -> MR.strings.nova_settings_interface
        NovaSettingsGroup.Reading -> MR.strings.nova_settings_reading
        NovaSettingsGroup.Sources -> MR.strings.nova_settings_sources
        NovaSettingsGroup.Storage -> MR.strings.nova_settings_storage
        NovaSettingsGroup.Advanced -> MR.strings.pref_category_advanced
        NovaSettingsGroup.About -> MR.strings.app_name
    }

private val NovaMeSettingDestination.label
    get() = when (this) {
        NovaMeSettingDestination.Appearance -> MR.strings.pref_category_appearance
        NovaMeSettingDestination.Reader -> MR.strings.pref_category_reader
        NovaMeSettingDestination.Library -> MR.strings.pref_category_library
        NovaMeSettingDestination.Downloads -> MR.strings.pref_category_downloads
        NovaMeSettingDestination.Browse -> MR.strings.browse
        NovaMeSettingDestination.Tracking -> MR.strings.pref_category_tracking
        NovaMeSettingDestination.Connections -> KMR.strings.pref_category_connections
        NovaMeSettingDestination.DataAndStorage -> MR.strings.label_data_storage
        NovaMeSettingDestination.Security -> MR.strings.pref_category_security
        NovaMeSettingDestination.Ehentai -> SYMR.strings.pref_category_eh
        NovaMeSettingDestination.Mangadex -> SYMR.strings.pref_category_mangadex
        NovaMeSettingDestination.Advanced -> MR.strings.pref_category_advanced
        NovaMeSettingDestination.About -> MR.strings.pref_category_about
    }

private val NovaMeSettingDestination.icon: Any
    get() = when (this) {
        NovaMeSettingDestination.Appearance -> Icons.Outlined.Palette
        NovaMeSettingDestination.Reader -> Icons.AutoMirrored.Outlined.ChromeReaderMode
        NovaMeSettingDestination.Library -> NovaIcons.Library
        NovaMeSettingDestination.Downloads -> NovaIcons.Download
        NovaMeSettingDestination.Browse -> Icons.Outlined.Explore
        NovaMeSettingDestination.Tracking -> Icons.Outlined.Sync
        NovaMeSettingDestination.Connections -> Icons.Outlined.Link
        NovaMeSettingDestination.DataAndStorage -> Icons.Outlined.Storage
        NovaMeSettingDestination.Security -> Icons.Outlined.Security
        NovaMeSettingDestination.Ehentai -> EhAssets.EhLogo
        NovaMeSettingDestination.Mangadex -> EhAssets.MangadexLogo
        NovaMeSettingDestination.Advanced -> Icons.Outlined.Code
        NovaMeSettingDestination.About -> Icons.Outlined.Info
    }
