package eu.kanade.presentation.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import eu.kanade.presentation.manga.DownloadAction
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DownloadDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = /* KMK --> */ DefaultDropdownMenuOffset, /* KMK <-- */
    actionCounts: Map<DownloadAction, Int> = emptyMap(),
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        content = {
            DownloadDropdownMenuItems(
                onDismissRequest = onDismissRequest,
                onDownloadClicked = onDownloadClicked,
                actionCounts = actionCounts,
            )
        },
    )
}

@Composable
private fun DownloadDropdownMenuItems(
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    actionCounts: Map<DownloadAction, Int>,
) {
    val options = persistentListOf(
        DownloadAction.NEXT_1_CHAPTER to pluralStringResource(MR.plurals.download_amount, 1, 1),
        DownloadAction.NEXT_5_CHAPTERS to pluralStringResource(MR.plurals.download_amount, 5, 5),
        DownloadAction.UNREAD_CHAPTERS to stringResource(MR.strings.download_unread),
        DownloadAction.ALL_CHAPTERS to stringResource(MR.strings.download_all),
    )

    options.forEach { (downloadAction, string) ->
        val count = actionCounts[downloadAction]
        DropdownMenuItem(
            text = { Text(text = string) },
            trailingIcon = count?.let {
                {
                    Text(
                        text = it.toString(),
                        maxLines = 1,
                    )
                }
            },
            enabled = count == null || count > 0,
            onClick = {
                onDownloadClicked(downloadAction)
                onDismissRequest()
            },
        )
    }
}
