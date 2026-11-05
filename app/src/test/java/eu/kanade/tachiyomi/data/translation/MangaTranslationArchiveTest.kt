package eu.kanade.tachiyomi.data.translation

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MangaTranslationArchiveTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `upload streams pages in deterministic index order`() = runTest {
        val archive = MangaTranslationArchive(File(temporaryDirectory.toFile(), "uploads"))
        val pages = listOf(
            page(2, jpeg("third")),
            page(0, png("first")),
            page(1, webp("second")),
        )

        val upload = archive.createUpload("source:manga:chapter", pages)
        val entries = readZip(upload)

        entries.keys.toList().shouldContainExactly("000000.png", "000001.webp", "000002.jpg")
        entries.values.map(::payload).shouldContainExactly("first", "second", "third")
    }

    @Test
    fun `upload rejects unreadable pages without leaving partial files`() = runTest {
        val uploads = File(temporaryDirectory.toFile(), "uploads")
        val archive = MangaTranslationArchive(uploads)
        val pages = listOf(
            page(0, png("first")),
            ReaderPage(1, stream = { throw IOException("offline") }),
        )

        shouldThrow<MangaTranslationProtocolException> {
            archive.createUpload("source:manga:broken", pages)
        }

        uploads.walkTopDown().filter { it.isFile }.toList() shouldBe emptyList()
    }

    @Test
    fun `cache rejects path traversal`() {
        val cache = cache()
        val result = zipOf(
            "../000000.png" to png("outside"),
            "000001.png" to png("second"),
        )

        shouldThrow<MangaTranslationProtocolException> {
            cache.installResult(metadata(totalPages = 2), result)
        }

        File(temporaryDirectory.toFile(), "000000.png").exists() shouldBe false
    }

    @Test
    fun `cache rejects duplicate page indexes`() {
        val cache = cache()
        val result = zipOf(
            "000000.png" to png("first"),
            "000000.jpg" to jpeg("duplicate"),
            "000001.png" to png("second"),
        )

        shouldThrow<MangaTranslationProtocolException> {
            cache.installResult(metadata(totalPages = 2), result)
        }
    }

    @Test
    fun `cache rejects a page count mismatch`() {
        val cache = cache()
        val result = zipOf("000000.png" to png("first"))

        shouldThrow<MangaTranslationProtocolException> {
            cache.installResult(metadata(totalPages = 2), result)
        }
    }

    @Test
    fun `cache rejects files disguised as translated images`() {
        val cache = cache()
        val result = zipOf("000000.png" to "not-an-image".encodeToByteArray())

        shouldThrow<MangaTranslationProtocolException> {
            cache.installResult(metadata(totalPages = 1), result)
        }
    }

    @Test
    fun `failed replacement keeps the previously installed translation`() {
        val cache = cache()
        val firstMetadata = metadata(totalPages = 2, configRevision = "config-v1")
        val first = zipOf(
            "000000.png" to png("first-v1"),
            "000001.png" to png("second-v1"),
        )
        cache.installResult(firstMetadata, first)

        val invalidReplacement = zipOf("000000.png" to png("first-v2"))
        shouldThrow<MangaTranslationProtocolException> {
            cache.installResult(
                metadata(totalPages = 2, configRevision = "config-v2"),
                invalidReplacement,
            )
        }

        payload(cache.translatedPage(CHAPTER_KEY, 0)!!.readBytes()) shouldBe "first-v1"
        cache.translatedPage(CHAPTER_KEY, 1)!!.exists() shouldBe true
    }

    @Test
    fun `cache cleans stale installation directories and partial files`() {
        val root = File(temporaryDirectory.toFile(), "cache")
        val staleDirectory = File(root, "chapter/.install-abandoned").apply { mkdirs() }
        File(staleDirectory, "page.png").writeBytes(png("stale"))
        val staleFile = File(root, "chapter/active.part").apply { writeText("stale") }

        MangaTranslationCache(root)

        staleDirectory.exists() shouldBe false
        staleFile.exists() shouldBe false
    }

    private fun cache() = MangaTranslationCache(File(temporaryDirectory.toFile(), "cache"))

    private fun metadata(
        totalPages: Int,
        configRevision: String = "config-v1",
    ) = TranslationResultMetadata(
        chapterKey = CHAPTER_KEY,
        targetLanguage = "French",
        configRevision = configRevision,
        totalPages = totalPages,
    )

    private fun page(index: Int, bytes: ByteArray) = ReaderPage(index, stream = { bytes.inputStream() })

    private fun zipOf(vararg entries: Pair<String, ByteArray>): File {
        val file = File.createTempFile("translation-result-", ".zip", temporaryDirectory.toFile())
        ZipOutputStream(file.outputStream().buffered()).use { output ->
            entries.forEach { (name, bytes) ->
                output.putNextEntry(ZipEntry(name).apply { time = 0L })
                output.write(bytes)
                output.closeEntry()
            }
        }
        return file
    }

    private fun readZip(file: File): LinkedHashMap<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(file.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                entries[entry.name] = input.readBytes()
                input.closeEntry()
            }
        }
        return entries
    }

    private fun png(text: String) = PNG_SIGNATURE + text.encodeToByteArray()

    private fun jpeg(text: String) = JPEG_SIGNATURE + text.encodeToByteArray()

    private fun webp(text: String) = WEBP_SIGNATURE + text.encodeToByteArray()

    private fun payload(bytes: ByteArray): String = bytes
        .dropWhile { it.toInt().toChar() !in 'a'..'z' }
        .toByteArray()
        .decodeToString()

    private companion object {
        const val CHAPTER_KEY = "source:manga:chapter"

        val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
        )
        val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val WEBP_SIGNATURE = "RIFF0000WEBP".encodeToByteArray()
    }
}
