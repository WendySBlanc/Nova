package mihon.domain.extensionrepo.interactor

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.model.LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT
import mihon.domain.extensionrepo.model.NOVA_EXTENSION_REPO_BASE_URL
import mihon.domain.extensionrepo.model.NOVA_SIGNING_KEY_FINGERPRINT
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import mihon.domain.extensionrepo.service.ExtensionRepoService

class UpdateExtensionRepo(
    private val repository: ExtensionRepoRepository,
    private val service: ExtensionRepoService,
) {

    suspend fun awaitAll() = coroutineScope {
        repository.getAll()
            .map { async { await(it) } }
            .awaitAll()
    }

    suspend fun await(repo: ExtensionRepo) {
        val migrateToNova = repo.signingKeyFingerprint == LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT ||
            repo.baseUrl.contains("keiyoushi/extensions", ignoreCase = true)
        val targetBaseUrl = if (migrateToNova) NOVA_EXTENSION_REPO_BASE_URL else repo.baseUrl
        val newRepo = service.fetchRepoDetails(targetBaseUrl) ?: return
        if (
            repo.signingKeyFingerprint.startsWith("NOFINGERPRINT") ||
            repo.signingKeyFingerprint == newRepo.signingKeyFingerprint ||
            migrateToNova && newRepo.signingKeyFingerprint == NOVA_SIGNING_KEY_FINGERPRINT
        ) {
            if (migrateToNova) {
                repository.upsertRepo(newRepo)
                if (repo.baseUrl != newRepo.baseUrl) {
                    repository.deleteRepo(repo.baseUrl)
                }
            } else if (repo.baseUrl == newRepo.baseUrl) {
                repository.upsertRepo(newRepo)
            } else {
                repository.replaceRepo(newRepo)
            }
        }
    }
}
