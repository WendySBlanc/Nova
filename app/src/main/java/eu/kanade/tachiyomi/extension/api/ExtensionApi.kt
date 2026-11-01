@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package eu.kanade.tachiyomi.extension.api

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.source.BlacklistedSources
import exh.source.ExhPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import logcat.LogPriority
import mihon.domain.extensionrepo.interactor.CreateExtensionRepo
import mihon.domain.extensionrepo.interactor.GetExtensionRepo
import mihon.domain.extensionrepo.interactor.UpdateExtensionRepo
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.model.LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT
import mihon.domain.extensionrepo.model.NOVA_EXTENSION_REPO_BASE_URL
import mihon.domain.extensionrepo.model.NOVA_EXTENSION_REPO_INDEX_URL
import mihon.domain.extensionrepo.model.NOVA_SIGNING_KEY_FINGERPRINT
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.BufferedSource
import okio.buffer
import okio.gzip
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import kotlin.time.Duration.Companion.days

internal class ExtensionApi {

    private val networkService: NetworkHelper by injectLazy()
    private val preferenceStore: PreferenceStore by injectLazy()
    private val createExtensionRepo: CreateExtensionRepo by injectLazy()
    private val getExtensionRepo: GetExtensionRepo by injectLazy()
    private val updateExtensionRepo: UpdateExtensionRepo by injectLazy()
    private val extensionManager: ExtensionManager by injectLazy()

    // SY -->
    private val sourcePreferences: SourcePreferences by injectLazy()

    // SY <--

    private val json: Json by injectLazy()
    private val protoBuf: ProtoBuf by injectLazy()

    private val lastExtCheck: Preference<Long> by lazy {
        preferenceStore.getLong(Preference.appStateKey("last_ext_check"), 0)
    }

    suspend fun findExtensions(): List<Extension.Available> {
        // KMK -->
        val disabledRepos = sourcePreferences.disabledRepos().get()
        // KMK <--
        return withIOContext {
            getConfiguredExtensionRepos()
                // KMK -->
                .filterNot { it.baseUrl in disabledRepos }
                // KMK <--
                .map { async { getExtensions(it) } }
                .awaitAll()
                .flatten()
        }
    }

    private suspend fun getConfiguredExtensionRepos(): List<ExtensionRepo> {
        var repos = getExtensionRepo.getAll()
        val legacyNovaRepos = repos.filter {
            it.signingKeyFingerprint == LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT ||
                it.baseUrl.contains("keiyoushi/extensions", ignoreCase = true)
        }
        legacyNovaRepos.forEach { updateExtensionRepo.await(it) }
        if (legacyNovaRepos.isNotEmpty()) {
            repos = getExtensionRepo.getAll()
        }

        val hasNovaRepo = repos.any {
            it.signingKeyFingerprint == NOVA_SIGNING_KEY_FINGERPRINT ||
                it.baseUrl.trimEnd('/') == NOVA_EXTENSION_REPO_BASE_URL
        }
        if (!hasNovaRepo) {
            createExtensionRepo.await(NOVA_EXTENSION_REPO_INDEX_URL)
            repos = getExtensionRepo.getAll()
        }
        return repos
    }

    private suspend fun getExtensions(extRepo: ExtensionRepo): List<Extension.Available> {
        val repoBaseUrl = extRepo.downloadBaseUrl()
        if (extRepo.usesModernExtensionIndex()) {
            try {
                getModernExtensions(extRepo, repoBaseUrl)?.let { return it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e) { "Failed to get modern extensions from $repoBaseUrl" }
            }
        }

        return try {
            networkService.client
                .newCall(GET("$repoBaseUrl/index.min.json"))
                .awaitSuccess()
                .use { response ->
                    with(json) {
                        response
                            .parseAs<List<ExtensionJsonObject>>()
                            .toExtensions(
                                repoBaseUrl,
                                // KMK -->
                                signature = extRepo.signingKeyFingerprint,
                                repoName = extRepo.shortName ?: extRepo.name,
                                // KMK <--
                            )
                    }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to get extensions from $repoBaseUrl" }
            emptyList()
        }
    }

    private suspend fun getModernExtensions(
        extRepo: ExtensionRepo,
        repoBaseUrl: String,
    ): List<Extension.Available>? {
        val indexUrl = "$repoBaseUrl/index.pb"
        val store = networkService.client
            .newCall(GET(indexUrl))
            .awaitSuccess()
            .use { response ->
                response.body.source().decompressIfGzipped().decodeModernStore()
            }

        val extensionList = store.extensionList ?: store.extensionListUrl?.let { listUrl ->
            val resolvedListUrl = resolveUrl(indexUrl, listUrl)
            networkService.client
                .newCall(GET(resolvedListUrl))
                .awaitSuccess()
                .use { response ->
                    response.body.source().decompressIfGzipped().decodeModernExtensionList()
                }
        } ?: return null

        val signature = store.signingKey.ifBlank { extRepo.signingKeyFingerprint }
        val repoName = store.badgeLabel.ifBlank { store.name.ifBlank { extRepo.name } }

        return extensionList.extensions.mapNotNull { extension ->
            val libVersion = extension.extensionLib.toDoubleOrNull() ?: return@mapNotNull null
            if (libVersion !in ExtensionLoader.LIB_VERSION_MIN..ExtensionLoader.LIB_VERSION_MAX) {
                return@mapNotNull null
            }
            val languages = extension.sources.map { it.language }.toSet()

            Extension.Available(
                name = extension.name,
                pkgName = extension.packageName,
                versionName = extension.versionName,
                versionCode = extension.versionCode,
                libVersion = libVersion,
                lang = languages.singleOrNull() ?: "all",
                isNsfw = extension.contentWarning >= ModernExtensionStore.ContentWarning.MIXED,
                signatureHash = signature,
                repoName = repoName,
                sources = extension.sources.map { source ->
                    Extension.Available.Source(
                        id = source.id,
                        lang = source.language,
                        name = source.name,
                        baseUrl = source.homeUrl,
                    )
                },
                apkName = resolveUrl(indexUrl, extension.resources.apkUrl),
                iconUrl = resolveUrl(indexUrl, extension.resources.iconUrl),
                repoUrl = repoBaseUrl,
            )
        }
    }

    private fun BufferedSource.decodeModernStore(): ModernExtensionStore {
        return when (peek().readByte()) {
            JSON_OBJECT_START -> json.decodeFromBufferedSource(this)
            else -> protoBuf.decodeFromByteArray(readByteArray())
        }
    }

    private fun BufferedSource.decodeModernExtensionList(): ModernExtensionStore.ExtensionList {
        return when (peek().readByte()) {
            JSON_OBJECT_START -> json.decodeFromBufferedSource(this)
            else -> protoBuf.decodeFromByteArray(readByteArray())
        }
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false,
    ): List<Extension.Installed>? {
        // Limit checks to once a day at most
        if (!fromAvailableExtensionList &&
            Instant.now().toEpochMilli() < lastExtCheck.get() + 1.days.inWholeMilliseconds
        ) {
            return null
        }

        // Update extension repo details
        updateExtensionRepo.awaitAll()

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionsFlow.value
        } else {
            findExtensions().also { lastExtCheck.set(Instant.now().toEpochMilli()) }
        }

        // SY -->
        val blacklistEnabled = sourcePreferences.enableSourceBlacklist().get()
        // SY <--

        val installedExtensions = ExtensionLoader.loadExtensions(context)
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }
            // SY -->
            .filterNot { it.isBlacklisted(blacklistEnabled) }
        // SY <--

        val extensionsWithUpdate = mutableListOf<Extension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue
            val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
            val hasUpdatedLib = availableExt.libVersion > installedExt.libVersion
            val hasUpdate = hasUpdatedVer || hasUpdatedLib
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        if (extensionsWithUpdate.isNotEmpty()) {
            ExtensionUpdateNotifier(context).promptUpdates(extensionsWithUpdate.map { it.name })
        }

        return extensionsWithUpdate
    }

    private fun List<ExtensionJsonObject>.toExtensions(
        repoUrl: String,
        // KMK -->
        signature: String,
        repoName: String,
        // KMK <--
    ): List<Extension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= ExtensionLoader.LIB_VERSION_MIN && libVersion <= ExtensionLoader.LIB_VERSION_MAX
            }
            .map {
                Extension.Available(
                    name = it.name.substringAfter("Tachiyomi: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    libVersion = it.extractLibVersion(),
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    sources = it.sources?.map(extensionSourceMapper).orEmpty(),
                    apkName = it.apk,
                    iconUrl = "$repoUrl/icon/${it.pkg}.png",
                    repoUrl = repoUrl,
                    // KMK -->
                    signatureHash = signature,
                    repoName = repoName,
                    // KMK <--
                )
            }
    }

    fun getApkUrl(extension: Extension.Available): String {
        return extension.apkName.takeIf { it.toHttpUrlOrNull() != null }
            ?: "${extension.repoUrl}/apk/${extension.apkName}"
    }

    private fun ExtensionJsonObject.extractLibVersion(): Double {
        return version.substringBeforeLast('.').toDouble()
    }

    // SY -->
    private fun Extension.isBlacklisted(
        blacklistEnabled: Boolean = sourcePreferences.enableSourceBlacklist().get(),
        // KMK -->
        isHentaiEnabled: Boolean = Injekt.get<ExhPreferences>().isHentaiEnabled().get(),
        // KMK <--
    ): Boolean {
        return pkgName in BlacklistedSources.BLACKLISTED_EXTENSIONS &&
            blacklistEnabled &&
            // KMK -->
            isHentaiEnabled
        // KMK <--
    }
    // SY <--
}

private fun ExtensionRepo.usesModernExtensionIndex(): Boolean {
    return signingKeyFingerprint == NOVA_SIGNING_KEY_FINGERPRINT ||
        signingKeyFingerprint == LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT ||
        baseUrl.trimEnd('/') == NOVA_EXTENSION_REPO_BASE_URL ||
        baseUrl.contains("keiyoushi/extensions", ignoreCase = true) ||
        website.contains("keiyoushi", ignoreCase = true)
}

private fun ExtensionRepo.downloadBaseUrl(): String {
    return if (
        signingKeyFingerprint == LEGACY_KEIYOUSHI_SIGNING_KEY_FINGERPRINT ||
        baseUrl.contains("keiyoushi/extensions", ignoreCase = true)
    ) {
        NOVA_EXTENSION_REPO_BASE_URL
    } else {
        baseUrl
    }
}

private fun resolveUrl(baseUrl: String, url: String): String {
    return url.toHttpUrlOrNull()?.toString()
        ?: baseUrl.toHttpUrl().resolve(url)?.toString()
        ?: url
}

private fun BufferedSource.decompressIfGzipped(): BufferedSource {
    val isGzip = peek().use { peeked ->
        runCatching { peeked.readShort().toInt() == 0x1f8b }.getOrDefault(false)
    }
    return if (isGzip) gzip().buffer() else this
}

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val sources: List<ExtensionSourceJsonObject>?,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)

@Serializable
private data class ModernExtensionStore(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val badgeLabel: String,
    @ProtoNumber(3) val signingKey: String,
    @ProtoNumber(4) val contact: Contact,
    @ProtoNumber(101) val extensionList: ExtensionList?,
    @ProtoNumber(102) val extensionListUrl: String?,
) {
    @Serializable
    data class Contact(
        @ProtoNumber(1) val website: String,
        @ProtoNumber(2) val discord: String?,
    )

    @Serializable
    data class ExtensionList(
        @ProtoNumber(1) val extensions: List<Extension>,
    )

    @Serializable
    data class Extension(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val packageName: String,
        @ProtoNumber(3) val resources: Resources,
        @ProtoNumber(4) val extensionLib: String,
        @ProtoNumber(5) val versionCode: Long,
        @ProtoNumber(6) val versionName: String,
        @ProtoNumber(7) val contentWarning: ContentWarning,
        @ProtoNumber(8) val sources: List<Source>,
    )

    @Serializable
    data class Resources(
        @ProtoNumber(1) val apkUrl: String,
        @ProtoNumber(2) val iconUrl: String,
    )

    @Serializable
    data class Source(
        @ProtoNumber(1) val id: Long,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val language: String,
        @ProtoNumber(4) val homeUrl: String = "",
        @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
        @ProtoNumber(7) val message: String? = null,
    )

    @Suppress("Unused")
    enum class ContentWarning {
        @ProtoNumber(0)
        @JsonNames("CONTENT_WARNING_UNSPECIFIED")
        UNSPECIFIED,

        @ProtoNumber(1)
        @JsonNames("CONTENT_WARNING_SAFE")
        SAFE,

        @ProtoNumber(2)
        @JsonNames("CONTENT_WARNING_MIXED")
        MIXED,

        @ProtoNumber(3)
        @JsonNames("CONTENT_WARNING_NSFW")
        NSFW,
    }
}

private const val JSON_OBJECT_START: Byte = 0x7B

private val extensionSourceMapper: (ExtensionSourceJsonObject) -> Extension.Available.Source = {
    Extension.Available.Source(
        id = it.id,
        lang = it.lang,
        name = it.name,
        baseUrl = it.baseUrl,
    )
}
