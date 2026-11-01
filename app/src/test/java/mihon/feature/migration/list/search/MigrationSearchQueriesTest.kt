package mihon.feature.migration.list.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MigrationSearchQueriesTest {

    @Test
    fun `keeps exact title first and adds distinctive fallbacks`() {
        val queries = MigrationSearchQueries.build("SSS-Class Revival Hunter (Official)")

        assertEquals("SSS-Class Revival Hunter (Official)", queries.first())
        assertTrue("Revival Hunter" in queries)
        assertTrue("Revival" in queries)
        assertTrue("Hunter" in queries)
    }

    @Test
    fun `adds an accent-free fallback without duplicates`() {
        val queries = MigrationSearchQueries.build("L'Épée du héros")

        assertTrue("Epee heros" in queries)
        assertEquals(queries.size, queries.map(String::lowercase).distinct().size)
    }
}
