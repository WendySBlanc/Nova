package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.NovaSignalDimensions

@Suppress("UNUSED_PARAMETER")
fun Modifier.novaHollowPacket(
    background: Color,
    outline: Color,
    accent: Color,
    secondary: Color,
    corner: Dp = NovaSignalDimensions.ControlCorner,
    particleCount: Int = 10,
): Modifier {
    val shape = RoundedCornerShape(corner)
    return clip(shape)
        .background(background)
        .border(1.dp, outline.copy(alpha = 0.62f), shape)
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun NovaFluidFixedTabRow(
    selectedIndex: Int,
    itemCount: Int,
    onSelect: (Int) -> Unit,
    background: Color,
    outline: Color,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    itemContent: @Composable BoxScope.(index: Int, selected: Boolean) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .heightIn(min = NovaSignalDimensions.MinimumTouchTarget),
    ) {
        repeat(itemCount) { index ->
            val selected = selectedIndex == index
            SegmentedButton(
                selected = selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, itemCount),
                modifier = Modifier.weight(1f),
                icon = {},
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    content = { itemContent(index, selected) },
                )
            }
        }
    }
}

@Composable
fun NovaLiquidTabLabel(
    selected: Boolean,
    accent: Color,
    outline: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
