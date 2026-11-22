package mihon.feature.migration.list.search

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException

class BaseSmartSearchEngineTest {

    @Test
    fun `deep search keeps successful variants when another variant fails`() = runTest {
        val engine = TestSearchEngine()

        val result = engine.search("Hero Academy") { query ->
            when (query) {
                "hero academy" -> throw IOException("temporary source failure")
                "academy" -> listOf("Hero Academy")
                else -> emptyList()
            }
        }

        result shouldBe "Hero Academy"
    }

    private class TestSearchEngine : BaseSmartSearchEngine<String>() {
        override fun getTitle(result: String): String = result

        suspend fun search(title: String, action: SearchAction<String>): String? {
            return deepSearch(action, title)
        }
    }
}
