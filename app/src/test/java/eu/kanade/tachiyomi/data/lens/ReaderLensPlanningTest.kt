package eu.kanade.tachiyomi.data.lens

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
    fun `disabled Lens never starts while a chapter opens`() {
        shouldRunReaderLens(pageIndex = 0, available = true, enabled = false) shouldBe false
        shouldRunReaderLens(pageIndex = 0, available = true, enabled = true) shouldBe true
        shouldRunReaderLens(pageIndex = -1, available = true, enabled = true) shouldBe false
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
