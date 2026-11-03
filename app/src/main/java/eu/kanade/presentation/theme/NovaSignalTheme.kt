package eu.kanade.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
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
    val background = if (isAmoled) Color.Black else Color(0xFF111311)
    val lowestSurface = if (isAmoled) Color.Black else Color(0xFF0C0F0C)
    val surface = if (isAmoled) Color(0xFF080A08) else Color(0xFF111311)

    return darkColorScheme(
        primary = Color(0xFF55DBC2),
        onPrimary = Color(0xFF00382F),
        primaryContainer = Color(0xFF005144),
        onPrimaryContainer = Color(0xFF7FF8DD),
        inversePrimary = Color(0xFF006B5B),
        secondary = Color(0xFFFFB4A8),
        onSecondary = Color(0xFF5F150A),
        secondaryContainer = Color(0xFF7D2C1F),
        onSecondaryContainer = Color(0xFFFFDAD4),
        tertiary = Color(0xFFBCD878),
        onTertiary = Color(0xFF2C3400),
        tertiaryContainer = Color(0xFF424D00),
        onTertiaryContainer = Color(0xFFD8F492),
        background = background,
        onBackground = Color(0xFFE2E4DE),
        surface = surface,
        onSurface = Color(0xFFE2E4DE),
        surfaceVariant = Color(0xFF424843),
        onSurfaceVariant = Color(0xFFC2C8C2),
        surfaceTint = Color(0xFF55DBC2),
        inverseSurface = Color(0xFFE2E4DE),
        inverseOnSurface = Color(0xFF2F312E),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF8C938D),
        outlineVariant = Color(0xFF424843),
        surfaceContainerLowest = lowestSurface,
        surfaceContainerLow = if (isAmoled) Color(0xFF0B0D0B) else Color(0xFF151815),
        surfaceContainer = if (isAmoled) Color(0xFF0E100E) else Color(0xFF191C19),
        surfaceContainerHigh = if (isAmoled) Color(0xFF151815) else Color(0xFF1E211E),
        surfaceContainerHighest = if (isAmoled) Color(0xFF1B1E1B) else Color(0xFF252925),
    )
}

fun novaSignalLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF006B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF78F8DC),
    onPrimaryContainer = Color(0xFF00201A),
    inversePrimary = Color(0xFF55DBC2),
    secondary = Color(0xFFA13A2A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF3F0300),
    tertiary = Color(0xFF586500),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8F492),
    onTertiaryContainer = Color(0xFF1A1E00),
    background = Color(0xFFFAFCF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFAFCF7),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDEE4DE),
    onSurfaceVariant = Color(0xFF424843),
    surfaceTint = Color(0xFF006B5B),
    inverseSurface = Color(0xFF2F312E),
    inverseOnSurface = Color(0xFFF1F3EE),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF727973),
    outlineVariant = Color(0xFFC2C8C2),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F6F1),
    surfaceContainer = Color(0xFFEEF0EB),
    surfaceContainerHigh = Color(0xFFE8EAE5),
    surfaceContainerHighest = Color(0xFFE2E4DF),
)
