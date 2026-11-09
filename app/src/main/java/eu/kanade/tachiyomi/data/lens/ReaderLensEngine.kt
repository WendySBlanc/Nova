package eu.kanade.tachiyomi.data.lens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class ReaderLensEngine(
    private val cache: ReaderLensCache,
) {
    private val scriptsByChapter = ConcurrentHashMap<String, LensScript>()
    private val languageIdentifier by lazy { LanguageIdentification.getClient() }
    private val recognizers: Map<LensScript, TextRecognizer> by lazy {
        mapOf(
            LensScript.Latin to TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
            LensScript.Korean to TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
            LensScript.Japanese to TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            LensScript.Chinese to TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
        )
    }

    fun cached(chapterKey: String, pageIndex: Int): List<ReaderLensRegion>? =
        cache.get(chapterKey, pageIndex)

    suspend fun analyze(
        chapterKey: String,
        pageIndex: Int,
        streamProvider: () -> InputStream,
    ): List<ReaderLensRegion> = withContext(Dispatchers.IO) {
        cache.get(chapterKey, pageIndex)?.let { return@withContext it }
        val bitmap = decodeForLens(streamProvider)
            ?: throw ReaderLensException("Cette page ne peut pas etre analysee")
        try {
            val recognized = recognize(chapterKey, bitmap)
            val regions = translate(recognized, bitmap.width, bitmap.height)
            cache.put(chapterKey, pageIndex, regions)
            regions
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun recognize(chapterKey: String, bitmap: Bitmap): RecognizedPage {
        val image = InputImage.fromBitmap(bitmap, 0)
        scriptsByChapter[chapterKey]?.let { script ->
            val text = recognizers.getValue(script).process(image).await()
            if (text.score() >= MIN_RECOGNIZED_CHARACTERS) return RecognizedPage(script, text)
        }

        var best: RecognizedPage? = null
        for (script in LensScript.entries) {
            val text = recognizers.getValue(script).process(image).await()
            val candidate = RecognizedPage(script, text)
            if (best == null || candidate.text.score() > best.text.score()) best = candidate
            if (candidate.text.score() >= EARLY_ACCEPT_CHARACTERS) break
        }
        val selected = best ?: throw ReaderLensException("Aucun texte detecte")
        if (selected.text.score() >= MIN_RECOGNIZED_CHARACTERS) {
            scriptsByChapter[chapterKey] = selected.script
        }
        return selected
    }

    private suspend fun translate(
        page: RecognizedPage,
        imageWidth: Int,
        imageHeight: Int,
    ): List<ReaderLensRegion> {
        val blocks = page.text.textBlocks
            .filter { it.boundingBox != null && it.text.isNotBlank() }
            .take(MAX_TEXT_BLOCKS)
        if (blocks.isEmpty()) return emptyList()

        val sourceLanguage = languageIdentifier.identifyLanguage(blocks.joinToString("\n") { it.text }).await()
            .takeUnless { it == "und" }
            ?: page.script.fallbackLanguage
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage)
            ?: TranslateLanguage.fromLanguageTag(page.script.fallbackLanguage)
            ?: return emptyList()
        if (source == TranslateLanguage.FRENCH) return emptyList()

        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(TranslateLanguage.FRENCH)
                .build(),
        )
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            return blocks.mapNotNull { block ->
                val bounds = block.boundingBox ?: return@mapNotNull null
                val translated = translator.translate(block.text).await().trim()
                if (translated.isBlank()) return@mapNotNull null
                val horizontalPadding = max(2f, bounds.width() * 0.06f)
                val verticalPadding = max(2f, bounds.height() * 0.08f)
                val left = (bounds.left - horizontalPadding).coerceAtLeast(0f)
                val top = (bounds.top - verticalPadding).coerceAtLeast(0f)
                val right = (bounds.right + horizontalPadding).coerceAtMost(imageWidth.toFloat())
                val bottom = (bounds.bottom + verticalPadding).coerceAtMost(imageHeight.toFloat())
                ReaderLensRegion(
                    left = (left / imageWidth).coerceIn(0f, 1f),
                    top = (top / imageHeight).coerceIn(0f, 1f),
                    width = ((right - left) / imageWidth).coerceIn(0.02f, 1f),
                    height = ((bottom - top) / imageHeight).coerceIn(0.02f, 1f),
                    text = translated,
                )
            }
        } finally {
            translator.close()
        }
    }

    private fun decodeForLens(streamProvider: () -> InputStream): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        streamProvider().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return streamProvider().use { BitmapFactory.decodeStream(it, null, options) }
    }
}

class ReaderLensException(message: String, cause: Throwable? = null) : Exception(message, cause)

private data class RecognizedPage(val script: LensScript, val text: Text)

private enum class LensScript(val fallbackLanguage: String) {
    Latin(TranslateLanguage.ENGLISH),
    Korean(TranslateLanguage.KOREAN),
    Japanese(TranslateLanguage.JAPANESE),
    Chinese(TranslateLanguage.CHINESE),
}

private fun Text.score(): Int = text.count { !it.isWhitespace() }

private const val MAX_IMAGE_DIMENSION = 2560
private const val MIN_RECOGNIZED_CHARACTERS = 3
private const val EARLY_ACCEPT_CHARACTERS = 12
private const val MAX_TEXT_BLOCKS = 48
