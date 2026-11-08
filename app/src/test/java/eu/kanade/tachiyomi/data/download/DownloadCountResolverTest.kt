package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DownloadCountResolverTest {

    @Test
    fun `current source count wins over restored folders`() {
        resolveRestoredDownloadCount(preferredCount = 3, fallbackCounts = listOf(7)) shouldBe 3
    }

    @Test
    fun `one restored folder is counted after a source migration`() {
        resolveRestoredDownloadCount(preferredCount = null, fallbackCounts = listOf(7)) shouldBe 7
    }

    @Test
    fun `ambiguous restored folders are not assigned to the wrong series`() {
        resolveRestoredDownloadCount(preferredCount = null, fallbackCounts = listOf(4, 7)) shouldBe 0
    }
}
