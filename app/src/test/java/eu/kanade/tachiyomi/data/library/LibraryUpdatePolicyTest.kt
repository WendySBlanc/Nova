package eu.kanade.tachiyomi.data.library

import androidx.work.WorkInfo
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
    fun `manual refresh also refreshes metadata and covers`() {
        shouldRefreshLibraryMetadata(
            autoUpdateMetadata = false,
            forceUpdate = true,
        ) shouldBe true
    }

    @Test
    fun `automatic refresh follows metadata preference`() {
        shouldRefreshLibraryMetadata(autoUpdateMetadata = true, forceUpdate = false) shouldBe true
        shouldRefreshLibraryMetadata(autoUpdateMetadata = false, forceUpdate = false) shouldBe false
    }

    @Test
    fun `queued blocked and running library jobs are active`() {
        WorkInfo.State.entries.associateWith(::isActiveLibraryWorkState) shouldBe mapOf(
            WorkInfo.State.ENQUEUED to true,
            WorkInfo.State.RUNNING to true,
            WorkInfo.State.SUCCEEDED to false,
            WorkInfo.State.FAILED to false,
            WorkInfo.State.BLOCKED to true,
            WorkInfo.State.CANCELLED to false,
        )
    }

    @Test
    fun `manual work detects its queued sync chain without treating periodic wait as active`() {
        hasActiveManualLibraryWork(listOf(WorkInfo.State.BLOCKED, WorkInfo.State.RUNNING)) shouldBe true
        hasActiveManualLibraryWork(listOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED)) shouldBe false

        hasRunningAutomaticLibraryWork(listOf(WorkInfo.State.ENQUEUED)) shouldBe false
        hasRunningAutomaticLibraryWork(listOf(WorkInfo.State.RUNNING)) shouldBe true
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
