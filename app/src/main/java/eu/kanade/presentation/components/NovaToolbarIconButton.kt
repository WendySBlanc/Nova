package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.NovaSignalDimensions

@Composable
internal fun NovaToolbarIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(5.dp)
    IconButton(onClick = onClick, modifier = Modifier.size(NovaSignalDimensions.MinimumTouchTarget)) {
        Box(
            modifier = Modifier.size(35.dp).background(colors.surface, shape).border(1.dp, colors.outlineVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = colors.onSurface, modifier = Modifier.size(18.dp))
        }
    }
}
