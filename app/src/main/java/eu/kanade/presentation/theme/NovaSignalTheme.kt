package eu.kanade.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NovaSignalDimensions {
    val SmallCorner = 4.dp
    val ContentCorner = 6.dp
    val ControlCorner = 8.dp
    val MinimumTouchTarget = 48.dp
    val CompactHorizontalPadding = 16.dp
    val ExpandedHorizontalPadding = 24.dp
    val ListRowMinHeight = 64.dp
    val NavigationBarHeight = 64.dp
    val SectionSpacing = 24.dp
    val CompactContentMaxWidth = 760.dp
    val WideContentMaxWidth = 1200.dp
    val GridContentMaxWidth = 1440.dp
    val SignalRailWidth = 3.dp
}

object NovaSignalMotion {
    const val Feedback = 90
    const val Control = 160
    const val Content = 220
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
    large = RoundedCornerShape(NovaSignalDimensions.ControlCorner),
    extraLarge = RoundedCornerShape(NovaSignalDimensions.ControlCorner),
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
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)

fun novaSignalDarkColorScheme(isAmoled: Boolean = false): ColorScheme {
    val background = if (isAmoled) Color.Black else Color(0xFF050607)
    val lowestSurface = Color.Black
    val surface = if (isAmoled) Color(0xFF030404) else Color(0xFF080A0C)

    return darkColorScheme(
        primary = Color(0xFFD9FF52),
        onPrimary = Color(0xFF172000),
        primaryContainer = Color(0xFF26330A),
        onPrimaryContainer = Color(0xFFE8FF9A),
        inversePrimary = Color(0xFF526900),
        secondary = Color(0xFF62D8FF),
        onSecondary = Color(0xFF003544),
        secondaryContainer = Color(0xFF073F50),
        onSecondaryContainer = Color(0xFFB9EAFF),
        tertiary = Color(0xFFAE8BFF),
        onTertiary = Color(0xFF2B0D63),
        tertiaryContainer = Color(0xFF3F2375),
        onTertiaryContainer = Color(0xFFE8DDFF),
        background = background,
        onBackground = Color(0xFFF3F5F7),
        surface = surface,
        onSurface = Color(0xFFF3F5F7),
        surfaceVariant = Color(0xFF262A31),
        onSurfaceVariant = Color(0xFFB8C0CA),
        surfaceTint = Color(0xFFD9FF52),
        inverseSurface = Color(0xFFF3F5F7),
        inverseOnSurface = Color(0xFF202328),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF747D89),
        outlineVariant = Color(0xFF282D35),
        surfaceContainerLowest = lowestSurface,
        surfaceContainerLow = if (isAmoled) Color(0xFF050606) else Color(0xFF0C0F12),
        surfaceContainer = if (isAmoled) Color(0xFF090A0B) else Color(0xFF111419),
        surfaceContainerHigh = if (isAmoled) Color(0xFF0E1012) else Color(0xFF171B21),
        surfaceContainerHighest = if (isAmoled) Color(0xFF15181B) else Color(0xFF20252C),
    )
}

fun novaSignalLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF4D6500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCF58B),
    onPrimaryContainer = Color(0xFF161F00),
    inversePrimary = Color(0xFFD9FF52),
    secondary = Color(0xFF006784),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBCE9FA),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = Color(0xFF6845A8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9DDFF),
    onTertiaryContainer = Color(0xFF23005B),
    background = Color(0xFFF4F5F2),
    onBackground = Color(0xFF17191C),
    surface = Color(0xFFF4F5F2),
    onSurface = Color(0xFF17191C),
    surfaceVariant = Color(0xFFDFE2E5),
    onSurfaceVariant = Color(0xFF454A51),
    surfaceTint = Color(0xFF4D6500),
    inverseSurface = Color(0xFF2C3035),
    inverseOnSurface = Color(0xFFF1F2F4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF737980),
    outlineVariant = Color(0xFFC4C8CD),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F1EE),
    surfaceContainer = Color(0xFFE9EBE8),
    surfaceContainerHigh = Color(0xFFE3E5E2),
    surfaceContainerHighest = Color(0xFFDDE0DD),
)
