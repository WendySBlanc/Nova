package mihon.domain.extensionrepo.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.model.LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT
import mihon.domain.extensionrepo.model.NOVA_EXTENSION_REPO_BASE_URL
import mihon.domain.extensionrepo.model.NOVA_SIGNING_KEY_FINGERPRINT
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import mihon.domain.extensionrepo.service.ExtensionRepoService
import org.junit.jupiter.api.Test

class UpdateExtensionRepoTest {

    private val repository = mockk<ExtensionRepoRepository>(relaxed = true)
    private val service = mockk<ExtensionRepoService>()
    private val updateExtensionRepo = UpdateExtensionRepo(repository, service)

    @Test
    fun `migrates a legacy Keiyoushi repository to Nova`() = runTest {
        val legacyRepo = repo(
            baseUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo",
            fingerprint = LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT,
        )
        val novaRepo = repo(
            baseUrl = NOVA_EXTENSION_REPO_BASE_URL,
            fingerprint = NOVA_SIGNING_KEY_FINGERPRINT,
        )
        coEvery { service.fetchRepoDetails(NOVA_EXTENSION_REPO_BASE_URL) } returns novaRepo

        updateExtensionRepo.await(legacyRepo)

        coVerify(exactly = 1) { repository.upsertRepo(novaRepo) }
        coVerify(exactly = 1) { repository.deleteRepo(legacyRepo.baseUrl) }
        coVerify(exactly = 0) { repository.replaceRepo(any()) }
    }

    @Test
    fun `does not replace a repository when its signing key unexpectedly changes`() = runTest {
        val installedRepo = repo(
            baseUrl = "https://example.org/extensions",
            fingerprint = "trusted-key",
        )
        val fetchedRepo = installedRepo.copy(signingKeyFingerprint = "different-key")
        coEvery { service.fetchRepoDetails(installedRepo.baseUrl) } returns fetchedRepo

        updateExtensionRepo.await(installedRepo)

        coVerify(exactly = 0) { repository.upsertRepo(any<ExtensionRepo>()) }
        coVerify(exactly = 0) { repository.replaceRepo(any()) }
        coVerify(exactly = 0) { repository.deleteRepo(any()) }
    }

    @Test
    fun `keeps repository state unchanged when metadata cannot be fetched`() = runTest {
        val installedRepo = repo(
            baseUrl = "https://example.org/extensions",
            fingerprint = "trusted-key",
        )
        coEvery { service.fetchRepoDetails(installedRepo.baseUrl) } returns null

        updateExtensionRepo.await(installedRepo)

        coVerify(exactly = 0) { repository.upsertRepo(any<ExtensionRepo>()) }
        coVerify(exactly = 0) { repository.replaceRepo(any()) }
        coVerify(exactly = 0) { repository.deleteRepo(any()) }
    }

    private fun repo(baseUrl: String, fingerprint: String) = ExtensionRepo(
        baseUrl = baseUrl,
        name = "Extensions",
        shortName = "EXT",
        website = "https://example.org",
        signingKeyFingerprint = fingerprint,
    )
}
