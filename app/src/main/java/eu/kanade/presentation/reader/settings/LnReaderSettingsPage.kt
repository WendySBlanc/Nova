package eu.kanade.presentation.reader.settings

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.util.collectAsState
import java.util.Locale

@Composable
internal fun LnReaderImagePage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem("Affichage")

    LnReaderThemeSelector(screenModel)

    val sidePadding by screenModel.preferences.webtoonSidePadding().collectAsState()
    SliderItem(
        value = sidePadding,
        valueRange = ReaderPreferences.WEBTOON_PADDING_MIN..ReaderPreferences.WEBTOON_PADDING_MAX,
        label = "Marges latérales",
        valueString = "$sidePadding%",
        onChange = screenModel.preferences.webtoonSidePadding()::set,
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    CheckboxItem(
        label = "Zoom par pincement",
        pref = screenModel.preferences.webtoonPinchToZoomEnabled(),
    )
}

@Composable
internal fun LnReaderPage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem("Texte")

    val textSize by screenModel.preferences.lnReaderTextSize().collectAsState()
    SliderItem(
        value = textSize,
        valueRange = 12..30,
        label = "Taille du texte",
        valueString = "${textSize}sp",
        onChange = screenModel.preferences.lnReaderTextSize()::set,
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    LnReaderThemeSelector(screenModel)
    LnReaderTextAlignSelector(screenModel)

    val lineHeight by screenModel.preferences.lnReaderLineHeight().collectAsState()
    SliderItem(
        value = lineHeight,
        valueRange = 120..240 step 10,
        label = "Interligne",
        valueString = "${lineHeight}%",
        onChange = screenModel.preferences.lnReaderLineHeight()::set,
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    val padding by screenModel.preferences.lnReaderTextPadding().collectAsState()
    SliderItem(
        value = padding,
        valueRange = 0..50,
        label = "Marges du texte",
        valueString = "${padding}px",
        onChange = screenModel.preferences.lnReaderTextPadding()::set,
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    LnReaderFontSelector(screenModel)
}

@Composable
internal fun LnReaderTtsPage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem("Lecture vocale")

    CheckboxItem(
        label = "Activer la lecture vocale",
        pref = screenModel.preferences.lnReaderTtsEnabled(),
    )

    val enabled by screenModel.preferences.lnReaderTtsEnabled().collectAsState()
    if (!enabled) return

    val rate by screenModel.preferences.lnReaderTtsRate().collectAsState()
    SliderItem(
        value = rate,
        valueRange = 10..500 step 10,
        label = "Vitesse",
        valueString = "${rate / 100f}x",
        onChange = screenModel.preferences.lnReaderTtsRate()::set,
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    val pitch by screenModel.preferences.lnReaderTtsPitch().collectAsState()
    SliderItem(
        value = pitch,
        valueRange = 10..500 step 10,
        label = "Hauteur de la voix",
        valueString = "${pitch / 100f}",
        onChange = screenModel.preferences.lnReaderTtsPitch()::set,
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    LnReaderVoiceSelector(screenModel)

    CheckboxItem(
        label = "Avancer automatiquement",
        pref = screenModel.preferences.lnReaderTtsAutoNext(),
    )
    CheckboxItem(
        label = "Revenir en haut au démarrage",
        pref = screenModel.preferences.lnReaderTtsScrollToTop(),
    )
    CheckboxItem(
        label = "Surligner la phrase lue",
        pref = screenModel.preferences.lnReaderTtsHighlightSentence(),
    )

    Row(
        modifier = Modifier.paddingSettings(),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedButton(
            onClick = {
                screenModel.preferences.lnReaderTtsRate().set(100)
                screenModel.preferences.lnReaderTtsPitch().set(100)
                screenModel.preferences.lnReaderTtsVoiceName().set("system")
                screenModel.preferences.lnReaderTtsAutoNext().set(true)
                screenModel.preferences.lnReaderTtsScrollToTop().set(true)
                screenModel.preferences.lnReaderTtsHighlightSentence().set(true)
            },
        ) {
            Text("Réinitialiser la voix")
        }
    }
}

@Composable
internal fun LnReaderGeneralPage(
    screenModel: ReaderSettingsScreenModel,
    textReader: Boolean,
) {
    HeadingItem("Comportement")

    CheckboxItem(
        label = "Plein écran",
        pref = screenModel.preferences.fullscreen(),
    )
    CheckboxItem(
        label = "Défilement automatique",
        pref = screenModel.preferences.lnReaderAutoScroll(),
    )
    val autoScroll by screenModel.preferences.lnReaderAutoScroll().collectAsState()
    if (autoScroll) {
        val interval by screenModel.preferences.lnReaderAutoScrollInterval().collectAsState()
        SliderItem(
            value = interval,
            valueRange = 1..30,
            label = "Intervalle de défilement",
            valueString = "${interval}s",
            onChange = screenModel.preferences.lnReaderAutoScrollInterval()::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val offset by screenModel.preferences.lnReaderAutoScrollOffsetPercent().collectAsState()
        SliderItem(
            value = offset,
            valueRange = 25..100 step 5,
            label = "Distance de défilement",
            valueString = "$offset%",
            onChange = screenModel.preferences.lnReaderAutoScrollOffsetPercent()::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
    CheckboxItem(
        label = "Défilement fluide",
        pref = screenModel.preferences.smoothAutoScroll(),
    )
    if (textReader) {
        CheckboxItem(
            label = "Gestes de balayage",
            pref = screenModel.preferences.lnReaderSwipeGestures(),
        )
        CheckboxItem(
            label = "Lecture page par page",
            pref = screenModel.preferences.lnReaderPageReader(),
        )
        CheckboxItem(
            label = "Réduire l'espace entre les paragraphes",
            pref = screenModel.preferences.lnReaderRemoveExtraParagraphSpacing(),
        )
    }
    CheckboxItem(
        label = "Défiler avec les boutons de volume",
        pref = screenModel.preferences.readWithVolumeKeys(),
    )
    if (textReader) {
        CheckboxItem(
            label = "Lecture bionique",
            pref = screenModel.preferences.lnReaderBionicReading(),
        )
        CheckboxItem(
            label = "Toucher pour faire défiler",
            pref = screenModel.preferences.lnReaderTapToScroll(),
        )
    }
    CheckboxItem(
        label = "Garder l'écran allumé",
        pref = screenModel.preferences.keepScreenOn(),
    )
}

@Composable
internal fun LnReaderAdvancedPage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem("Personnalisation avancée")

    val customBackground by screenModel.preferences.lnReaderCustomBackgroundColor().collectAsState()
    OutlinedTextField(
        value = customBackground,
        onValueChange = {
            screenModel.preferences.lnReaderCustomBackgroundColor().set(it)
            screenModel.preferences.lnReaderTheme().set("custom")
        },
        modifier = Modifier
            .paddingSettings()
            .fillMaxWidth(),
        label = { Text("Couleur d'arrière-plan") },
        singleLine = true,
    )

    val customTextColor by screenModel.preferences.lnReaderCustomTextColor().collectAsState()
    OutlinedTextField(
        value = customTextColor,
        onValueChange = {
            screenModel.preferences.lnReaderCustomTextColor().set(it)
            screenModel.preferences.lnReaderTheme().set("custom")
        },
        modifier = Modifier
            .paddingSettings()
            .fillMaxWidth(),
        label = { Text("Couleur du texte") },
        singleLine = true,
    )

    val customCss by screenModel.preferences.lnReaderCustomCss().collectAsState()
    OutlinedTextField(
        value = customCss,
        onValueChange = screenModel.preferences.lnReaderCustomCss()::set,
        modifier = Modifier
            .paddingSettings()
            .fillMaxWidth()
            .heightIn(min = 140.dp),
        label = { Text("CSS personnalisé") },
        minLines = 6,
    )

    val customJs by screenModel.preferences.lnReaderCustomJs().collectAsState()
    OutlinedTextField(
        value = customJs,
        onValueChange = screenModel.preferences.lnReaderCustomJs()::set,
        modifier = Modifier
            .paddingSettings()
            .fillMaxWidth()
            .heightIn(min = 140.dp),
        label = { Text("JavaScript personnalisé") },
        minLines = 6,
    )
}

@Composable
private fun LnReaderTextAlignSelector(screenModel: ReaderSettingsScreenModel) {
    val selected by screenModel.preferences.lnReaderTextAlign().collectAsState()
    val options = listOf(
        "left" to "Gauche",
        "justify" to "Justifié",
        "center" to "Centré",
        "right" to "Droite",
    )

    HeadingItem("Alignement")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .paddingSettings(),
    ) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { screenModel.preferences.lnReaderTextAlign().set(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LnReaderFontSelector(screenModel: ReaderSettingsScreenModel) {
    val selected by screenModel.preferences.lnReaderFontFamily().collectAsState()
    val options = listOf(
        "system" to "Système",
        "arbutus-slab" to "Arbutus Slab",
        "domine" to "Domine",
        "lato" to "Lato",
        "lora" to "Lora",
        "noto-sans" to "Noto Sans",
        "nunito" to "Nunito",
        "open-sans" to "Open Sans",
        "OpenDyslexic3-Regular" to "OpenDyslexic",
        "pt-sans-bold" to "PT Sans",
        "pt-serif" to "PT Serif",
        "monospace" to "Monospace",
    )

    HeadingItem("Police")
    ReaderDropdownSetting(
        selected = selected,
        options = options,
        onSelected = screenModel.preferences.lnReaderFontFamily()::set,
    )
}

@Composable
private fun LnReaderVoiceSelector(screenModel: ReaderSettingsScreenModel) {
    val context = LocalContext.current
    val selected by screenModel.preferences.lnReaderTtsVoiceName().collectAsState()
    var voices by remember { mutableStateOf<List<VoiceOption>>(emptyList()) }

    DisposableEffect(context) {
        val mainHandler = Handler(Looper.getMainLooper())
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val systemLanguage = Locale.getDefault().language
                val options = engine?.voices
                    .orEmpty()
                    .filter { !it.isNetworkConnectionRequired }
                    .sortedWith(
                        compareBy<Voice> {
                            it.locale.language != systemLanguage
                        }.thenBy { it.locale.displayLanguage }.thenBy { it.name },
                    )
                    .map { voice ->
                        VoiceOption(
                            value = voice.name,
                            label = buildString {
                                append(voice.locale.displayLanguage.ifBlank { voice.locale.language })
                                append(" - ")
                                append(voice.name.substringAfterLast('.'))
                            },
                        )
                    }
                    .distinctBy { it.value }
                    .take(24)
                mainHandler.post {
                    voices = listOf(VoiceOption("system", "Système")) + options
                }
            }
        }

        onDispose { engine?.shutdown() }
    }

    val options = voices
        .ifEmpty { listOf(VoiceOption("system", "Système")) }
        .map { it.value to it.label }
    HeadingItem("Voix")
    ReaderDropdownSetting(
        selected = selected,
        options = options,
        onSelected = screenModel.preferences.lnReaderTtsVoiceName()::set,
    )
}

private data class VoiceOption(
    val value: String,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderDropdownSetting(
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second
        ?: options.firstOrNull()?.second
        .orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .paddingSettings(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun LnReaderThemeSelector(screenModel: ReaderSettingsScreenModel) {
    val selected by screenModel.preferences.lnReaderTheme().collectAsState()
    val themes = listOf(
        LnReaderThemeOption("dark", "Sombre", Color(0xFF121212), Color(0xFFECECEC)),
        LnReaderThemeOption("black", "Noir", Color.Black, Color.White),
        LnReaderThemeOption("sepia", "Sépia", Color(0xFFF4ECD8), Color(0xFF3B2F20)),
        LnReaderThemeOption("light", "Clair", Color.White, Color(0xFF202124)),
        LnReaderThemeOption(
            "custom",
            "Perso",
            parseReaderColor(
                screenModel.preferences.lnReaderCustomBackgroundColor().collectAsState().value,
                Color(0xFF292832),
            ),
            parseReaderColor(
                screenModel.preferences.lnReaderCustomTextColor().collectAsState().value,
                Color(0xFFCCCCCC),
            ),
        ),
    )

    HeadingItem("Thème")
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .paddingSettings(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        themes.forEach { option ->
            val isSelected = selected == option.value
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .heightIn(min = 68.dp)
                    .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
                    .clickable { screenModel.preferences.lnReaderTheme().set(option.value) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
                        .background(option.background)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) NovaVikiColors.Accent else NovaVikiColors.Outline,
                            shape = RoundedCornerShape(NovaVikiDimensions.CardCorner),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Aa",
                        color = option.foreground,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    text = option.label,
                    color = if (isSelected) NovaVikiColors.Accent else NovaVikiColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun parseReaderColor(value: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(value)) }
        .getOrDefault(fallback)
}

private data class LnReaderThemeOption(
    val value: String,
    val label: String,
    val background: Color,
    val foreground: Color,
)

private fun Modifier.paddingSettings(): Modifier = this.then(
    Modifier.padding(
        start = SettingsItemsPaddings.Horizontal,
        top = 0.dp,
        end = SettingsItemsPaddings.Horizontal,
        bottom = SettingsItemsPaddings.Vertical,
    ),
)
