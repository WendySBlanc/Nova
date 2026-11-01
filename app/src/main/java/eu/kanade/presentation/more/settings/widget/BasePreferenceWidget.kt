package eu.kanade.presentation.more.settings.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import eu.kanade.presentation.more.settings.LocalPreferenceMinHeight
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun BasePreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subcomponent: @Composable (ColumnScope.() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
) {
    val highlighted = LocalPreferenceHighlighted.current
    val minHeight = LocalPreferenceMinHeight.current
    Row(
        modifier = modifier
            .highlightBackground(highlighted)
            .sizeIn(minHeight = minHeight)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
                content = { icon() },
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = if (icon != null) 12.dp else 4.dp,
                    top = PrefsVerticalPadding,
                    end = 12.dp,
                    bottom = PrefsVerticalPadding,
                ),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            subcomponent?.invoke(this)
        }
        if (widget != null) {
            Box(
                modifier = Modifier.padding(end = 4.dp),
                content = { widget() },
            )
        }
    }
}

@Composable
internal fun Modifier.highlightBackground(highlighted: Boolean): Modifier {
    var highlightFlag by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (highlighted) {
            highlightFlag = true
            delay(1.6.seconds)
            highlightFlag = false
        }
    }
    val highlight by animateColorAsState(
        targetValue = if (highlightFlag) {
            MaterialTheme.colorScheme.surfaceTint.copy(alpha = .12f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(180),
        label = "highlight",
    )
    return this.background(color = highlight)
}

internal val TrailingWidgetBuffer = 16.dp
internal val PrefsHorizontalPadding = 16.dp
internal val PrefsVerticalPadding = 12.dp
internal val TitleFontSize = 16.sp
