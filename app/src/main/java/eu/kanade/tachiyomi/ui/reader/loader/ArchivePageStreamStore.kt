package eu.kanade.tachiyomi.ui.reader.loader

import java.io.File
import java.io.InputStream

internal class ArchivePageStreamStore(
    private val cacheDirectory: File,
    private val openArchiveEntry: (String) -> InputStream,
) {
    private val extractionLock = Any()

    fun open(pageIndex: Int, entryName: String): InputStream {
        require(pageIndex >= 0) { "Page index cannot be negative" }
        val cachedPage = File(cacheDirectory, "%06d.page".format(pageIndex))
        synchronized(extractionLock) {
            if (!cachedPage.isFile) {
                extract(entryName, cachedPage)
            }
        }
        return cachedPage.inputStream()
    }

    private fun extract(entryName: String, destination: File) {
        cacheDirectory.mkdirs()
        val temporary = File(cacheDirectory, ".${destination.name}.part")
        try {
            openArchiveEntry(entryName).use { input ->
                temporary.outputStream().buffered().use(input::copyTo)
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }
}
