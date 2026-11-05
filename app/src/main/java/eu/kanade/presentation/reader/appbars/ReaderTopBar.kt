package eu.kanade.presentation.reader.appbars

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.reader.MangaTranslationAction
import eu.kanade.presentation.reader.MangaTranslationUiModel
import eu.kanade.presentation.reader.contentDescriptionText
import eu.kanade.presentation.reader.labelText
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
    mangaTranslation: MangaTranslationUiModel?,
    onMangaTranslationAction: () -> Unit,
    onRetranslateCurrentPage: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val compactHeight = configuration.screenHeightDp < 420 ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val barMinHeight = if (compactHeight) 56.dp else 64.dp
    var moreExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = barMinHeight)
            .padding(horizontal = 4.dp, vertical = if (compactHeight) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = navigateUp,
            modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left_24dp),
                contentDescription = stringResource(MR.strings.action_close),
                modifier = Modifier.size(24.dp),
                tint = NovaVikiColors.TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClickTitle)
                .heightIn(min = NovaVikiDimensions.MinimumTouchTarget)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = mangaTitle.orEmpty(),
                color = NovaVikiColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = chapterTitle.orEmpty(),
                color = NovaVikiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = onToggleBookmarked,
            modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
        ) {
            Icon(
                imageVector = if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(
                    if (bookmarked) {
                        MR.strings.action_remove_bookmark
                    } else {
                        MR.strings.action_bookmark
                    },
                ),
                modifier = Modifier.size(24.dp),
                tint = if (bookmarked) NovaVikiColors.Accent else NovaVikiColors.TextPrimary,
            )
        }

        if (
            mangaTranslation != null ||
            onRetranslateCurrentPage != null ||
            onOpenInWebView != null ||
            onOpenInBrowser != null ||
            onShare != null
        ) {
            Box {
                IconButton(
                    onClick = { moreExpanded = true },
                    modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Plus d'actions",
                        modifier = Modifier.size(24.dp),
                        tint = NovaVikiColors.TextPrimary,
                    )
                }
                DropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = { moreExpanded = false },
                ) {
                    mangaTranslation?.let { translation ->
                        val description = translation.contentDescriptionText()
                        DropdownMenuItem(
                            text = { Text(translation.labelText()) },
                            enabled = translation.enabled,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (translation.action == MangaTranslationAction.Cancel) {
                                        Icons.Outlined.Close
                                    } else {
                                        Icons.Outlined.Public
                                    },
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                if (!translation.enabled) return@DropdownMenuItem
                                moreExpanded = false
                                onMangaTranslationAction()
                            },
                            modifier = Modifier.semantics {
                                stateDescription = description
                            },
                        )
                    }
                    onRetranslateCurrentPage?.let { retranslateCurrentPage ->
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.manga_translation_retranslate_page)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Refresh, contentDescription = null)
                            },
                            onClick = {
                                moreExpanded = false
                                retranslateCurrentPage()
                            },
                        )
                    }
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
    }
}
