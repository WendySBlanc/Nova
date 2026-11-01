package eu.kanade.tachiyomi.extension

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import mihon.domain.extensionrepo.model.NOVA_EXTENSION_REPO_BASE_URL
import tachiyomi.domain.storage.service.StorageManager
import java.time.OffsetDateTime

class InstalledExtensionsInventory(
    private val context: Context,
    private val extensionManager: ExtensionManager,
    private val storageManager: StorageManager,
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun start(scope: CoroutineScope) {
        combine(
            extensionManager.installedExtensionsFlow,
            extensionManager.availableExtensionsFlow,
            storageManager.changes.onStart { emit(Unit) },
        ) { installed, available, _ ->
            createInventory(installed, available)
        }
            .onEach { inventory ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        writeToStorage(json.encodeToString(inventory))
                    }.onFailure { error ->
                        logcat(LogPriority.ERROR, error) { "Failed to update installed extension inventory" }
                    }
                }
            }
            .catch { error ->
                logcat(LogPriority.ERROR, error) { "Failed to update installed extension inventory" }
            }
            .launchIn(scope)
    }

    suspend fun share() {
        val uri = withContext(Dispatchers.IO) {
            runCatching {
                val inventory = createInventory(
                    extensionManager.installedExtensionsFlow.value,
                    extensionManager.availableExtensionsFlow.value,
                )
                val contents = json.encodeToString(inventory)
                writeToStorage(contents)

                context.createFileInCacheDir(INVENTORY_FILE_NAME)
                    .apply { writeText(contents) }
                    .getUriCompat(context)
            }.onFailure { error ->
                logcat(LogPriority.ERROR, error) { "Failed to share installed extension inventory" }
            }.getOrNull()
        }

        withContext(Dispatchers.Main) {
            if (uri == null) {
                context.toast("Impossible de creer la liste des extensions")
            } else {
                context.startActivity(uri.toShareIntent(context, INVENTORY_MIME_TYPE))
            }
        }
    }

    private fun writeToStorage(contents: String) {
        val directory = storageManager.getExtensionsDirectory() ?: return
        val file = directory.findFile(INVENTORY_FILE_NAME)
            ?: directory.createFile(INVENTORY_FILE_NAME)
            ?: return

        file.openOutputStream().bufferedWriter().use { writer ->
            writer.write(contents)
        }
    }

    private fun createInventory(
        installed: List<Extension.Installed>,
        available: List<Extension.Available>,
    ): Inventory {
        val availableByPackage = available.groupBy(Extension.Available::pkgName)
        val entries = installed
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            .map { extension ->
                val latest = availableByPackage[extension.pkgName]
                    ?.firstOrNull { it.signatureHash == extension.signatureHash }

                InventoryEntry(
                    name = extension.name,
                    packageName = extension.pkgName,
                    language = extension.lang,
                    installedVersionName = extension.versionName,
                    installedVersionCode = extension.versionCode,
                    availableVersionName = latest?.versionName,
                    availableVersionCode = latest?.versionCode,
                    extensionLibVersion = extension.libVersion,
                    hasUpdate = extension.hasUpdate || (latest?.versionCode ?: 0) > extension.versionCode,
                    isObsolete = extension.isObsolete,
                    repositoryName = extension.repoName ?: latest?.repoName,
                    signatureSha256 = extension.signatureHash,
                    sources = extension.sources.map { it.name }.distinct().sorted(),
                )
            }

        return Inventory(
            generatedAt = OffsetDateTime.now().toString(),
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            extensionRepository = NOVA_EXTENSION_REPO_BASE_URL,
            installedCount = entries.size,
            updatesAvailable = entries.count(InventoryEntry::hasUpdate),
            extensions = entries,
        )
    }

    @Serializable
    private data class Inventory(
        val formatVersion: Int = 1,
        val generatedAt: String,
        val appVersionName: String,
        val appVersionCode: Int,
        val extensionRepository: String,
        val installedCount: Int,
        val updatesAvailable: Int,
        val extensions: List<InventoryEntry>,
    )

    @Serializable
    private data class InventoryEntry(
        val name: String,
        val packageName: String,
        val language: String,
        val installedVersionName: String,
        val installedVersionCode: Long,
        val availableVersionName: String?,
        val availableVersionCode: Long?,
        val extensionLibVersion: Double,
        val hasUpdate: Boolean,
        val isObsolete: Boolean,
        val repositoryName: String?,
        val signatureSha256: String,
        val sources: List<String>,
    )

    private companion object {
        const val INVENTORY_FILE_NAME = "nova-installed-extensions.json"
        const val INVENTORY_MIME_TYPE = "application/json"
    }
}
