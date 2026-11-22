package exh.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchEngineTest {

    private val engine = SearchEngine()

    @Test
    fun `excluded modifier only applies to the next component`() {
        val components = engine.parseQuery("-ancien nouveau")

        components.map(QueryComponent::excluded) shouldBe listOf(true, false)
    }

    @Test
    fun `exact modifier only applies to the next component`() {
        val components = engine.parseQuery("${'$'}exact normal")

        components.map(QueryComponent::exact) shouldBe listOf(true, false)
    }
}
