package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

    @Test
    fun `cover gradient changes decorative surfaces while preserving the stable background`() {
        val background = Color(0xFF050607)
        val redPalette = novaCoverGradientPalette(
            seedColor = Color(0xFFE53935),
            background = background,
            fallbackAccent = Color(0xFFD9FF52),
            secondaryAccent = Color(0xFF62D8FF),
            tertiaryAccent = Color(0xFFAE8BFF),
        )
        val bluePalette = novaCoverGradientPalette(
            seedColor = Color(0xFF2962FF),
            background = background,
            fallbackAccent = Color(0xFFD9FF52),
            secondaryAccent = Color(0xFF62D8FF),
            tertiaryAccent = Color(0xFFAE8BFF),
        )

        redPalette.primary shouldNotBe bluePalette.primary
        redPalette.secondary shouldNotBe bluePalette.secondary
        redPalette.background shouldBe background
        bluePalette.background shouldBe background
    }

    @Test
    fun `cover gradient keeps bright covers dark enough for fixed light text`() {
        val palette = novaCoverGradientPalette(
            seedColor = Color.White,
            background = Color(0xFF050607),
            fallbackAccent = Color(0xFFD9FF52),
            secondaryAccent = Color(0xFF62D8FF),
            tertiaryAccent = Color(0xFFAE8BFF),
        )

        (palette.primary.luminance() < 0.35f) shouldBe true
        palette.contentScrim shouldBe Color.Black.copy(alpha = 0.82f)
    }
}
