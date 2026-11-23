package eu.kanade.tachiyomi.data.lens

import android.content.Context
import android.graphics.Bitmap
import com.paddle.ocr.EngineConfig
import com.paddle.ocr.PaddleOCR
import com.paddle.ocr.PaddleOCRConfig
import com.paddle.ocr.util.OpenCVUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PaddleReaderLensRecognizer(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var delegate: PaddleOCR? = null

    suspend fun recognize(
        bitmap: Bitmap,
        tile: ReaderLensImageTile,
        imageWidth: Int,
    ): List<ReaderLensTextBlock> = mutex.withLock {
        val ocr = delegate ?: createRecognizer().also { delegate = it }
        val result = runReaderLensNativeInference { ocr.recognize(bitmap) }
        result.results.mapNotNull { item ->
            val points = item.box.points
            mapPaddleOcrBlock(
                left = points.minOf { it.x },
                top = points.minOf { it.y },
                right = points.maxOf { it.x },
                bottom = points.maxOf { it.y },
                text = item.text,
                confidence = item.confidence,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
                tile = tile,
                imageWidth = imageWidth,
            )
        }
    }

    override fun close() {
        delegate?.close()
        delegate = null
    }

    private suspend fun createRecognizer(): PaddleOCR {
        check(OpenCVUtils.init(appContext)) { "OpenCV is unavailable for Reader Lens OCR" }
        return PaddleOCR.create(
            context = appContext,
            config = PaddleOCRConfig(recBatchSize = 8),
            engineConfig = EngineConfig(
                numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4),
            ),
        )
    }
}
