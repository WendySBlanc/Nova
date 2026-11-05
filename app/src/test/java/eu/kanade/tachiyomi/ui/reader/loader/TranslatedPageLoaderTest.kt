package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.data.translation.InstalledTranslation
import eu.kanade.tachiyomi.data.translation.TranslationResultMetadata
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TranslatedPageLoaderTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `loads the persistent translated pages directly`() = runTest {
        val directory = temporaryDirectory.toFile()
        val files = listOf("first", "second").mapIndexed { index, text ->
            directory.resolve("%06d.png".format(index)).apply { writeText(text) }
        }
        val translation = InstalledTranslation(
            metadata = TranslationResultMetadata("chapter", "French", "config", files.size),
            directory = directory,
            pages = files,
        )

        val pages = TranslatedPageLoader(translation).getPages()

        pages.map { page ->
            page.stream!!.invoke().bufferedReader().use { it.readText() }
        }
            .shouldContainExactly("first", "second")
    }
}
