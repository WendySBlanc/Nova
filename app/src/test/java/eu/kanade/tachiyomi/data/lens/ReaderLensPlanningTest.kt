package eu.kanade.tachiyomi.data.lens

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ReaderLensPlanningTest {

    @Test
    fun `empty OCR result is reported instead of cached as translated`() {
        shouldThrow<ReaderLensException> {
            requireReaderLensDialogueBlocks(emptyList())
        }
    }

    @Test
    fun `long webtoon keeps enough horizontal pixels for OCR`() {
        val tiles = buildReaderLensImageTiles(width = 1080, height = 12_000)

        tiles.size shouldBeGreaterThan 1
        tiles.first().outputWidth shouldBe 1080
        tiles.forEach { tile ->
            (tile.outputWidth * tile.outputHeight) shouldBeLessThanOrEqual READER_LENS_MAX_TILE_PIXELS
        }
        tiles.first().top shouldBe 0
        tiles.last().bottom shouldBe 12_000
    }

    @Test
    fun `wide image is sampled without using its height as the scale factor`() {
        val tiles = buildReaderLensImageTiles(width = 5000, height = 14_000)

        tiles.first().sampleSize shouldBe 4
        tiles.first().outputWidth shouldBe 1250
        tiles.last().bottom shouldBe 14_000
    }

    @Test
    fun `script selection prefers matching characters instead of the first recognizer`() {
        val selected = selectReaderLensScript(
            listOf(
                ReaderLensScriptCandidate(ReaderLensScript.Latin, "I1l0 random OCR noise"),
                ReaderLensScriptCandidate(ReaderLensScript.Japanese, "これは日本語の吹き出しです"),
                ReaderLensScriptCandidate(ReaderLensScript.Korean, "abc"),
            ),
        )

        selected shouldBe ReaderLensScript.Japanese
    }

    @Test
    fun `nearby dialogue lines become one translated bubble`() {
        val groups = groupReaderLensTextBlocks(
            listOf(
                ReaderLensTextBlock(100f, 100f, 300f, 145f, "Isn't this"),
                ReaderLensTextBlock(108f, 150f, 292f, 195f, "a beautiful view?"),
            ),
        )

        groups.shouldHaveSize(1)
        groups.single() shouldBe ReaderLensTextBlock(
            left = 100f,
            top = 100f,
            right = 300f,
            bottom = 195f,
            text = "Isn't this a beautiful view?",
        )
    }

    @Test
    fun `wrapped OCR word is rejoined before translation`() {
        val groups = groupReaderLensTextBlocks(
            listOf(
                ReaderLensTextBlock(100f, 100f, 300f, 145f, "THEIR REINFORCEMENTS AR-"),
                ReaderLensTextBlock(108f, 150f, 292f, 195f, "RIVED QUICKLY."),
            ),
        )

        groups.single().text shouldBe "THEIR REINFORCEMENTS ARRIVED QUICKLY."
    }

    @Test
    fun `scan credits are excluded from translated dialogue`() {
        val groups = groupReaderLensTextBlocks(
            listOf(
                ReaderLensTextBlock(100f, 100f, 400f, 180f, "WAKE UP ALREADY! WE'RE LEAVING!"),
                ReaderLensTextBlock(700f, 900f, 980f, 940f, "IMPERFECTCOMIC.COM"),
            ),
        )

        groups.map { it.text } shouldBe listOf("WAKE UP ALREADY! WE'RE LEAVING!")
    }

    @Test
    fun `PP OCR coordinates are mapped from tile to full page`() {
        val block = mapPaddleOcrBlock(
            left = 10f,
            top = 20f,
            right = 110f,
            bottom = 60f,
            text = "WAKE UP ALREADY!",
            confidence = 0.92f,
            bitmapWidth = 200,
            bitmapHeight = 100,
            tile = ReaderLensImageTile(
                top = 100,
                bottom = 300,
                sampleSize = 1,
                outputWidth = 200,
                outputHeight = 100,
            ),
            imageWidth = 400,
        )

        block shouldBe ReaderLensTextBlock(
            left = 20f,
            top = 140f,
            right = 220f,
            bottom = 220f,
            text = "WAKE UP ALREADY!",
        )
    }

    @Test
    fun `low confidence PP OCR line is ignored`() {
        mapPaddleOcrBlock(
            left = 10f,
            top = 20f,
            right = 110f,
            bottom = 60f,
            text = "eant",
            confidence = 0.32f,
            bitmapWidth = 200,
            bitmapHeight = 100,
            tile = ReaderLensImageTile(0, 100, 1, 200, 100),
            imageWidth = 200,
        ) shouldBe null
    }

    @Test
    fun `non Latin PP OCR watermark is ignored`() {
        mapPaddleOcrBlock(
            left = 10f,
            top = 20f,
            right = 110f,
            bottom = 60f,
            text = "腾讯动漫",
            confidence = 0.99f,
            bitmapWidth = 200,
            bitmapHeight = 100,
            tile = ReaderLensImageTile(0, 100, 1, 200, 100),
            imageWidth = 200,
        ) shouldBe null
    }

    @Test
    fun `distant dialogue bubbles remain independent`() {
        val groups = groupReaderLensTextBlocks(
            listOf(
                ReaderLensTextBlock(80f, 80f, 260f, 130f, "First bubble"),
                ReaderLensTextBlock(520f, 420f, 760f, 480f, "Second bubble"),
            ),
        )

        groups.shouldHaveSize(2)
    }

    @Test
    fun `a grouped bubble does not absorb another aligned bubble below it`() {
        val groups = groupReaderLensTextBlocks(
            listOf(
                ReaderLensTextBlock(100f, 100f, 300f, 145f, "First line"),
                ReaderLensTextBlock(108f, 150f, 292f, 195f, "second line"),
                ReaderLensTextBlock(110f, 285f, 290f, 330f, "Another bubble"),
            ),
        )

        groups.shouldHaveSize(2)
    }

    @Test
    fun `disabled Lens never starts while a chapter opens`() {
        shouldRunReaderLens(pageIndex = 0, available = true, enabled = false) shouldBe false
        shouldRunReaderLens(pageIndex = 0, available = true, enabled = true) shouldBe true
        shouldRunReaderLens(pageIndex = -1, available = true, enabled = true) shouldBe false
    }

    @Test
    fun `image chapter offers Lens before reader html is built`() {
        shouldOfferReaderLens(pageCount = 1) shouldBe true
        shouldOfferReaderLens(pageCount = 120) shouldBe true
        shouldOfferReaderLens(pageCount = 0) shouldBe false
    }

    @Test
    fun `regular page uses one complete tile`() {
        val tiles = buildReaderLensImageTiles(width = 900, height = 1400)

        tiles.shouldHaveSize(1)
        tiles.single().top shouldBe 0
        tiles.single().bottom shouldBe 1400
    }

    @Test
    fun `cancelling Lens waits for native inference before releasing its image`() = runTest {
        val inferenceStarted = CompletableDeferred<Unit>()
        val inferenceFinished = CompletableDeferred<Unit>()
        val job = launch {
            runReaderLensNativeInference {
                inferenceStarted.complete(Unit)
                inferenceFinished.await()
            }
        }

        runCurrent()
        inferenceStarted.await()
        job.cancel()
        runCurrent()

        job.isCompleted shouldBe false
        inferenceFinished.complete(Unit)
        job.join()
        job.isCancelled shouldBe true
    }

    @Test
    fun `Lens analysis gate never runs two native analyses together`() = runTest {
        val gate = ReaderLensAnalysisGate()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        var activeAnalyses = 0
        var maximumActiveAnalyses = 0

        val first = launch {
            gate.run {
                activeAnalyses++
                maximumActiveAnalyses = maxOf(maximumActiveAnalyses, activeAnalyses)
                firstStarted.complete(Unit)
                releaseFirst.await()
                activeAnalyses--
            }
        }
        firstStarted.await()
        val second = launch {
            gate.run {
                activeAnalyses++
                maximumActiveAnalyses = maxOf(maximumActiveAnalyses, activeAnalyses)
                activeAnalyses--
            }
        }

        runCurrent()
        maximumActiveAnalyses shouldBe 1
        releaseFirst.complete(Unit)
        first.join()
        second.join()
        maximumActiveAnalyses shouldBe 1
    }
}
