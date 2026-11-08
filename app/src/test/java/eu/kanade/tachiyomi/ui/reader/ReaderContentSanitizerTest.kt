package eu.kanade.tachiyomi.ui.reader

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class ReaderContentSanitizerTest {

    @Test
    fun `chapter html keeps reading content and removes executable markup`() {
        val unsafeHtml = """
            <article onclick="AndroidReader.postMessage('bad')">
                <h2>Chapter title</h2>
                <p>Readable <strong>content</strong>.</p>
                <img src="https://cdn.example/page.jpg" onerror="alert(1)" alt="Illustration">
                <a href="javascript:alert(2)">Unsafe link</a>
                <iframe src="https://tracking.example"></iframe>
                <script>AndroidReader.postMessage('bad')</script>
            </article>
        """.trimIndent()

        val sanitized = unsafeHtml.sanitizeReaderContentHtml()

        sanitized.shouldContain("Chapter title")
        sanitized.shouldContain("<strong>content</strong>")
        sanitized.shouldContain("src=\"https://cdn.example/page.jpg\"")
        sanitized.shouldNotContain("onclick")
        sanitized.shouldNotContain("onerror")
        sanitized.shouldNotContain("javascript:")
        sanitized.shouldNotContain("<iframe")
        sanitized.shouldNotContain("<script")
        sanitized.shouldNotContain("AndroidReader")
    }
}
