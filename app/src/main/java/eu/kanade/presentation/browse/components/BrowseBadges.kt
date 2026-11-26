package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import eu.kanade.presentation.theme.NovaIcons
import tachiyomi.presentation.core.components.Badge

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = NovaIcons.Library,
        )
    }
}
