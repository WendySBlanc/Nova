package eu.kanade.tachiyomi.data.lens

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

class ReaderLensCache(
    private val rootDirectory: File,
    private val json: Json,
) {
    init {
        rootDirectory.mkdirs()
    }

    fun get(chapterKey: String, pageIndex: Int, pageIdentity: String): List<ReaderLensRegion>? {
        if (chapterKey.isBlank() || pageIndex < 0 || pageIdentity.isBlank()) return null
        val file = pageFile(chapterKey, pageIndex, pageIdentity)
        if (!file.isFile) return null
        return runCatching {
            json.decodeFromString<List<ReaderLensRegion>>(file.readText())
        }.getOrNull()
    }

    fun put(chapterKey: String, pageIndex: Int, pageIdentity: String, regions: List<ReaderLensRegion>) {
        require(chapterKey.isNotBlank()) { "chapterKey cannot be blank" }
        require(pageIndex >= 0) { "pageIndex cannot be negative" }
        require(pageIdentity.isNotBlank()) { "pageIdentity cannot be blank" }
        val destination = pageFile(chapterKey, pageIndex, pageIdentity)
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, ".${destination.name}.part")
        temporary.writeText(json.encodeToString(regions))
        destination.parentFile
            ?.listFiles { file -> file.name.startsWith("%06d-".format(pageIndex)) && file != destination }
            ?.forEach(File::delete)
        if (!temporary.renameTo(destination)) {
            temporary.copyTo(destination, overwrite = true)
            temporary.delete()
        }
    }

    private fun pageFile(chapterKey: String, pageIndex: Int, pageIdentity: String): File {
        val chapterDirectory = File(rootDirectory, "$READER_LENS_CACHE_VERSION:$READER_LENS_TARGET_LANGUAGE:$chapterKey".sha256())
        return File(chapterDirectory, "%06d-%s.json".format(pageIndex, pageIdentity.sha256().take(16)))
    }
}

private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray())
    .joinToString("") { byte -> "%02x".format(byte) }

private const val READER_LENS_CACHE_VERSION = 2
private const val READER_LENS_TARGET_LANGUAGE = "fr"
