package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class LibraryUpdatePolicyTest {

    @Test
    fun `manual refresh includes titles configured to fetch once`() {
        shouldSkipLibraryUpdate(
            updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
            totalChapters = 12L,
            forceUpdate = true,
        ) shouldBe false
    }

    @Test
    fun `automatic refresh still skips titles configured to fetch once`() {
        shouldSkipLibraryUpdate(
            updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
            totalChapters = 12L,
            forceUpdate = false,
        ) shouldBe true
    }

    @Test
    fun `library update cleanup runs when preparation fails`() = runTest {
        val events = mutableListOf<String>()

        runCatching {
            runLibraryUpdateLifecycle(
                onStart = { events += "start" },
                onFinish = { events += "finish" },
            ) {
                events += "prepare"
                error("preparation failed")
            }
        }

        events shouldBe listOf("start", "prepare", "finish")
    }
}
