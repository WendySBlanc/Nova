package eu.kanade.tachiyomi.data.lens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import com.google.mlkit.nl.languageid.LanguageIdentification
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
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class ReaderLensEngine internal constructor(
    private val cache: ReaderLensCache,
    private val translator: ReaderLensTranslator,
    private val latinRecognizer: PaddleReaderLensRecognizer,
) : AutoCloseable {
    private val analysisGate = ReaderLensAnalysisGate()
    private val scriptsByChapter = ConcurrentHashMap<String, ReaderLensScript>()
    private val languageIdentifierDelegate = lazy { LanguageIdentification.getClient() }
    private val languageIdentifier by languageIdentifierDelegate
    private val recognizersDelegate = lazy<Map<ReaderLensScript, TextRecognizer>> {
        mapOf(
            ReaderLensScript.Latin to TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
            ReaderLensScript.Korean to TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
            ReaderLensScript.Japanese to TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            ReaderLensScript.Chinese to TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
        )
    }
    private val recognizers by recognizersDelegate

    suspend fun analyze(
        chapterKey: String,
        pageIndex: Int,
        pageIdentity: String,
        streamProvider: () -> InputStream,
    ): List<ReaderLensRegion> = analysisGate.run {
        withContext(Dispatchers.IO) {
            val imageInfo = readImageInfo(streamProvider)
                ?: throw ReaderLensException("Cette page ne peut pas etre analysee")
            val cacheIdentity = "$pageIdentity:${imageInfo.fingerprint}"
            cache.get(chapterKey, pageIndex, cacheIdentity)?.let { return@withContext it }
            val recognized = recognizeTiles(chapterKey, imageInfo.size, streamProvider)
            val translation = translate(recognized, imageInfo.size.width, imageInfo.size.height)
            if (translation.cacheable) {
                cache.put(chapterKey, pageIndex, cacheIdentity, translation.regions)
            }
            translation.regions
        }
    }

    private suspend fun recognizeTiles(
        chapterKey: String,
        imageSize: ReaderLensImageSize,
        streamProvider: () -> InputStream,
    ): RecognizedPage {
        val tiles = buildReaderLensImageTiles(imageSize.width, imageSize.height)
        val blocks = mutableListOf<ReaderLensTextBlock>()
        var selectedScript = scriptsByChapter[chapterKey]

        streamProvider().use { input ->
            val decoder = BitmapRegionDecoder.newInstance(input, false)
                ?: throw ReaderLensException("Cette page ne peut pas etre decoupee")
            try {
                for (tile in tiles) {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = tile.sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bitmap = decoder.decodeRegion(
                        Rect(0, tile.top, imageSize.width, tile.bottom),
                        options,
                    ) ?: continue
                    try {
                        if (selectedScript == ReaderLensScript.Latin) {
                            val paddleBlocks = latinRecognizer.recognize(bitmap, tile, imageSize.width)
                            if (paddleBlocks.isNotEmpty()) {
                                blocks += paddleBlocks
                                continue
                            }
                        }
                        val recognizedTile = recognizeTile(chapterKey, selectedScript, bitmap)
                        selectedScript = recognizedTile.script
                        val paddleBlocks = if (recognizedTile.script == ReaderLensScript.Latin) {
                            latinRecognizer.recognize(bitmap, tile, imageSize.width)
                        } else {
                            emptyList()
                        }
                        if (paddleBlocks.isNotEmpty()) {
                            blocks += paddleBlocks
                        } else {
                            recognizedTile.text.textBlocks
                                .asSequence()
                                .filter { it.boundingBox != null && it.text.isNotBlank() }
                                .mapNotNull { block ->
                                    val bounds = block.boundingBox ?: return@mapNotNull null
                                    block.toRecognizedBlock(bounds, bitmap, tile, imageSize)
                                }
                                .forEach(blocks::add)
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
            } finally {
                decoder.recycle()
            }
        }

        val script = selectedScript ?: throw ReaderLensException("Aucun texte detecte")
        return RecognizedPage(script, blocks)
    }

    private suspend fun recognizeTile(
        chapterKey: String,
        preferredScript: ReaderLensScript?,
        bitmap: Bitmap,
    ): RecognizedTile {
        val image = InputImage.fromBitmap(bitmap, 0)
        preferredScript?.let { script ->
            val text = runReaderLensNativeInference {
                recognizers.getValue(script).process(image).await()
            }
            if (text.score() >= MIN_RECOGNIZED_CHARACTERS) return RecognizedTile(script, text)
        }

        val results = ReaderLensScript.entries.map { script ->
            script to runReaderLensNativeInference {
                recognizers.getValue(script).process(image).await()
            }
        }
        val selectedScript = selectReaderLensScript(
            results.map { (script, text) -> ReaderLensScriptCandidate(script, text.text) },
        ) ?: throw ReaderLensException("Aucun texte detecte")
        val selectedText = results.first { it.first == selectedScript }.second
        if (selectedText.score() >= MIN_RECOGNIZED_CHARACTERS) {
            scriptsByChapter[chapterKey] = selectedScript
        }
        return RecognizedTile(selectedScript, selectedText)
    }

    private suspend fun translate(
        page: RecognizedPage,
        imageWidth: Int,
        imageHeight: Int,
    ): ReaderLensPageTranslation {
        val dialogueBubbles = requireReaderLensDialogueBlocks(groupReaderLensTextBlocks(page.blocks))
        val sourceLanguage = languageIdentifier.identifyLanguage(
            dialogueBubbles.joinToString("\n") { it.text }.take(MAX_LANGUAGE_SAMPLE_CHARACTERS),
        ).await()
            .takeUnless { it == "und" }
            ?: page.script.fallbackLanguage
        return translateReaderLensPage(
            blocks = dialogueBubbles,
            sourceLanguage = sourceLanguage,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            translator = translator,
        )
    }

    override fun close() {
        if (recognizersDelegate.isInitialized()) {
            recognizers.values.forEach(TextRecognizer::close)
        }
        if (languageIdentifierDelegate.isInitialized()) {
            languageIdentifier.close()
        }
        translator.close()
        latinRecognizer.close()
        scriptsByChapter.clear()
    }

    fun releaseWarmModel() {
        translator.releaseWarmModel()
        latinRecognizer.close()
    }

    private fun readImageInfo(streamProvider: () -> InputStream): ReaderLensImageInfo? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val digest = MessageDigest.getInstance("SHA-256")
        streamProvider().use { rawInput ->
            DigestInputStream(rawInput, digest).use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (input.read(buffer) != -1) Unit
            }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return ReaderLensImageInfo(
            size = ReaderLensImageSize(bounds.outWidth, bounds.outHeight),
            fingerprint = digest.digest().joinToString("") { byte -> "%02x".format(byte) },
        )
    }
}

class ReaderLensException(message: String, cause: Throwable? = null) : Exception(message, cause)

private data class ReaderLensImageSize(val width: Int, val height: Int)

private data class ReaderLensImageInfo(
    val size: ReaderLensImageSize,
    val fingerprint: String,
)

private data class RecognizedTile(val script: ReaderLensScript, val text: Text)

private data class RecognizedPage(
    val script: ReaderLensScript,
    val blocks: List<ReaderLensTextBlock>,
)

private fun Text.TextBlock.toRecognizedBlock(
    bounds: Rect,
    bitmap: Bitmap,
    tile: ReaderLensImageTile,
    imageSize: ReaderLensImageSize,
): ReaderLensTextBlock {
    val tileHeight = tile.bottom - tile.top
    return ReaderLensTextBlock(
        left = bounds.left.toFloat() / bitmap.width * imageSize.width,
        top = tile.top + bounds.top.toFloat() / bitmap.height * tileHeight,
        right = bounds.right.toFloat() / bitmap.width * imageSize.width,
        bottom = tile.top + bounds.bottom.toFloat() / bitmap.height * tileHeight,
        text = text,
    )
}

private fun ReaderLensTextBlock.toRegion(
    translated: String,
    imageWidth: Int,
    imageHeight: Int,
): ReaderLensRegion? {
    if (translated.isBlank()) return null
    val horizontalPadding = max(3f, (right - left) * 0.12f)
    val verticalPadding = max(3f, (bottom - top) * 0.18f)
    val paddedLeft = (left - horizontalPadding).coerceAtLeast(0f)
    val paddedTop = (top - verticalPadding).coerceAtLeast(0f)
    val paddedRight = (right + horizontalPadding).coerceAtMost(imageWidth.toFloat())
    val paddedBottom = (bottom + verticalPadding).coerceAtMost(imageHeight.toFloat())
    return ReaderLensRegion(
        left = (paddedLeft / imageWidth).coerceIn(0f, 1f),
        top = (paddedTop / imageHeight).coerceIn(0f, 1f),
        width = ((paddedRight - paddedLeft) / imageWidth).coerceIn(0.02f, 1f),
        height = ((paddedBottom - paddedTop) / imageHeight).coerceIn(0.02f, 1f),
        text = translated,
    )
}

internal suspend fun translateReaderLensPage(
    blocks: List<ReaderLensTextBlock>,
    sourceLanguage: String,
    imageWidth: Int,
    imageHeight: Int,
    translator: ReaderLensTranslator,
): ReaderLensPageTranslation {
    if (blocks.isEmpty() || sourceLanguage.equals("fr", ignoreCase = true)) {
        return ReaderLensPageTranslation(emptyList(), cacheable = true)
    }
    val translation = translator.translatePage(blocks, sourceLanguage)
    val regions = blocks.mapIndexedNotNull { index, block ->
        val translated = translation.translations["b$index"]
            ?: throw ReaderLensTranslationException("La traduction de la bulle b$index est absente")
        block.toRegion(translated, imageWidth, imageHeight)
    }
    return ReaderLensPageTranslation(regions, translation.cacheable)
}

internal fun requireReaderLensDialogueBlocks(
    blocks: List<ReaderLensTextBlock>,
): List<ReaderLensTextBlock> {
    if (blocks.isEmpty()) throw ReaderLensException("Aucun texte détecté sur cette page")
    return blocks
}

internal data class ReaderLensPageTranslation(
    val regions: List<ReaderLensRegion>,
    val cacheable: Boolean,
)

private fun Text.score(): Int = text.count { !it.isWhitespace() }

private const val MIN_RECOGNIZED_CHARACTERS = 3
private const val MAX_LANGUAGE_SAMPLE_CHARACTERS = 4000
