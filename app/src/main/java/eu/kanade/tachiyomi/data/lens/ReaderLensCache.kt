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

    fun get(chapterKey: String, pageIndex: Int): List<ReaderLensRegion>? {
        if (chapterKey.isBlank() || pageIndex < 0) return null
        val file = pageFile(chapterKey, pageIndex)
        if (!file.isFile) return null
        return runCatching {
            json.decodeFromString<List<ReaderLensRegion>>(file.readText())
        }.getOrNull()
    }

    fun put(chapterKey: String, pageIndex: Int, regions: List<ReaderLensRegion>) {
        require(chapterKey.isNotBlank()) { "chapterKey cannot be blank" }
        require(pageIndex >= 0) { "pageIndex cannot be negative" }
        val destination = pageFile(chapterKey, pageIndex)
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, ".${destination.name}.part")
        temporary.writeText(json.encodeToString(regions))
        if (!temporary.renameTo(destination)) {
            temporary.copyTo(destination, overwrite = true)
            temporary.delete()
        }
    }

    private fun pageFile(chapterKey: String, pageIndex: Int): File {
        return File(File(rootDirectory, chapterKey.sha256()), "%06d.json".format(pageIndex))
    }
}

private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray())
    .joinToString("") { byte -> "%02x".format(byte) }
