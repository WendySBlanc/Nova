@file:Suppress("PropertyName")

package eu.kanade.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NovaSignalDimensions {
    val SmallCorner = 4.dp
    val ContentCorner = 8.dp
    val ControlCorner = 12.dp
    val LargeCorner = 16.dp
    val ExtraLargeCorner = 28.dp
    val MinimumTouchTarget = 48.dp
    val CompactHorizontalPadding = 16.dp
    val ExpandedHorizontalPadding = 24.dp
    val ListRowMinHeight = 64.dp
    val NavigationBarHeight = 80.dp
    val SectionSpacing = 24.dp
    val CompactContentMaxWidth = 760.dp
    val WideContentMaxWidth = 1200.dp
    val GridContentMaxWidth = 1440.dp
    val SignalRailWidth = 3.dp
}

enum class NovaSignalStatus(
    val hasNonColorIndicator: Boolean,
) {
    Unread(true),
    InProgress(true),
    Read(true),
    Downloaded(true),
    Downloading(true),
    Failed(true),
    NewChapter(true),
}

val NovaSignalShapes = Shapes(
    extraSmall = RoundedCornerShape(NovaSignalDimensions.SmallCorner),
    small = RoundedCornerShape(NovaSignalDimensions.ContentCorner),
    medium = RoundedCornerShape(NovaSignalDimensions.ControlCorner),
    large = RoundedCornerShape(NovaSignalDimensions.LargeCorner),
    extraLarge = RoundedCornerShape(NovaSignalDimensions.ExtraLargeCorner),
)

private val DefaultTypography = Typography()

val NovaSignalTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    ),
    displayMedium = DefaultTypography.displayMedium.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    ),
    displaySmall = DefaultTypography.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    headlineLarge = DefaultTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    headlineSmall = DefaultTypography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = DefaultTypography.titleSmall.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = DefaultTypography.bodySmall.copy(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = DefaultTypography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)

fun novaSignalDarkColorScheme(isAmoled: Boolean = false): ColorScheme {
    val background = if (isAmoled) Color.Black else Color(0xFF141416)
    val surface = if (isAmoled) Color(0xFF101012) else Color(0xFF1C1C1F)

    return darkColorScheme(
        primary = Color(0xFFC7C2FF),
        onPrimary = Color(0xFF251D78),
        primaryContainer = Color(0xFF383060),
        onPrimaryContainer = Color(0xFFEAE8FF),
        inversePrimary = Color(0xFF4C42C5),
        secondary = Color(0xFF9DD5E3),
        onSecondary = Color(0xFF103640),
        secondaryContainer = Color(0xFF214853),
        onSecondaryContainer = Color(0xFFBDEAF5),
        tertiary = Color(0xFFF4B5C7),
        onTertiary = Color(0xFF512432),
        tertiaryContainer = Color(0xFF633642),
        onTertiaryContainer = Color(0xFFFFD9E2),
        background = background,
        onBackground = Color(0xFFF2F1F4),
        surface = surface,
        onSurface = Color(0xFFF2F1F4),
        surfaceVariant = Color(0xFF303035),
        onSurfaceVariant = Color(0xFFC5C3CB),
        surfaceTint = Color(0xFFF2F1F4),
        inverseSurface = Color(0xFFF2F1F4),
        inverseOnSurface = Color(0xFF26262A),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF93919B),
        outlineVariant = Color(0xFF3A3940),
        surfaceDim = background,
        surfaceBright = Color(0xFF38383D),
        surfaceContainerLowest = if (isAmoled) Color.Black else Color(0xFF101012),
        surfaceContainerLow = if (isAmoled) Color(0xFF0C0C0E) else Color(0xFF19191C),
        surfaceContainer = if (isAmoled) Color(0xFF161618) else Color(0xFF222225),
        surfaceContainerHigh = if (isAmoled) Color(0xFF1D1D20) else Color(0xFF29292D),
        surfaceContainerHighest = if (isAmoled) Color(0xFF27272B) else Color(0xFF323237),
    )
}

fun novaSignalLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF4C42C5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE8FF),
    onPrimaryContainer = Color(0xFF201A72),
    inversePrimary = Color(0xFFC7C2FF),
    secondary = Color(0xFF245E6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5EDF2),
    onSecondaryContainer = Color(0xFF143C46),
    tertiary = Color(0xFF895267),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF9DEE6),
    onTertiaryContainer = Color(0xFF53293A),
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF171719),
    surface = Color(0xFFFEFEFE),
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFE9E9EB),
    onSurfaceVariant = Color(0xFF54555A),
    surfaceTint = Color(0xFF202124),
    inverseSurface = Color(0xFF2C2C30),
    inverseOnSurface = Color(0xFFF2F1F4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF6B6B73),
    outlineVariant = Color(0xFFDCDCDD),
    surfaceDim = Color(0xFFE0E0E2),
    surfaceBright = Color(0xFFFEFEFE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEFEFF0),
    surfaceContainerHighest = Color(0xFFE9E9EB),
)

internal fun ColorScheme.withNovaAccentColors(accent: ColorScheme): ColorScheme {
    val primary = accent.primary.contrastingAccent(this.primary, background, surfaceContainerHighest)
    val secondary = accent.secondary.contrastingAccent(this.secondary, background, surfaceContainerHighest)
    val tertiary = accent.tertiary.contrastingAccent(this.tertiary, background, surfaceContainerHighest)
    val primaryContainer = accent.primaryContainer.opaqueOr(this.primaryContainer)
    val secondaryContainer = accent.secondaryContainer.opaqueOr(this.secondaryContainer)
    val tertiaryContainer = accent.tertiaryContainer.opaqueOr(this.tertiaryContainer)
    return copy(
        primary = primary,
        onPrimary = primary.readableForeground(accent.onPrimary.opaqueOr(onPrimary)),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.readableForeground(
            if (accent.primaryContainer.alpha == 1f) accent.onPrimaryContainer else onPrimaryContainer,
        ),
        inversePrimary = accent.inversePrimary.contrastingAccent(inversePrimary, inverseSurface, inverseSurface),
        secondary = secondary,
        onSecondary = secondary.readableForeground(accent.onSecondary.opaqueOr(onSecondary)),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.readableForeground(
            if (accent.secondaryContainer.alpha == 1f) accent.onSecondaryContainer else onSecondaryContainer,
        ),
        tertiary = tertiary,
        onTertiary = tertiary.readableForeground(accent.onTertiary.opaqueOr(onTertiary)),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = tertiaryContainer.readableForeground(
            if (accent.tertiaryContainer.alpha == 1f) accent.onTertiaryContainer else onTertiaryContainer,
        ),
    )
}

private fun Color.opaqueOr(fallback: Color): Color = if (alpha == 1f) this else fallback

private fun Color.readableForeground(preferred: Color): Color = when {
    hasTextContrast(preferred) -> preferred
    hasTextContrast(Color.Black) -> Color.Black
    else -> Color.White
}

private fun Color.contrastingAccent(fallback: Color, first: Color, second: Color): Color {
    if (alpha != 1f) return fallback
    fun readable(value: Color) = value.hasTextContrast(first) && value.hasTextContrast(second)
    if (readable(this)) return this
    val target = first.readableForeground(Color.Unspecified)
    if (!readable(target)) return fallback

    // Use the smallest tone adjustment that passes on both Nova surfaces.
    var low = 0f
    var high = 1f
    repeat(12) {
        val amount = (low + high) / 2f
        if (readable(lerp(this, target, amount))) high = amount else low = amount
    }
    return lerp(this, target, high)
}

private fun Color.hasTextContrast(other: Color): Boolean {
    if (alpha != 1f || other.alpha != 1f) return false
    val first = luminance()
    val second = other.luminance()
    return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f) >= 4.5f
}
