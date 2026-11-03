package eu.kanade.presentation.reader

import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaReaderProgressTest {

    @Test
    fun `image progress uses the active chapter page only`() {
        val progress = resolveNovaReaderProgress(
            currentPage = 5,
            totalPages = 10,
            isTextReader = false,
        )

        progress.sliderValue shouldBe 5
        progress.sliderRange shouldBe 0..10
        progress.label shouldBe "5 / 10"
        progress.fraction.shouldBeExactly(0.5f)
        readerDestinationFromSlider(5, isTextReader = false) shouldBe 4
    }

    @Test
    fun `text progress remains a percentage`() {
        val progress = resolveNovaReaderProgress(
            currentPage = 63,
            totalPages = 0,
            isTextReader = true,
        )

        progress.sliderRange shouldBe 0..100
        progress.label shouldBe "63%"
        progress.fraction.shouldBeExactly(0.63f)
        readerDestinationFromSlider(63, isTextReader = true) shouldBe 63
    }

    @Test
    fun `invalid page values are clamped without jumping chapters`() {
        resolveNovaReaderProgress(-10, 20, isTextReader = false).sliderValue shouldBe 1
        resolveNovaReaderProgress(99, 20, isTextReader = false).sliderValue shouldBe 20
        readerDestinationFromSlider(0, isTextReader = false) shouldBe 0
    }
}
