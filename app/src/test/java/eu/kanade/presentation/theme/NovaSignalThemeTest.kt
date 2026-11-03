package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class NovaSignalThemeTest {

    @Test
    fun `shape spacing and touch tokens match the Nova Signal contract`() {
        NovaSignalDimensions.SmallCorner shouldBe 4.dp
        NovaSignalDimensions.ContentCorner shouldBe 6.dp
        NovaSignalDimensions.ControlCorner shouldBe 8.dp
        NovaSignalDimensions.MinimumTouchTarget shouldBe 48.dp
        NovaSignalDimensions.CompactHorizontalPadding shouldBe 16.dp
        NovaSignalDimensions.ExpandedHorizontalPadding shouldBe 24.dp
    }

    @Test
    fun `motion durations keep feedback fast and content transitions calm`() {
        NovaSignalMotion.Feedback shouldBe 90
        NovaSignalMotion.Control shouldBe 160
        NovaSignalMotion.Content shouldBe 220
    }

    @Test
    fun `every reading state has a non color indicator`() {
        NovaSignalStatus.entries.map { it.name } shouldContainExactlyInAnyOrder listOf(
            "Unread",
            "InProgress",
            "Read",
            "Downloaded",
            "Downloading",
            "Failed",
            "NewChapter",
        )
        NovaSignalStatus.entries.all { it.hasNonColorIndicator } shouldBe true
    }

    @Test
    fun `signal palettes use distinct primary secondary and tertiary roles`() {
        listOf(novaSignalLightColorScheme(), novaSignalDarkColorScheme()).forEach { scheme ->
            scheme.primary shouldNotBe scheme.secondary
            scheme.primary shouldNotBe scheme.tertiary
            scheme.secondary shouldNotBe scheme.tertiary
        }
    }

    @Test
    fun `amoled palette uses a black background without flattening raised surfaces`() {
        val scheme = novaSignalDarkColorScheme(isAmoled = true)

        scheme.background shouldBe Color.Black
        scheme.surface shouldNotBe scheme.surfaceContainerHigh
    }
}
