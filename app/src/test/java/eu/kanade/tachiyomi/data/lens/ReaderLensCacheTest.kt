package eu.kanade.tachiyomi.data.lens

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ReaderLensCacheTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `regions survive a cache round trip`() {
        val cache = ReaderLensCache(temporaryDirectory.toFile(), Json)
        val regions = listOf(
            ReaderLensRegion(
                left = 0.1f,
                top = 0.2f,
                width = 0.3f,
                height = 0.1f,
                text = "Bonjour <Lens>",
            ),
        )

        cache.put("source:manga:chapter", 7, regions)

        cache.get("source:manga:chapter", 7) shouldBe regions
        cache.get("source:manga:chapter", 8) shouldBe null
    }
}
