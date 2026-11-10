package eu.kanade.tachiyomi.data.lens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class ReaderLensEngine(
    private val cache: ReaderLensCache,
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
    private val translators = ConcurrentHashMap<String, Translator>()
    private val downloadedModels = ConcurrentHashMap.newKeySet<String>()
    private val modelDownloadMutex = Mutex()
    private val translationDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_TRANSLATIONS)

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
            val regions = translate(recognized, imageInfo.size.width, imageInfo.size.height)
            cache.put(chapterKey, pageIndex, cacheIdentity, regions)
            regions
        }
    }

    private suspend fun recognizeTiles(
        chapterKey: String,
        imageSize: ReaderLensImageSize,
        streamProvider: () -> InputStream,
    ): RecognizedPage {
        val tiles = buildReaderLensImageTiles(imageSize.width, imageSize.height)
        val blocks = mutableListOf<RecognizedBlock>()
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
                        val recognizedTile = recognizeTile(chapterKey, selectedScript, bitmap)
                        selectedScript = recognizedTile.script
                        recognizedTile.text.textBlocks
                            .asSequence()
                            .filter { it.boundingBox != null && it.text.isNotBlank() }
                            .mapNotNull { block ->
                                val bounds = block.boundingBox ?: return@mapNotNull null
                                block.toRecognizedBlock(bounds, bitmap, tile, imageSize)
                            }
                            .forEach(blocks::add)
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
    ): List<ReaderLensRegion> {
        if (page.blocks.isEmpty()) return emptyList()
        val sourceLanguage = languageIdentifier.identifyLanguage(
            page.blocks.joinToString("\n") { it.text }.take(MAX_LANGUAGE_SAMPLE_CHARACTERS),
        ).await()
            .takeUnless { it == "und" }
            ?: page.script.fallbackLanguage
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage)
            ?: TranslateLanguage.fromLanguageTag(page.script.fallbackLanguage)
            ?: return emptyList()
        if (source == TranslateLanguage.FRENCH) return emptyList()

        val translator = translators[source] ?: synchronized(translators) {
            translators[source] ?: Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(TranslateLanguage.FRENCH)
                    .build(),
            ).also { translators[source] = it }
        }
        modelDownloadMutex.withLock {
            if (source !in downloadedModels) {
                try {
                    translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                    downloadedModels.add(source)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    throw ReaderLensException(
                        "Le modele Lens doit etre telecharge. Verifiez la connexion internet.",
                        error,
                    )
                }
            }
        }

        return coroutineScope {
            page.blocks.map { block ->
                async(translationDispatcher) {
                    val translated = translator.translate(block.text).await().trim()
                    block.toRegion(translated, imageWidth, imageHeight)
                }
            }.awaitAll().filterNotNull()
        }
    }

    override fun close() {
        if (recognizersDelegate.isInitialized()) {
            recognizers.values.forEach(TextRecognizer::close)
        }
        if (languageIdentifierDelegate.isInitialized()) {
            languageIdentifier.close()
        }
        translators.values.forEach(Translator::close)
        translators.clear()
        downloadedModels.clear()
        scriptsByChapter.clear()
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
    val blocks: List<RecognizedBlock>,
)

private data class RecognizedBlock(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val text: String,
)

private fun Text.TextBlock.toRecognizedBlock(
    bounds: Rect,
    bitmap: Bitmap,
    tile: ReaderLensImageTile,
    imageSize: ReaderLensImageSize,
): RecognizedBlock {
    val tileHeight = tile.bottom - tile.top
    return RecognizedBlock(
        left = bounds.left.toFloat() / bitmap.width * imageSize.width,
        top = tile.top + bounds.top.toFloat() / bitmap.height * tileHeight,
        right = bounds.right.toFloat() / bitmap.width * imageSize.width,
        bottom = tile.top + bounds.bottom.toFloat() / bitmap.height * tileHeight,
        text = text,
    )
}

private fun RecognizedBlock.toRegion(
    translated: String,
    imageWidth: Int,
    imageHeight: Int,
): ReaderLensRegion? {
    if (translated.isBlank()) return null
    val horizontalPadding = max(2f, (right - left) * 0.08f)
    val verticalPadding = max(2f, (bottom - top) * 0.12f)
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

private fun Text.score(): Int = text.count { !it.isWhitespace() }

private const val MIN_RECOGNIZED_CHARACTERS = 3
private const val MAX_LANGUAGE_SAMPLE_CHARACTERS = 4000
private const val MAX_PARALLEL_TRANSLATIONS = 3
