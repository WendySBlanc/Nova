package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupExtensionRepos
import mihon.domain.extensionrepo.interactor.GetExtensionRepo
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.model.LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT
import mihon.domain.extensionrepo.model.NOVA_EXTENSION_REPO_BASE_URL
import mihon.domain.extensionrepo.model.NOVA_SIGNING_KEY_FINGERPRINT
import tachiyomi.data.DatabaseHandler
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionRepoRestorer(
    private val handler: DatabaseHandler = Injekt.get(),
    private val getExtensionRepos: GetExtensionRepo = Injekt.get(),
) {

    suspend operator fun invoke(
        backupRepo: BackupExtensionRepos,
    ) {
        val migrateToNova = backupRepo.signingKeyFingerprint == LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT ||
            backupRepo.baseUrl.contains("keiyoushi/extensions", ignoreCase = true)
        val restoredRepo = if (migrateToNova) {
            ExtensionRepo(
                baseUrl = NOVA_EXTENSION_REPO_BASE_URL,
                name = "Nova Extensions",
                shortName = "NOVA",
                website = "https://github.com/WendySBlanc/extensions",
                signingKeyFingerprint = NOVA_SIGNING_KEY_FINGERPRINT,
            )
        } else {
            ExtensionRepo(
                baseUrl = backupRepo.baseUrl,
                name = backupRepo.name,
                shortName = backupRepo.shortName,
                website = backupRepo.website,
                signingKeyFingerprint = backupRepo.signingKeyFingerprint,
            )
        }
        val dbRepos = getExtensionRepos.getAll()
        val existingReposBySHA = dbRepos.associateBy { it.signingKeyFingerprint }
        val existingReposByUrl = dbRepos.associateBy { it.baseUrl }

        val urlExists = existingReposByUrl[restoredRepo.baseUrl]
        val shaExists = existingReposBySHA[restoredRepo.signingKeyFingerprint]

        if (urlExists != null) {
            if (urlExists.signingKeyFingerprint != restoredRepo.signingKeyFingerprint) {
                error("Already Exists with different signing key fingerprint")
            }
            return
        }

        if (shaExists != null) {
            return
        }

        handler.await {
            extension_reposQueries.insert(
                restoredRepo.baseUrl,
                restoredRepo.name,
                restoredRepo.shortName,
                restoredRepo.website,
                restoredRepo.signingKeyFingerprint,
            )
        }
    }
}
