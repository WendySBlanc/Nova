package eu.kanade.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NovaVikiColors {
    val Background = Color(0xFF080A0D)
    val Surface = Color(0xFF111419)
    val SurfaceRaised = Color(0xFF191D23)
    val SurfaceStrong = Color(0xFF222831)
    val Outline = Color(0xFF313844)
    val TextPrimary = Color(0xFFF7F8FA)
    val TextSecondary = Color(0xFFAEB6C2)
    val Accent = Color(0xFF39B8FF)
    val AccentWarm = Color(0xFFFF7085)
    val AccentGold = Color(0xFFF4C95D)
    val Success = Color(0xFF71D6A0)
}

object NovaVikiDimensions {
    val CompactHorizontalPadding = 10.dp
    val HorizontalPadding = 14.dp
    val ExpandedHorizontalPadding = 20.dp
    val MinimumTouchTarget = 48.dp
    val ListRowMinHeight = 64.dp
    val NavigationBarHeight = 56.dp
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
        letterSpacing = 0.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleSmall = DefaultTypography.titleSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    bodyLarge = DefaultTypography.bodyLarge.copy(letterSpacing = 0.sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(letterSpacing = 0.sp),
    bodySmall = DefaultTypography.bodySmall.copy(letterSpacing = 0.sp),
    labelLarge = DefaultTypography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
)
