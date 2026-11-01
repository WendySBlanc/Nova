package eu.kanade.presentation.reader

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ReaderPageIndicator(
    // SY -->
    currentPage: String,
    // SY <--
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    if (currentPage.isEmpty() || totalPages <= 0) return

    val isTextProgress = currentPage.endsWith("%")
    val pageNumber = currentPage
        .takeUnless { isTextProgress }
        ?.substringBefore("-")
        ?.trim()
        ?.toIntOrNull()
        ?.coerceIn(1, totalPages)
    val progress = if (isTextProgress) {
        currentPage
    } else {
        pageNumber
            ?.let { ((it.toFloat() / totalPages.toFloat()) * 100).roundToInt().coerceIn(0, 100) }
            ?.let { "$it%" }
            .orEmpty()
    }
    val text = if (isTextProgress) currentPage else "$currentPage/$totalPages"
    var time by remember { mutableStateOf(currentReaderTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            time = currentReaderTime()
        }
    }

    val backgroundColor = MaterialTheme.colorScheme
        .surfaceColorAtElevation(3.dp)
        .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
    val configuration = LocalConfiguration.current
    val compactHeight = configuration.screenHeightDp < 420 ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactWidth = configuration.screenWidthDp < 360
    val tabletWidth = configuration.screenWidthDp >= 600
    val horizontalPadding = when {
        tabletWidth -> 64.dp
        compactWidth -> 16.dp
        else -> 32.dp
    }
    val verticalPadding = if (compactHeight) 2.dp else 4.dp
    val textSize = if (compactHeight || compactWidth) {
        MaterialTheme.typography.labelSmall.fontSize
    } else {
        MaterialTheme.typography.bodySmall.fontSize
    }

    val style = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = textSize,
        letterSpacing = 0.sp,
    )

    Surface(
        modifier = modifier,
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = progress,
                modifier = Modifier.weight(1f),
                style = style,
                textAlign = TextAlign.Start,
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = style,
                textAlign = TextAlign.Center,
            )
            Text(
                text = time,
                modifier = Modifier.weight(1f),
                style = style,
                textAlign = TextAlign.End,
            )
        }
    }
}

private val readerTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun currentReaderTime(): String = LocalTime.now().format(readerTimeFormatter)

@PreviewLightDark
@Composable
private fun ReaderPageIndicatorPreview() {
    TachiyomiPreviewTheme {
        Surface {
            ReaderPageIndicator(currentPage = "10", totalPages = 69)
        }
    }
}
