package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

data class NovaCoverGradientPalette(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val outline: Color,
    val contentScrim: Color,
)

fun novaCoverGradientPalette(
    seedColor: Color?,
    background: Color,
    fallbackAccent: Color,
    secondaryAccent: Color,
    tertiaryAccent: Color,
): NovaCoverGradientPalette {
    val source = (seedColor ?: fallbackAccent).readableGradientSource()
    val secondarySource = lerp(source, secondaryAccent, 0.32f)
    val tertiarySource = lerp(source, tertiaryAccent, 0.38f)

    return NovaCoverGradientPalette(
        background = background,
        primary = lerp(background, source, 0.42f),
        secondary = lerp(background, secondarySource, 0.34f),
        tertiary = lerp(background, tertiarySource, 0.3f),
        outline = lerp(Color.White, source, 0.38f),
        contentScrim = Color.Black.copy(alpha = 0.82f),
    )
}

private fun Color.readableGradientSource(): Color = when {
    luminance() < 0.04f -> lerp(this, Color.White, 0.42f)
    luminance() > 0.72f -> lerp(this, Color.Black, 0.42f)
    else -> this
}
