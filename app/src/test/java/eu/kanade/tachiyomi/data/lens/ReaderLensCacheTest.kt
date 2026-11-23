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

        cache.put("source:manga:chapter", 7, "image-a", regions)

        cache.get("source:manga:chapter", 7, "image-a") shouldBe regions
        cache.get("source:manga:chapter", 7, "image-b") shouldBe null
        cache.get("source:manga:chapter", 8, "image-a") shouldBe null

        cache.put("source:manga:chapter", 7, "image-b", regions)

        cache.get("source:manga:chapter", 7, "image-a") shouldBe null
        cache.get("source:manga:chapter", 7, "image-b") shouldBe regions
    }

    @Test
    fun `model and prompt changes invalidate cached translations`() {
        val regions = listOf(
            ReaderLensRegion(0.1f, 0.2f, 0.3f, 0.1f, "Bonjour"),
        )
        val original = ReaderLensCache(
            rootDirectory = temporaryDirectory.toFile(),
            json = Json,
            modelIdentity = "model-a",
            promptSchemaVersion = 1,
        )
        original.put("chapter", 0, "image", regions)

        ReaderLensCache(
            rootDirectory = temporaryDirectory.toFile(),
            json = Json,
            modelIdentity = "model-b",
            promptSchemaVersion = 1,
        ).get("chapter", 0, "image") shouldBe null
        ReaderLensCache(
            rootDirectory = temporaryDirectory.toFile(),
            json = Json,
            modelIdentity = "model-a",
            promptSchemaVersion = 2,
        ).get("chapter", 0, "image") shouldBe null
    }

    @Test
    fun `OCR engine changes invalidate cached translations`() {
        val regions = listOf(
            ReaderLensRegion(0.1f, 0.2f, 0.3f, 0.1f, "Ancienne traduction"),
        )
        ReaderLensCache(
            rootDirectory = temporaryDirectory.toFile(),
            json = Json,
            ocrSchemaVersion = 1,
        ).put("chapter", 0, "image", regions)

        ReaderLensCache(
            rootDirectory = temporaryDirectory.toFile(),
            json = Json,
            ocrSchemaVersion = 2,
        ).get("chapter", 0, "image") shouldBe null
    }
}
