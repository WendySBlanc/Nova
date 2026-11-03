package eu.kanade.presentation.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
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
    // SY -->
    showNavUpdates: Boolean,
    showNavHistory: Boolean,
    // SY <--
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickSettings: () -> Unit,
    onClickBatchAdd: () -> Unit,
    onClickUpdates: () -> Unit,
    onClickHistory: () -> Unit,
    // KMK -->
    onClickLibraryUpdateErrors: () -> Unit,
    // KMK <--
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

    Scaffold(contentWindowInsets = WindowInsets(0)) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaVikiColors.Background),
        ) {
            NovaMeTopBar()
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
                    item(contentType = "me_identity") {
                        NovaMeIdentity(
                            downloadedOnly = downloadedOnly,
                            incognitoMode = incognitoMode,
                        )
                    }
                    item(contentType = "me_quick_actions") {
                        NovaQuickActions(
                            downloadQueueState = downloadQueueState,
                            downloadQueueSubtitle = downloadQueueSubtitle,
                            onClickDownloadQueue = onClickDownloadQueue,
                            onClickStats = onClickStats,
                            onClickCategories = onClickCategories,
                            onClickSettings = onClickSettings,
                        )
                    }
                    item(contentType = "me_sections") {
                        if (expandedLayout) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(NovaVikiDimensions.SectionSpacing),
                                ) {
                                    NovaReadingModesSection(
                                        downloadedOnly = downloadedOnly,
                                        onDownloadedOnlyChange = onDownloadedOnlyChange,
                                        incognitoMode = incognitoMode,
                                        onIncognitoModeChange = onIncognitoModeChange,
                                    )
                                    if (!showNavUpdates || !showNavHistory) {
                                        NovaActivitySection(
                                            showNavUpdates = showNavUpdates,
                                            showNavHistory = showNavHistory,
                                            onClickUpdates = onClickUpdates,
                                            onClickHistory = onClickHistory,
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(NovaVikiDimensions.SectionSpacing),
                                ) {
                                    NovaLibraryToolsSection(
                                        showBatchAdd = showBatchAdd,
                                        onClickLibraryUpdateErrors = onClickLibraryUpdateErrors,
                                        onClickBatchAdd = onClickBatchAdd,
                                    )
                                    NovaApplicationSection(
                                        onClickHelp = { uriHandler.openUri(Constants.URL_HELP) },
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(NovaVikiDimensions.SectionSpacing),
                            ) {
                                NovaReadingModesSection(
                                    downloadedOnly = downloadedOnly,
                                    onDownloadedOnlyChange = onDownloadedOnlyChange,
                                    incognitoMode = incognitoMode,
                                    onIncognitoModeChange = onIncognitoModeChange,
                                )
                                if (!showNavUpdates || !showNavHistory) {
                                    NovaActivitySection(
                                        showNavUpdates = showNavUpdates,
                                        showNavHistory = showNavHistory,
                                        onClickUpdates = onClickUpdates,
                                        onClickHistory = onClickHistory,
                                    )
                                }
                                NovaLibraryToolsSection(
                                    showBatchAdd = showBatchAdd,
                                    onClickLibraryUpdateErrors = onClickLibraryUpdateErrors,
                                    onClickBatchAdd = onClickBatchAdd,
                                )
                                NovaApplicationSection(
                                    onClickHelp = { uriHandler.openUri(Constants.URL_HELP) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaMeTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(NovaVikiColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = NovaVikiDimensions.WideContentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = NovaVikiDimensions.HorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Moi",
                color = NovaVikiColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NovaMeIdentity(
    downloadedOnly: Boolean,
    incognitoMode: Boolean,
) {
    val status = when {
        downloadedOnly && incognitoMode -> "T\u00e9l\u00e9charg\u00e9s uniquement \u2022 Historique suspendu"
        downloadedOnly -> "T\u00e9l\u00e9charg\u00e9s uniquement"
        incognitoMode -> "Historique suspendu"
        else -> "Manhwa & Light Novel"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .clip(RoundedCornerShape(NovaVikiDimensions.ControlCorner))
            .background(NovaVikiColors.Surface)
            .border(1.dp, NovaVikiColors.Outline, RoundedCornerShape(NovaVikiDimensions.ControlCorner)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(NovaVikiColors.Accent),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NovaVikiColors.SurfaceStrong)
                    .border(1.dp, NovaVikiColors.Accent.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "N",
                    color = NovaVikiColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NOVA",
                    color = NovaVikiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
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
    onClickCategories: () -> Unit,
    onClickSettings: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Acc\u00e8s rapides",
            color = NovaVikiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth < 600.dp) 2 else 4
            val gap = 8.dp
            val actionWidth = (maxWidth - gap * (columns - 1).toFloat()) / columns.toFloat()
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                maxItemsInEachRow = columns,
            ) {
                NovaQuickAction(
                    title = "T\u00e9l\u00e9chargements",
                    status = downloadQueueSubtitle,
                    icon = Icons.Outlined.GetApp,
                    accent = when (downloadQueueState) {
                        DownloadQueueState.Stopped -> NovaMoreAccent
                        is DownloadQueueState.Paused -> NovaVikiColors.AccentGold
                        is DownloadQueueState.Downloading -> NovaVikiColors.Success
                    },
                    onClick = onClickDownloadQueue,
                    modifier = Modifier.width(actionWidth),
                )
                NovaQuickAction(
                    title = "Statistiques",
                    icon = Icons.Outlined.QueryStats,
                    accent = NovaVikiColors.AccentWarm,
                    onClick = onClickStats,
                    modifier = Modifier.width(actionWidth),
                )
                NovaQuickAction(
                    title = "Cat\u00e9gories",
                    icon = Icons.AutoMirrored.Outlined.Label,
                    accent = NovaVikiColors.AccentGold,
                    onClick = onClickCategories,
                    modifier = Modifier.width(actionWidth),
                )
                NovaQuickAction(
                    title = "Param\u00e8tres",
                    icon = Icons.Outlined.Settings,
                    accent = NovaVikiColors.Success,
                    onClick = onClickSettings,
                    modifier = Modifier.width(actionWidth),
                )
            }
        }
    }
}

@Composable
private fun NovaQuickAction(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: String? = null,
) {
    Column(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(NovaVikiDimensions.ControlCorner))
            .background(NovaVikiColors.SurfaceStrong)
            .border(1.dp, NovaVikiColors.Outline, RoundedCornerShape(NovaVikiDimensions.ControlCorner))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(
                text = title,
                color = NovaVikiColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!status.isNullOrBlank()) {
                Text(
                    text = status,
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
private fun NovaActivitySection(
    showNavUpdates: Boolean,
    showNavHistory: Boolean,
    onClickUpdates: () -> Unit,
    onClickHistory: () -> Unit,
) {
    NovaMoreSection(title = "Activit\u00e9") {
        if (!showNavUpdates) {
            NovaMoreActionRow(
                title = stringResource(MR.strings.label_recent_updates),
                icon = Icons.Outlined.NewReleases,
                onClick = onClickUpdates,
            )
        }
        if (!showNavHistory) {
            NovaMoreActionRow(
                title = stringResource(MR.strings.label_recent_manga),
                icon = Icons.Outlined.History,
                onClick = onClickHistory,
            )
        }
    }
}

@Composable
private fun NovaLibraryToolsSection(
    showBatchAdd: Boolean,
    onClickLibraryUpdateErrors: () -> Unit,
    onClickBatchAdd: () -> Unit,
) {
    NovaMoreSection(title = stringResource(MR.strings.label_library)) {
        NovaMoreActionRow(
            title = stringResource(KMR.strings.option_label_library_update_errors),
            icon = Icons.Outlined.NewReleases,
            accent = NovaVikiColors.AccentWarm,
            onClick = onClickLibraryUpdateErrors,
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
private fun NovaApplicationSection(
    onClickHelp: () -> Unit,
) {
    NovaMoreSection(title = "Application") {
        NovaMoreActionRow(
            title = stringResource(MR.strings.label_help),
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            onClick = onClickHelp,
        )
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
            color = NovaVikiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            HorizontalDivider(color = NovaVikiColors.Outline)
            content()
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
        modifier = modifier.clickable(onClick = onClick),
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
        modifier = Modifier.clickable { onCheckedChanged(!checked) },
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = NovaVikiDimensions.ListRowMinHeight)
                .background(NovaMoreRow)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
                    .background(accent.copy(alpha = 0.17f)),
                contentAlignment = Alignment.Center,
            ) {
                NovaMoreIcon(
                    icon = icon,
                    tint = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NovaVikiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = NovaVikiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            trailing()
        }
        HorizontalDivider(color = NovaVikiColors.Outline.copy(alpha = 0.72f))
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

private val NovaMoreRow = NovaVikiColors.SurfaceRaised
private val NovaMoreAccent = NovaVikiColors.Accent
