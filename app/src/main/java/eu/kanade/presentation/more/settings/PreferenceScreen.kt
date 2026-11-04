package eu.kanade.presentation.more.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaSpatialBackdrop
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .novaSpatialBackdrop(
                background = NovaVikiColors.Background,
                primary = NovaVikiColors.Accent,
                secondary = NovaVikiColors.AccentWarm,
                tertiary = NovaVikiColors.AccentGold,
                wireColor = NovaVikiColors.OutlineStrong,
                wireAlpha = 0.055f,
                planeAlpha = 0.022f,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        ScrollbarLazyColumn(
            modifier = Modifier
                .widthIn(max = NovaVikiDimensions.CompactContentMaxWidth)
                .fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
        ) {
            items.fastForEachIndexed { i, preference ->
                when (preference) {
                    is Preference.PreferenceGroup -> {
                        if (!preference.enabled) return@fastForEachIndexed

                        item {
                            Column {
                                PreferenceGroupHeader(title = preference.title)
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)),
                                ) {
                                    preference.preferenceItems.forEach { item ->
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            PreferenceItem(
                                                item = item,
                                                highlightKey = highlightKey,
                                            )
                                        }
                                    }
                                }
                            }
                            if (i < items.lastIndex) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    is Preference.PreferenceItem<*, *> -> item {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)),
                        ) {
                            PreferenceItem(
                                item = preference,
                                highlightKey = highlightKey,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return indexOfFirst { preference ->
        when (preference) {
            is Preference.PreferenceGroup -> {
                preference.title == highlightKey ||
                    preference.preferenceItems.any { item -> item.title == highlightKey }
            }
            is Preference.PreferenceItem<*, *> -> preference.title == highlightKey
        }
    }
}
