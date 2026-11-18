package eu.kanade.tachiyomi.ui.reader.loader

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class ArchivePageStreamStoreTest {

    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun `concurrent consumers receive independent streams from one archive extraction`() {
        val archiveReads = AtomicInteger()
        val expected = "downloaded chapter page".encodeToByteArray()
        val store = ArchivePageStreamStore(temporaryDirectory) {
            archiveReads.incrementAndGet()
            Thread.sleep(50)
            ByteArrayInputStream(expected)
        }
        val executor = Executors.newFixedThreadPool(4)

        val results = try {
            executor.invokeAll(
                List<Callable<String>>(4) {
                    Callable {
                        store.open(pageIndex = 3, entryName = "chapter/page-004.webp")
                            .use { it.readBytes().decodeToString() }
                    }
                },
            ).map { it.get() }
        } finally {
            executor.shutdownNow()
        }

        results.shouldContainExactly(List(4) { expected.decodeToString() })
        archiveReads.get() shouldBe 1
    }
}
