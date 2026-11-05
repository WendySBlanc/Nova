package eu.kanade.tachiyomi.data.translation

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

data class InstalledTranslation(
    val metadata: TranslationResultMetadata,
    val directory: File,
    val pages: List<File>,
)

class MangaTranslationCache(
    private val rootDirectory: File,
    private val json: Json = Json,
) : MangaTranslationCacheGateway {
    init {
        rootDirectory.mkdirs()
        cleanStaleFiles()
    }

    @Synchronized
    override fun installResult(metadata: TranslationResultMetadata, archive: File): InstalledTranslation {
        if (metadata.chapterKey.isBlank() || metadata.totalPages <= 0) {
            throw MangaTranslationProtocolException("Métadonnées de traduction invalides")
        }
        if (!archive.isFile) {
            throw MangaTranslationProtocolException("Archive traduite introuvable")
        }

        val chapterDirectory = chapterDirectory(metadata.chapterKey).apply { mkdirs() }
        val temporary = File(chapterDirectory, ".install-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            extractAndValidate(metadata, archive, temporary)
            File(temporary, MANIFEST_FILE).writeText(json.encodeToString(metadata))

            val variantName = metadata.variantKey()
            val installedDirectory = File(chapterDirectory, variantName)
            if (installedDirectory.exists()) {
                temporary.deleteRecursively()
            } else {
                promoteDirectory(temporary, installedDirectory)
            }
            val installedPages = pageFiles(installedDirectory, metadata.totalPages)
            activate(chapterDirectory, variantName)
            return InstalledTranslation(metadata, installedDirectory, installedPages)
        } catch (error: MangaTranslationException) {
            temporary.deleteRecursively()
            throw error
        } catch (error: IOException) {
            temporary.deleteRecursively()
            throw MangaTranslationProtocolException("Archive traduite invalide", error)
        } catch (error: RuntimeException) {
            temporary.deleteRecursively()
            throw MangaTranslationProtocolException("Archive traduite invalide", error)
        }
    }

    override fun translatedPage(chapterKey: String, pageIndex: Int): File? {
        if (chapterKey.isBlank() || pageIndex < 0) return null
        val chapterDirectory = chapterDirectory(chapterKey)
        val variantName = runCatching {
            File(chapterDirectory, ACTIVE_FILE)
                .takeIf(File::isFile)
                ?.readText()
                ?.trim()
                ?.takeIf { VARIANT_PATTERN.matches(it) }
        }.getOrNull() ?: return null
        val variantDirectory = File(chapterDirectory, variantName)
        val prefix = "%06d.".format(pageIndex)
        return variantDirectory.listFiles()
            ?.singleOrNull { file ->
                file.isFile &&
                    file.length() > 0L &&
                    file.name.startsWith(prefix) &&
                    PAGE_PATTERN.matches(file.name)
            }
    }

    override fun activeMetadata(chapterKey: String): TranslationResultMetadata? {
        if (chapterKey.isBlank()) return null
        val chapterDirectory = chapterDirectory(chapterKey)
        return runCatching {
            val variantName = File(chapterDirectory, ACTIVE_FILE)
                .takeIf(File::isFile)
                ?.readText()
                ?.trim()
                ?.takeIf(VARIANT_PATTERN::matches)
                ?: return null
            val manifest = File(File(chapterDirectory, variantName), MANIFEST_FILE)
                .takeIf(File::isFile)
                ?: return null
            json.decodeFromString<TranslationResultMetadata>(manifest.readText())
                .takeIf { metadata ->
                    metadata.chapterKey == chapterKey && metadata.variantKey() == variantName
                }
        }.getOrNull()
    }

    private fun extractAndValidate(
        metadata: TranslationResultMetadata,
        archive: File,
        destination: File,
    ): List<File> {
        val pageIndexes = mutableListOf<Int>()
        var totalBytes = 0L
        try {
            ZipInputStream(archive.inputStream().buffered()).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (entry.isDirectory || !PAGE_PATTERN.matches(entry.name)) {
                        throw MangaTranslationProtocolException("Entrée ZIP non autorisée: ${entry.name.take(80)}")
                    }
                    val pageIndex = PAGE_PATTERN.matchEntire(entry.name)!!.groupValues[1].toInt()
                    if (pageIndex !in 0 until metadata.totalPages || pageIndex in pageIndexes) {
                        throw MangaTranslationProtocolException("Page traduite dupliquée ou inattendue")
                    }
                    pageIndexes += pageIndex
                    val outputFile = File(destination, entry.name)
                    if (outputFile.canonicalFile.parentFile != destination.canonicalFile) {
                        throw MangaTranslationProtocolException("Chemin ZIP non autorisé")
                    }
                    outputFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var pageBytes = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            pageBytes += count
                            totalBytes += count
                            if (pageBytes > MAX_PAGE_BYTES || totalBytes > MAX_ARCHIVE_BYTES) {
                                throw MangaTranslationProtocolException("Archive traduite trop volumineuse")
                            }
                            output.write(buffer, 0, count)
                        }
                        if (pageBytes == 0L) {
                            throw MangaTranslationProtocolException("Page traduite vide")
                        }
                    }
                    val header = ByteArray(IMAGE_HEADER_SIZE)
                    val headerSize = outputFile.inputStream().buffered().use { input ->
                        input.readNBytes(header, 0, IMAGE_HEADER_SIZE)
                    }
                    if (detectImageExtension(header, headerSize) == null) {
                        throw MangaTranslationProtocolException("Page traduite corrompue")
                    }
                    input.closeEntry()
                }
            }
        } catch (error: ZipException) {
            throw MangaTranslationProtocolException("Archive traduite corrompue", error)
        }
        if (pageIndexes != (0 until metadata.totalPages).toList()) {
            throw MangaTranslationProtocolException("Ordre ou nombre de pages traduites incorrect")
        }
        return pageFiles(destination, metadata.totalPages)
    }

    private fun pageFiles(directory: File, expectedCount: Int): List<File> {
        val files = directory.listFiles()
            .orEmpty()
            .filter { it.isFile && PAGE_PATTERN.matches(it.name) }
            .sortedBy { PAGE_PATTERN.matchEntire(it.name)!!.groupValues[1].toInt() }
        val pageIndexes = files.map { PAGE_PATTERN.matchEntire(it.name)!!.groupValues[1].toInt() }
        if (pageIndexes != (0 until expectedCount).toList()) {
            throw MangaTranslationProtocolException("Cache de traduction incomplet")
        }
        return files
    }

    private fun activate(chapterDirectory: File, variantName: String) {
        val active = File(chapterDirectory, ACTIVE_FILE)
        val temporary = File(chapterDirectory, ".$ACTIVE_FILE.${UUID.randomUUID()}.part")
        try {
            temporary.writeText(variantName)
            promoteFile(temporary, active)
        } finally {
            temporary.delete()
        }
    }

    private fun chapterDirectory(chapterKey: String) = File(rootDirectory, chapterKey.sha256())

    private fun cleanStaleFiles() {
        rootDirectory.walkBottomUp().forEach { file ->
            if (file != rootDirectory && (file.name.startsWith(".install-") || file.name.endsWith(".part"))) {
                file.deleteRecursively()
            }
        }
    }
}

private fun TranslationResultMetadata.variantKey(): String =
    "$chapterKey|$totalPages|$targetLanguage|$configRevision".sha256()

private fun promoteDirectory(source: File, destination: File) {
    try {
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(source.toPath(), destination.toPath())
    }
}

private const val ACTIVE_FILE = "active"
private const val MANIFEST_FILE = "manifest.json"
private const val IMAGE_HEADER_SIZE = 12
private const val MAX_PAGE_BYTES = 100L * 1024L * 1024L
private const val MAX_ARCHIVE_BYTES = 2L * 1024L * 1024L * 1024L
private val PAGE_PATTERN = Regex("^(\\d{6})\\.(?:jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE)
private val VARIANT_PATTERN = Regex("^[a-f0-9]{64}$")
