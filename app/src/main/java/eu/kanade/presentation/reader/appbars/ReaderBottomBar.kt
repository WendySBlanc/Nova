package eu.kanade.presentation.reader.appbars

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.reader.readerDestinationFromSlider
import eu.kanade.presentation.reader.resolveNovaReaderProgress
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Slider
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderBottomBar(
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    onClickScrollToTop: () -> Unit,
    onClickChapterList: () -> Unit,
    onClickSettings: () -> Unit,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    currentPage: Int,
    currentPageText: String,
    totalPages: Int,
    isTextReader: Boolean,
    showProgressSlider: Boolean,
    onPageIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val compactHeight = configuration.screenHeightDp < 420 ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val barMinHeight = if (compactHeight) 56.dp else 64.dp
    val iconColor = NovaVikiColors.TextPrimary
    val disabledIconColor = NovaVikiColors.TextSecondary.copy(alpha = 0.38f)

    Column(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {},
    ) {
        if (showProgressSlider && (isTextReader || totalPages > 1)) {
            ReaderProgressSlider(
                currentPage = currentPage,
                currentPageText = currentPageText,
                totalPages = totalPages,
                isTextReader = isTextReader,
                onPageIndexChange = onPageIndexChange,
            )
            HorizontalDivider(color = NovaVikiColors.Outline.copy(alpha = 0.72f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = barMinHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderDockButton(
                iconRes = R.drawable.ic_chevron_left_24dp,
                contentDescription = stringResource(MR.strings.action_previous_chapter),
                onClick = onPreviousChapter,
                enabled = enabledPrevious,
                tint = if (enabledPrevious) iconColor else disabledIconColor,
            )
            ReaderDockButton(
                iconRes = R.drawable.ic_vertical_align_top_24dp,
                contentDescription = stringResource(MR.strings.action_move_to_top),
                onClick = onClickScrollToTop,
                tint = iconColor,
            )
            Surface(
                shape = RoundedCornerShape(NovaVikiDimensions.CardCorner),
                color = NovaVikiColors.Accent.copy(alpha = 0.12f),
                contentColor = NovaVikiColors.Accent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    NovaVikiColors.Accent.copy(alpha = 0.5f),
                ),
            ) {
                ReaderDockButton(
                    iconRes = R.drawable.ic_format_align_right_24dp,
                    contentDescription = stringResource(MR.strings.chapters),
                    onClick = onClickChapterList,
                    tint = NovaVikiColors.Accent,
                )
            }
            ReaderDockButton(
                iconRes = R.drawable.ic_settings_24dp,
                contentDescription = stringResource(MR.strings.action_settings),
                onClick = onClickSettings,
                tint = iconColor,
            )
            ReaderDockButton(
                iconRes = R.drawable.ic_chevron_right_24dp,
                contentDescription = stringResource(MR.strings.action_next_chapter),
                onClick = onNextChapter,
                enabled = enabledNext,
                tint = if (enabledNext) iconColor else disabledIconColor,
            )
        }
    }
}

@Composable
private fun ReaderProgressSlider(
    currentPage: Int,
    currentPageText: String,
    totalPages: Int,
    isTextReader: Boolean,
    onPageIndexChange: (Int) -> Unit,
) {
    val progress = resolveNovaReaderProgress(currentPage, totalPages, isTextReader)
    var sliderValue by remember {
        mutableIntStateOf(progress.sliderValue)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val dragged by interactionSource.collectIsDraggedAsState()
    ReaderSliderHaptics(sliderValue, dragged)
    LaunchedEffect(progress.sliderValue, dragged) {
        if (!dragged) sliderValue = progress.sliderValue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NovaVikiDimensions.MinimumTouchTarget)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReaderProgressLabel(
            text = if (dragged) {
                resolveNovaReaderProgress(sliderValue, totalPages, isTextReader).label
            } else {
                currentPageText.ifBlank { progress.label.substringBefore(" /") }
            },
            color = NovaVikiColors.Accent,
        )
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            value = sliderValue,
            valueRange = progress.sliderRange,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                onPageIndexChange(readerDestinationFromSlider(sliderValue, isTextReader))
            },
            colors = readerSliderColors(),
            interactionSource = interactionSource,
        )
        ReaderProgressLabel(
            text = if (isTextReader) "100%" else totalPages.toString(),
            color = NovaVikiColors.TextSecondary,
        )
    }
}

@Composable
private fun ReaderSliderHaptics(
    sliderValue: Int,
    dragged: Boolean,
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(sliderValue, dragged) {
        if (dragged) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

@Composable
private fun readerSliderColors() = SliderDefaults.colors(
    thumbColor = NovaVikiColors.Accent,
    activeTrackColor = NovaVikiColors.Accent,
    inactiveTrackColor = NovaVikiColors.Outline,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)

@Composable
private fun ReaderProgressLabel(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier.width(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun ReaderDockButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = tint,
        )
    }
}
