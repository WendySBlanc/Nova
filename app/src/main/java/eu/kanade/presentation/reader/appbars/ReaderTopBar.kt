package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderTopBar(
    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    onClickTitle: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    onOpenInBrowser: (() -> Unit)?,
    onShare: (() -> Unit)?,
    readerLensAvailable: Boolean,
    readerLensEnabled: Boolean,
    readerLensWorking: Boolean,
    onReaderLensToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var moreExpanded by remember { mutableStateOf(false) }
    val readerLensStateDescription = stringResource(
        if (readerLensEnabled) MR.strings.reader_lens_active else MR.strings.reader_lens_inactive,
    )
    val readerLensLabel = stringResource(MR.strings.reader_lens_realtime)

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = navigateUp) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left_24dp),
                    contentDescription = stringResource(MR.strings.action_close),
                )
            }
        },
        title = {
            Column(
                modifier = Modifier
                    .clickable(onClick = onClickTitle)
                    .heightIn(min = NovaVikiDimensions.MinimumTouchTarget)
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = mangaTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = chapterTitle.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            if (readerLensAvailable) {
                IconToggleButton(
                    checked = readerLensEnabled,
                    onCheckedChange = { onReaderLensToggle() },
                    modifier = Modifier.semantics {
                        role = Role.Switch
                        contentDescription = readerLensLabel
                        stateDescription = readerLensStateDescription
                    },
                ) {
                    if (readerLensWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = if (readerLensEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            IconButton(onClick = onToggleBookmarked) {
                Icon(
                    imageVector = if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(
                        if (bookmarked) {
                            MR.strings.action_remove_bookmark
                        } else {
                            MR.strings.action_bookmark
                        },
                    ),
                    tint = if (bookmarked) MaterialTheme.colorScheme.primary else NovaVikiColors.TextPrimary,
                )
            }

            if (
                onOpenInWebView != null ||
                onOpenInBrowser != null ||
                onShare != null
            ) {
                Box {
                    IconButton(onClick = { moreExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Plus d'actions",
                        )
                    }
                    DropdownMenu(
                        expanded = moreExpanded,
                        onDismissRequest = { moreExpanded = false },
                    ) {
                        onOpenInWebView?.let { openWebView ->
                            DropdownMenuItem(
                                text = { Text("Ouvrir le site") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Public, contentDescription = null)
                                },
                                onClick = {
                                    moreExpanded = false
                                    openWebView()
                                },
                            )
                        }
                        onOpenInBrowser?.let { openBrowser ->
                            DropdownMenuItem(
                                text = { Text("Ouvrir dans le navigateur") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                                },
                                onClick = {
                                    moreExpanded = false
                                    openBrowser()
                                },
                            )
                        }
                        onShare?.let { share ->
                            DropdownMenuItem(
                                text = { Text("Partager") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, contentDescription = null)
                                },
                                onClick = {
                                    moreExpanded = false
                                    share()
                                },
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
