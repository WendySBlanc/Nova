package tachiyomi.domain.release.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetApplicationReleaseTest {

    private lateinit var getApplicationRelease: GetApplicationRelease
    private lateinit var releaseService: ReleaseService
    private lateinit var preference: Preference<Long>

    @BeforeEach
    fun beforeEach() {
        val preferenceStore = mockk<PreferenceStore>()
        preference = mockk()
        every { preferenceStore.getLong(any(), any()) } returns preference
        releaseService = mockk()

        getApplicationRelease = GetApplicationRelease(releaseService, preferenceStore)
    }

    @Test
    fun `When has update but is preview expect new update`() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        val releases = listOf(
            Release(
                "r2000",
                "info",
                "http://example.com/release_link",
                "http://example.com/release_link.apk",
            ),
        )

        coEvery { releaseService.releaseNotes(any()) } returns releases

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isPreview = true,
                commitCount = 1000,
                versionName = "",
                repository = "test",
            ),
        )

        // KMK: Don't cast, will throw exception if the result is different from expected
        result shouldBe GetApplicationRelease.Result.NewUpdate(releases.getLatest()!!)
    }

    @Test
    fun `Preview update ignores stable release tags`() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        val previewRelease = Release(
            "r2000",
            "preview info",
            "http://example.com/preview",
            "http://example.com/preview.apk",
        )
        coEvery { releaseService.releaseNotes(any()) } returns listOf(
            Release(
                "v9.0.0",
                "stable info",
                "http://example.com/stable",
                "http://example.com/stable.apk",
            ),
            previewRelease,
        )

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isPreview = true,
                commitCount = 1000,
                versionName = "",
                repository = "test",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NewUpdate(listOf(previewRelease).getLatest()!!)
    }

    @Test
    fun `Stable update ignores preview release tags`() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        val stableRelease = Release(
            "v2.0.0",
            "stable info",
            "http://example.com/stable",
            "http://example.com/stable.apk",
        )
        coEvery { releaseService.releaseNotes(any()) } returns listOf(
            Release(
                "r2000",
                "preview info",
                "http://example.com/preview",
                "http://example.com/preview.apk",
            ),
            stableRelease,
        )

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isPreview = false,
                commitCount = 0,
                versionName = "v1.0.0",
                repository = "test",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NewUpdate(listOf(stableRelease).getLatest()!!)
    }

    @Test
    fun `When has update expect new update`() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        val releases =
            listOf(
                Release(
                    "v2.0.0",
                    "info",
                    "http://example.com/release_link",
                    "http://example.com/release_link.apk",
                ),
            )

        coEvery { releaseService.releaseNotes(any()) } returns releases

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isPreview = false,
                commitCount = 0,
                versionName = "v1.0.0",
                repository = "test",
            ),
        )

        // KMK: Don't cast, will throw exception if the result is different from expected
        result shouldBe GetApplicationRelease.Result.NewUpdate(releases.getLatest()!!)
    }

    @Test
    fun `When has no update expect no new update`() = runTest {
        every { preference.get() } returns 0
        every { preference.set(any()) }.answers { }

        val releases = listOf(
            Release(
                "v1.0.0",
                "info",
                "http://example.com/release_link",
                "http://example.com/release_link.apk",
            ),
        )

        coEvery { releaseService.releaseNotes(any()) } returns releases

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isPreview = false,
                commitCount = 0,
                versionName = "v2.0.0",
                repository = "test",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    @Test
    fun `When last check is newer than thirty minutes expect no new update`() = runTest {
        every { preference.get() } returns Instant.now().toEpochMilli()
        every { preference.set(any()) }.answers { }

        val releases = listOf(
            Release(
                "v2.0.0",
                "info",
                "http://example.com/release_link",
                "http://example.com/release_link.apk",
            ),
        )

        coEvery { releaseService.releaseNotes(any()) } returns releases

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isPreview = false,
                commitCount = 0,
                versionName = "v1.0.0",
                repository = "test",
            ),
        )

        coVerify(exactly = 0) { releaseService.latest(any()) }
        coVerify(exactly = 0) { releaseService.releaseNotes(any()) }
        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }

    @Test
    fun `When last check is older than thirty minutes expect update check`() = runTest {
        every { preference.get() } returns Instant.now().minus(31, ChronoUnit.MINUTES).toEpochMilli()
        every { preference.set(any()) }.answers { }

        val releases = listOf(
            Release(
                "v2.0.0",
                "info",
                "http://example.com/release_link",
                "http://example.com/release_link.apk",
            ),
        )
        coEvery { releaseService.releaseNotes(any()) } returns releases

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isPreview = false,
                commitCount = 0,
                versionName = "v1.0.0",
                repository = "test",
            ),
        )

        coVerify(exactly = 1) { releaseService.releaseNotes(any()) }
        result shouldBe GetApplicationRelease.Result.NewUpdate(releases.getLatest()!!)
    }
}
