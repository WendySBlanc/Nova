package eu.kanade.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NovaVikiColors {
    val Background = Color(0xFF07090D)
    val Surface = Color(0xFF0D1117)
    val SurfaceRaised = Color(0xFF151B24)
    val SurfaceStrong = Color(0xFF1D2632)
    val SurfaceInteractive = Color(0xFF253141)
    val Outline = Color(0xFF2B3747)
    val OutlineStrong = Color(0xFF647A96)
    val TextPrimary = Color(0xFFF7F9FC)
    val TextSecondary = Color(0xFFA9B4C2)
    val TextMuted = Color(0xFF7C8999)
    val Accent = Color(0xFF4CC9FF)
    val AccentSoft = Color(0xFF123A4D)
    val AccentWarm = Color(0xFFFF7694)
    val AccentGold = Color(0xFFF7C95C)
    val Success = Color(0xFF68D7A7)
    val Danger = Color(0xFFFF8A80)
    val Scrim = Color(0xFF030406)
}

object NovaVikiDimensions {
    val CompactHorizontalPadding = 10.dp
    val HorizontalPadding = 14.dp
    val ExpandedHorizontalPadding = 20.dp
    val MinimumTouchTarget = 48.dp
    val ListRowMinHeight = 64.dp
    val NavigationBarHeight = 56.dp
    val SectionSpacing = 20.dp
    val CompactContentMaxWidth = 760.dp
    val WideContentMaxWidth = 1200.dp
    val GridContentMaxWidth = 1440.dp
    val CardCorner = 6.dp
    val ControlCorner = 8.dp
}

object NovaVikiMotion {
    const val Fast = 120
    const val Standard = 180
    const val Content = 220
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
