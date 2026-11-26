package eu.kanade.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.colorscheme.CloudflareColorScheme
import eu.kanade.presentation.theme.colorscheme.MatrixColorScheme
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class NovaSignalThemeTest {

    @Test
    fun `shape spacing and touch tokens follow the Material 3 contract`() {
        NovaSignalDimensions.SmallCorner shouldBe 4.dp
        NovaSignalDimensions.ContentCorner shouldBe 8.dp
        NovaSignalDimensions.ControlCorner shouldBe 12.dp
        NovaSignalDimensions.LargeCorner shouldBe 16.dp
        NovaSignalDimensions.ExtraLargeCorner shouldBe 28.dp
        NovaSignalDimensions.MinimumTouchTarget shouldBe 48.dp
        NovaSignalDimensions.NavigationBarHeight shouldBe 80.dp
        NovaSignalDimensions.CompactHorizontalPadding shouldBe 16.dp
        NovaSignalDimensions.ExpandedHorizontalPadding shouldBe 24.dp
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
    fun `default light palette carries the catalog paper and indigo into every screen`() {
        val scheme = novaSignalLightColorScheme()

        scheme.background shouldBe Color(0xFFF7F7F7)
        scheme.surface shouldBe Color(0xFFFEFEFE)
        scheme.primary shouldBe Color(0xFF4C42C5)
    }

    @Test
    fun `custom accent keeps the shared surfaces in light dark and amoled modes`() {
        val cases = listOf(
            novaSignalLightColorScheme() to lightColorScheme(primary = Color(0xFF005845), onPrimary = Color.White),
            novaSignalDarkColorScheme() to darkColorScheme(primary = Color(0xFFB4F8DB), onPrimary = Color.Black),
            novaSignalDarkColorScheme(isAmoled = true) to darkColorScheme(),
        )

        cases.forEach { (base, accent) ->
            val scheme = base.withNovaAccentColors(accent)

            scheme.primary shouldBe accent.primary
            scheme.background shouldBe base.background
            scheme.surface shouldBe base.surface
            scheme.surfaceTint shouldBe base.surfaceTint
            scheme.surfaceContainerHigh shouldBe base.surfaceContainerHigh
            scheme.outlineVariant shouldBe base.outlineVariant
            scheme.onSurface shouldBe base.onSurface
        }
    }

    @Test
    fun `text and selected controls remain readable in every mode even with a poor accent`() {
        listOf(novaSignalLightColorScheme(), novaSignalDarkColorScheme(), novaSignalDarkColorScheme(true)).forEach { base ->
            val scheme = base.withNovaAccentColors(
                lightColorScheme(primary = Color.Transparent, primaryContainer = Color.Transparent, onPrimaryContainer = Color.White),
            )

            scheme.primary shouldBe base.primary
            scheme.primaryContainer shouldBe base.primaryContainer
            listOf(
                scheme.onBackground to scheme.background,
                scheme.onSurface to scheme.surface,
                scheme.onSurfaceVariant to scheme.surfaceContainerHighest,
                scheme.primary to scheme.background,
                scheme.primary to scheme.surface,
                scheme.onPrimary to scheme.primary,
                scheme.onPrimaryContainer to scheme.primaryContainer,
            ).forEach { (foreground, background) ->
                (contrastRatio(foreground, background) >= 4.5f) shouldBe true
            }
        }
    }

    @Test
    fun `presets preserve recognizable orange and green after contrast correction`() {
        val cloudflare = novaSignalLightColorScheme().withNovaAccentColors(CloudflareColorScheme.lightScheme)
        val matrix = novaSignalDarkColorScheme().withNovaAccentColors(MatrixColorScheme.darkScheme)

        (cloudflare.primary.red > cloudflare.primary.green && cloudflare.primary.green > cloudflare.primary.blue) shouldBe true
        matrix.primary shouldBe MatrixColorScheme.darkScheme.primary
        listOf(cloudflare, matrix).forEach { scheme ->
            (contrastRatio(scheme.primary, scheme.surfaceContainerHighest) >= 4.5f) shouldBe true
            (contrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f) shouldBe true
            (contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5f) shouldBe true
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val luminances = listOf(foreground.luminance(), background.luminance())
        return (luminances.max() + 0.05f) / (luminances.min() + 0.05f)
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
