package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.tachiyomi.data.translation.MangaTranslationState
import eu.kanade.tachiyomi.data.translation.isActive
import eu.kanade.tachiyomi.data.translation.progress
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

enum class MangaTranslationAction {
    Translate,
    Cancel,
    ShowOriginal,
    ShowTranslation,
}

enum class MangaTranslationUiText {
    TranslateChapter,
    TranslateChapterDescription,
    TranslationInProgress,
    TranslationProgress,
    CancelTranslationDescription,
    CancelTranslationProgressDescription,
    ShowOriginal,
    ShowOriginalDescription,
    ShowTranslation,
    ShowTranslationDescription,
    ConfigureServerDescription,
}

data class MangaTranslationUiModel(
    val action: MangaTranslationAction,
    val label: MangaTranslationUiText,
    val contentDescription: MangaTranslationUiText,
    val enabled: Boolean = true,
    val completedPages: Int? = null,
    val totalPages: Int? = null,
)

fun mangaTranslationUiModel(
    available: Boolean,
    configured: Boolean,
    state: MangaTranslationState,
    hasTranslation: Boolean,
    showingTranslation: Boolean,
): MangaTranslationUiModel? {
    if (!available) return null
    val progress = state.progress
    return when {
        state.isActive -> MangaTranslationUiModel(
            action = MangaTranslationAction.Cancel,
            label = if (progress == null) {
                MangaTranslationUiText.TranslationInProgress
            } else {
                MangaTranslationUiText.TranslationProgress
            },
            contentDescription = if (progress == null) {
                MangaTranslationUiText.CancelTranslationDescription
            } else {
                MangaTranslationUiText.CancelTranslationProgressDescription
            },
            completedPages = progress?.first,
            totalPages = progress?.second,
        )
        hasTranslation && showingTranslation -> null
        hasTranslation -> MangaTranslationUiModel(
            action = MangaTranslationAction.ShowTranslation,
            label = MangaTranslationUiText.ShowTranslation,
            contentDescription = MangaTranslationUiText.ShowTranslationDescription,
        )
        else -> MangaTranslationUiModel(
            action = MangaTranslationAction.Translate,
            label = MangaTranslationUiText.TranslateChapter,
            contentDescription = if (configured) {
                MangaTranslationUiText.TranslateChapterDescription
            } else {
                MangaTranslationUiText.ConfigureServerDescription
            },
        )
    }
}

@Composable
fun MangaTranslationProgress(
    model: MangaTranslationUiModel,
    modifier: Modifier = Modifier,
) {
    if (model.action != MangaTranslationAction.Cancel) return
    val label = model.labelText()
    val description = model.contentDescriptionText()
    val completed = model.completedPages
    val total = model.totalPages

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = description
                progressBarRangeInfo = if (completed != null && total != null && total > 0) {
                    ProgressBarRangeInfo(completed.toFloat().div(total).coerceIn(0f, 1f), 0f..1f)
                } else {
                    ProgressBarRangeInfo.Indeterminate
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = NovaVikiColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (completed != null && total != null && total > 0) {
            LinearProgressIndicator(
                progress = { completed.toFloat().div(total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = NovaVikiColors.Accent,
                trackColor = NovaVikiColors.Outline,
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = NovaVikiColors.Accent,
                trackColor = NovaVikiColors.Outline,
            )
        }
    }
}

@Composable
fun MangaTranslationUiModel.labelText(): String = when (label) {
    MangaTranslationUiText.TranslateChapter -> stringResource(MR.strings.manga_translation_translate_chapter)
    MangaTranslationUiText.TranslationInProgress -> stringResource(MR.strings.manga_translation_in_progress)
    MangaTranslationUiText.TranslationProgress -> stringResource(
        MR.strings.manga_translation_progress,
        completedPages ?: 0,
        totalPages ?: 0,
    )
    MangaTranslationUiText.ShowOriginal -> stringResource(MR.strings.manga_translation_show_original)
    MangaTranslationUiText.ShowTranslation -> stringResource(MR.strings.manga_translation_show_translation)
    else -> contentDescriptionText()
}

@Composable
fun MangaTranslationUiModel.contentDescriptionText(): String = when (contentDescription) {
    MangaTranslationUiText.TranslateChapterDescription ->
        stringResource(MR.strings.manga_translation_translate_chapter_description)
    MangaTranslationUiText.CancelTranslationDescription ->
        stringResource(MR.strings.manga_translation_cancel_description)
    MangaTranslationUiText.CancelTranslationProgressDescription -> stringResource(
        MR.strings.manga_translation_cancel_progress_description,
        completedPages ?: 0,
        totalPages ?: 0,
    )
    MangaTranslationUiText.ShowOriginalDescription ->
        stringResource(MR.strings.manga_translation_show_original_description)
    MangaTranslationUiText.ShowTranslationDescription ->
        stringResource(MR.strings.manga_translation_show_translation_description)
    MangaTranslationUiText.ConfigureServerDescription ->
        stringResource(MR.strings.manga_translation_configure_server_description)
    else -> labelText()
}
