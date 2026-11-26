package eu.kanade.tachiyomi.data.library

import androidx.work.WorkInfo
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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

    @Test
    fun `an unresponsive source is reported and does not stop the next title from updating`() = runTest {
        val failure = runCatching { withLibrarySourceDeadline(100) { awaitCancellation() } }.exceptionOrNull()

        (failure is LibrarySourceTimeoutException) shouldBe true
        withLibrarySourceDeadline(100) { listOf("chapter") } shouldBe listOf("chapter")
    }

    @Test
    fun `an extension inner timeout is a title error but a parent timeout is cancellation`() = runTest {
        val innerFailure = runCatching {
            withLibrarySourceDeadline(100) { withTimeout(10) { awaitCancellation() } }
        }.exceptionOrNull()
        val parentFailure = runCatching {
            withTimeout(10) { withLibrarySourceDeadline(100) { awaitCancellation() } }
        }.exceptionOrNull()

        (innerFailure is LibrarySourceTimeoutException) shouldBe true
        (parentFailure is TimeoutCancellationException) shouldBe true
    }

    @Test
    fun `cancelling a refresh does not report a source timeout`() = runTest {
        val started = CompletableDeferred<Unit>()
        var failure: Throwable? = null
        val job = launch {
            failure = runCatching {
                withLibrarySourceDeadline(100) {
                    started.complete(Unit)
                    awaitCancellation()
                }
            }.exceptionOrNull()
        }
        started.await()
        job.cancelAndJoin()

        (failure is kotlinx.coroutines.CancellationException) shouldBe true
    }

    @Test
    fun `concurrent progress counts each processed title once and ends at the real total`() = runTest {
        val events = mutableListOf<Pair<List<String>, Int>>()
        val progress = LibraryRefreshProgress<String>(2) { active, completed, total ->
            total shouldBe 2
            events += active to completed
        }
        val started = CompletableDeferred<Unit>()
        val proceed = CompletableDeferred<Unit>()
        val first = async {
            progress.track("first") {
                started.complete(Unit)
                proceed.await()
            }
        }
        started.await()
        progress.track("removed from library") {}
        proceed.complete(Unit)
        first.await()

        events shouldBe listOf(
            listOf("first") to 0,
            listOf("first", "removed from library") to 0,
            listOf("first") to 1,
            emptyList<String>() to 2,
        )
    }
}
