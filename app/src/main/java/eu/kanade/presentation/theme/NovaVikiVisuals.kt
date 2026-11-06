@file:Suppress("PropertyName")

package eu.kanade.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

object NovaVikiColors {
    val Background: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background
    val Surface: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainer
    val SurfaceRaised: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val SurfaceStrong: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest
    val SurfaceInteractive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primaryContainer
    val Outline: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant
    val OutlineStrong: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outline
    val TextPrimary: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface
    val TextSecondary: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant
    val TextMuted: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
    val Accent: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary
    val AccentSoft: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primaryContainer
    val AccentWarm: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary
    val AccentGold: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiary
    val Success: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiary
    val Danger: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error
    val Scrim: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.scrim
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
    val CardCorner = NovaSignalDimensions.ControlCorner
    val ControlCorner = NovaSignalDimensions.ControlCorner
    val LargeCorner = NovaSignalDimensions.LargeCorner
    val ExtraLargeCorner = NovaSignalDimensions.ExtraLargeCorner
}

val NovaVikiShapes = NovaSignalShapes

val NovaVikiTypography = NovaSignalTypography
