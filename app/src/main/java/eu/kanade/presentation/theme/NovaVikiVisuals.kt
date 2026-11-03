package eu.kanade.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NovaVikiColors {
    val Background: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
    val Surface: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainer
    val SurfaceRaised: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val SurfaceStrong: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHighest
    val SurfaceInteractive: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
    val Outline: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
    val OutlineStrong: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val TextMuted: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
    val Accent: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val AccentSoft: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
    val AccentWarm: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
    val AccentGold: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
    val Success: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
    val Danger: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
    val Scrim: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.scrim
}

object NovaVikiDimensions {
    val CompactHorizontalPadding = NovaSignalDimensions.CompactHorizontalPadding
    val HorizontalPadding = NovaSignalDimensions.CompactHorizontalPadding
    val ExpandedHorizontalPadding = NovaSignalDimensions.ExpandedHorizontalPadding
    val MinimumTouchTarget = NovaSignalDimensions.MinimumTouchTarget
    val ListRowMinHeight = NovaSignalDimensions.ListRowMinHeight
    val NavigationBarHeight = NovaSignalDimensions.NavigationBarHeight
    val SectionSpacing = NovaSignalDimensions.SectionSpacing
    val CompactContentMaxWidth = NovaSignalDimensions.CompactContentMaxWidth
    val WideContentMaxWidth = NovaSignalDimensions.WideContentMaxWidth
    val GridContentMaxWidth = NovaSignalDimensions.GridContentMaxWidth
    val CardCorner = NovaSignalDimensions.ContentCorner
    val ControlCorner = NovaSignalDimensions.ControlCorner
}

object NovaVikiMotion {
    const val Fast = NovaSignalMotion.Feedback
    const val Standard = NovaSignalMotion.Control
    const val Content = NovaSignalMotion.Content
}

val NovaVikiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(NovaVikiDimensions.CardCorner),
    medium = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
    large = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
    extraLarge = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
)

private val DefaultTypography = Typography()

val NovaVikiTypography = Typography(
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
        fontWeight = FontWeight.Bold,
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
