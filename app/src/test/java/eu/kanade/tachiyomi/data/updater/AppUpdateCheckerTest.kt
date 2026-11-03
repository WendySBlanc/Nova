package eu.kanade.tachiyomi.data.updater

import eu.kanade.tachiyomi.BuildConfig
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class AppUpdateCheckerTest {

    @Test
    fun `Nova update checks use the Nova repository`() {
        getGithubRepo() shouldBe BuildConfig.NOVA_UPDATER_REPOSITORY
        getGithubRepo(peekIntoPreview = true) shouldBe BuildConfig.NOVA_UPDATER_REPOSITORY
        getGithubRepo() shouldNotContain "komikku-app"
    }
}
