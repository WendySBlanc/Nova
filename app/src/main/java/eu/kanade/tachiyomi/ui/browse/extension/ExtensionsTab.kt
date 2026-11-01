package eu.kanade.tachiyomi.ui.browse.extension

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined._18UpRating
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.settings.screen.browse.ExtensionReposScreen
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.collections.immutable.toPersistentList
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun extensionsTab(
    extensionsScreenModel: ExtensionsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow

    val state by extensionsScreenModel.state.collectAsState()
    var bulkActionToConfirm by remember { mutableStateOf<BulkExtensionAction?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedExtensionKeys by remember { mutableStateOf(emptySet<String>()) }

    val visibleExtensions = state.items.values
        .flatten()
        .map(ExtensionUiModel.Item::extension)
        .distinctBy(Extension::selectionKey)
    val selectedExtensions = visibleExtensions.filter { it.selectionKey() in selectedExtensionKeys }
    val installableSelected = selectedExtensions.filter {
        it is Extension.Available || (it is Extension.Installed && it.hasUpdate)
    }
    val uninstallableSelected = selectedExtensions.filterNot { it is Extension.Available }

    val clearSelection = {
        selectionMode = false
        selectedExtensionKeys = emptySet()
    }
    val toggleSelection: (Extension) -> Unit = { extension ->
        val key = extension.selectionKey()
        selectedExtensionKeys = if (key in selectedExtensionKeys) {
            selectedExtensionKeys - key
        } else {
            selectedExtensionKeys + key
        }
        selectionMode = selectedExtensionKeys.isNotEmpty()
    }

    val actions = buildList<AppBar.AppBarAction> {
        // KMK -->
        add(
            AppBar.Action(
                title = stringResource(KMR.strings.action_toggle_nsfw_only),
                icon = Icons.Outlined._18UpRating,
                iconTint = if (state.nsfwOnly) MaterialTheme.colorScheme.error else LocalContentColor.current,
                onClick = { extensionsScreenModel.toggleNsfwOnly() },
            ),
        )
        add(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.action_webview_refresh),
                onClick = extensionsScreenModel::findAvailableExtensions,
            ),
        )
        // KMK <--
        if (state.isBulkInstalling) {
            add(
                AppBar.OverflowAction(
                    title = stringResource(
                        MR.strings.action_cancel_install_all_extensions,
                        state.bulkInstallCompleted,
                        state.bulkInstallTotal,
                    ),
                    onClick = extensionsScreenModel::cancelInstallAllExtensions,
                ),
            )
        } else if (visibleExtensions.isNotEmpty()) {
            add(
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.action_select_extensions),
                    onClick = { selectionMode = true },
                ),
            )
        }
        add(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.action_filter),
                onClick = { navigator.push(ExtensionFilterScreen()) },
            ),
        )
        add(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.label_extension_repos),
                onClick = { navigator.push(ExtensionReposScreen()) },
            ),
        )
        add(
            AppBar.OverflowAction(
                title = stringResource(MR.strings.action_share_installed_extensions),
                onClick = extensionsScreenModel::shareInstalledExtensions,
            ),
        )
    }.toPersistentList()

    return TabContent(
        titleRes = MR.strings.label_extensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = actions,
        topBar = if (selectionMode) {
            {
                val selectionActions = buildList<AppBar.AppBarAction> {
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_select_all),
                            icon = Icons.Outlined.SelectAll,
                            onClick = {
                                selectedExtensionKeys = visibleExtensions
                                    .map(Extension::selectionKey)
                                    .toSet()
                            },
                        ),
                    )
                    if (installableSelected.isNotEmpty()) {
                        add(
                            AppBar.Action(
                                title = stringResource(
                                    MR.strings.action_install_selected_extensions,
                                    installableSelected.size,
                                ),
                                icon = Icons.Outlined.GetApp,
                                onClick = {
                                    bulkActionToConfirm = BulkExtensionAction.InstallSelected
                                },
                            ),
                        )
                    }
                    if (uninstallableSelected.isNotEmpty()) {
                        add(
                            AppBar.Action(
                                title = stringResource(
                                    MR.strings.action_uninstall_selected_extensions,
                                    uninstallableSelected.size,
                                ),
                                icon = Icons.Outlined.Delete,
                                onClick = {
                                    bulkActionToConfirm = BulkExtensionAction.UninstallSelected
                                },
                            ),
                        )
                    }
                }.toPersistentList()
                AppBar(
                    titleContent = { Text(selectedExtensionKeys.size.toString()) },
                    actions = { AppBarActions(selectionActions) },
                    isActionMode = true,
                    onCancelActionMode = clearSelection,
                )
            }
        } else {
            null
        },
        content = { contentPadding, _ ->
            BackHandler(enabled = selectionMode || state.searchQuery != null) {
                if (selectionMode) {
                    clearSelection()
                } else {
                    extensionsScreenModel.search(null)
                }
            }
            ExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                selectionMode = selectionMode,
                selectedExtensionKeys = selectedExtensionKeys,
                onSelectionChange = toggleSelection,
                onLongClickItem = { extension ->
                    selectionMode = true
                    toggleSelection(extension)
                },
                onClickItemCancel = extensionsScreenModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsScreenModel::updateAllExtensions,
                onOpenWebView = { extension ->
                    extension.sources.getOrNull(0)?.let {
                        navigator.push(
                            WebViewScreen(
                                url = it.baseUrl,
                                initialTitle = it.name,
                                sourceId = it.id,
                            ),
                        )
                    }
                },
                onInstallExtension = extensionsScreenModel::installExtension,
                onOpenExtension = { navigator.push(ExtensionDetailsScreen(it.pkgName)) },
                onTrustExtension = { extensionsScreenModel.trustExtension(it) },
                onUninstallExtension = { extensionsScreenModel.uninstallExtension(it) },
                onUpdateExtension = extensionsScreenModel::updateExtension,
                onRefresh = extensionsScreenModel::findAvailableExtensions,
            )

            bulkActionToConfirm?.let { action ->
                val isInstall = action == BulkExtensionAction.InstallSelected
                val count = if (isInstall) installableSelected.size else uninstallableSelected.size
                BulkExtensionsConfirmation(
                    title = stringResource(
                        if (isInstall) {
                            MR.strings.ext_install_selected_confirmation_title
                        } else {
                            MR.strings.ext_uninstall_selected_confirmation_title
                        },
                    ),
                    message = stringResource(
                        if (isInstall) {
                            MR.strings.ext_install_selected_confirmation_message
                        } else {
                            MR.strings.ext_uninstall_selected_confirmation_message
                        },
                        count,
                    ),
                    confirmLabel = stringResource(
                        if (isInstall) MR.strings.ext_install else MR.strings.ext_uninstall,
                    ),
                    destructive = !isInstall,
                    onClickConfirm = {
                        if (isInstall) {
                            extensionsScreenModel.installExtensions(installableSelected)
                        } else {
                            extensionsScreenModel.uninstallExtensions(uninstallableSelected)
                        }
                        clearSelection()
                    },
                    onDismissRequest = { bulkActionToConfirm = null },
                )
            }
        },
    )
}

private enum class BulkExtensionAction {
    InstallSelected,
    UninstallSelected,
}

@Composable
private fun BulkExtensionsConfirmation(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    onClickConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onClickConfirm()
                },
            ) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
