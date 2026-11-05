package eu.kanade.tachiyomi.data.translation

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MangaTranslationArchive(
    private val uploadDirectory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MangaTranslationArchiveGateway {
    override suspend fun createUpload(chapterKey: String, pages: List<ReaderPage>): File = withContext(ioDispatcher) {
        require(chapterKey.isNotBlank()) { "chapterKey cannot be blank" }
        require(pages.isNotEmpty()) { "pages cannot be empty" }
        val orderedPages = pages.sortedBy { it.index }
        if (orderedPages.map { it.index } != orderedPages.indices.toList()) {
            throw MangaTranslationProtocolException("La numérotation des pages est invalide")
        }

        uploadDirectory.mkdirs()
        val destination = File(uploadDirectory, "${chapterKey.sha256()}.zip")
        val temporary = File(uploadDirectory, ".${destination.name}.${UUID.randomUUID()}.part")
        try {
            ZipOutputStream(temporary.outputStream().buffered()).use { output ->
                orderedPages.forEach { page ->
                    writePage(output, page)
                }
            }
            promoteFile(temporary, destination)
            destination
        } catch (error: CancellationException) {
            temporary.delete()
            throw error
        } catch (error: MangaTranslationException) {
            temporary.delete()
            throw error
        } catch (error: Exception) {
            temporary.delete()
            throw MangaTranslationProtocolException("Impossible de créer l'archive du chapitre", error)
        }
    }

    private fun writePage(output: ZipOutputStream, page: ReaderPage) {
        val streamProvider = page.stream
            ?: throw MangaTranslationProtocolException("La page ${page.index + 1} n'est pas chargée")
        try {
            BufferedInputStream(streamProvider()).use { input ->
                input.mark(IMAGE_HEADER_SIZE)
                val header = ByteArray(IMAGE_HEADER_SIZE)
                val headerSize = input.readNBytes(header, 0, IMAGE_HEADER_SIZE)
                input.reset()
                val extension = detectImageExtension(header, headerSize)
                    ?: throw MangaTranslationProtocolException(
                        "Le format de la page ${page.index + 1} n'est pas pris en charge",
                    )
                val entry = ZipEntry("%06d.%s".format(page.index, extension)).apply { time = 0L }
                output.putNextEntry(entry)
                input.copyTo(output)
                output.closeEntry()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: MangaTranslationException) {
            throw error
        } catch (error: IOException) {
            throw MangaTranslationProtocolException("Impossible de lire la page ${page.index + 1}", error)
        } catch (error: RuntimeException) {
            throw MangaTranslationProtocolException("Impossible de lire la page ${page.index + 1}", error)
        }
    }
}

internal fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(encodeToByteArray())
    .joinToString("") { "%02x".format(it) }

internal fun promoteFile(source: File, destination: File) {
    destination.parentFile?.mkdirs()
    try {
        Files.move(
            source.toPath(),
            destination.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

internal fun detectImageExtension(header: ByteArray, size: Int): String? = when {
    size >= PNG_SIGNATURE.size && header.startsWith(PNG_SIGNATURE) -> "png"
    size >= JPEG_SIGNATURE.size && header.startsWith(JPEG_SIGNATURE) -> "jpg"
    size >= WEBP_HEADER_SIZE &&
        header.copyOfRange(0, 4).contentEquals(RIFF_SIGNATURE) &&
        header.copyOfRange(8, 12).contentEquals(WEBP_SIGNATURE) -> "webp"
    else -> null
}

private fun ByteArray.startsWith(signature: ByteArray): Boolean =
    copyOfRange(0, signature.size).contentEquals(signature)

private const val IMAGE_HEADER_SIZE = 12
private const val WEBP_HEADER_SIZE = 12
private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(),
    0x50,
    0x4E,
    0x47,
    0x0D,
    0x0A,
    0x1A,
    0x0A,
)
private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val RIFF_SIGNATURE = "RIFF".encodeToByteArray()
private val WEBP_SIGNATURE = "WEBP".encodeToByteArray()
