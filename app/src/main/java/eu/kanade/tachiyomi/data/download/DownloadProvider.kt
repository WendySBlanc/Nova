package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.lang.Hash.md5
import eu.kanade.tachiyomi.util.storage.DiskUtil
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name>/<manga>/<chapter>
 *
 * @param context the application context.
 */
class DownloadProvider(
    private val context: Context,
    private val storageManager: StorageManager = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    // SY -->
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    // SY <--
) {

    private val downloadsDir: UniFile?
        get() = storageManager.getDownloadsDirectory()

    /**
     * Returns the download directory for a manga. For internal use only.
     *
     * @param mangaTitle the title of the manga to query.
     * @param source the source of the manga.
     */
    internal fun getMangaDir(mangaTitle: String, source: Source): Result<UniFile> {
        val downloadsDir = downloadsDir
        if (downloadsDir == null) {
            logcat(LogPriority.ERROR) { "Failed to create download directory" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_download_directory)),
            )
        }

        val sourceDirName = getSourceDirName(source)
        val sourceDir = downloadsDir.createDirectory(sourceDirName)
        if (sourceDir == null) {
            val displayablePath = downloadsDir.displayablePath + "/$sourceDirName"
            logcat(LogPriority.ERROR) { "Failed to create source download directory: $displayablePath" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_directory, displayablePath)),
            )
        }

        val mangaDirName = getMangaDirName(mangaTitle)
        val mangaDir = sourceDir.createDirectory(mangaDirName)
        if (mangaDir == null) {
            val displayablePath = sourceDir.displayablePath + "/$mangaDirName"
            logcat(LogPriority.ERROR) { "Failed to create manga download directory: $displayablePath" }
            return Result.failure(
                IOException(context.stringResource(MR.strings.storage_failed_to_create_directory, displayablePath)),
            )
        }

        return Result.success(mangaDir)
    }

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(source: Source): UniFile? {
        // KMK -->
        return if (source.isLocal()) {
            storageManager.getLocalSourceDirectory()
        } else {
            // KMK <--
            downloadsDir?.findFile(getSourceDirName(source))
        }
    }

    /**
     * Returns the download directory for a manga if it exists.
     *
     * @param mangaTitle the title of the manga to query.
     * @param source the source of the manga.
     */
    fun findMangaDir(mangaTitle: String, source: Source): UniFile? {
        val sourceDir = findSourceDir(source)
        return sourceDir?.findFile(getMangaDirName(mangaTitle))
    }

    private fun findMangaDirs(mangaTitle: String, source: Source): Sequence<UniFile> {
        val mangaDirName = getMangaDirName(mangaTitle)
        val preferredMangaDir = findMangaDir(mangaTitle, source)
        val fallbackMangaDirs = downloadsDir?.listFiles().orEmpty().asSequence()
            .filter { it.isDirectory }
            .mapNotNull { sourceDir -> sourceDir.findFile(mangaDirName) }

        return (sequenceOf(preferredMangaDir).filterNotNull() + fallbackMangaDirs)
            .distinctBy { it.uri.toString() }
    }

    /**
     * Returns the download directory for a chapter if it exists.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param mangaTitle the title of the manga to query.
     * @param source the source of the chapter.
     */
    fun findChapterDir(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        mangaTitle: String,
        source: Source,
        mangaId: Long? = null,
        chapterId: Long? = null,
    ): UniFile? {
        return findMangaDirs(mangaTitle, source)
            .flatMap { mangaDir -> mangaDir.listFiles().orEmpty().asSequence() }
            .filter { it.isDirectory || it.isFile }
            .filter { isValidChapterDirName(it.name, chapterName, chapterScanlator, chapterUrl, chapterId) }
            .firstOrNull()
            ?: findRestoredLightNovelChapterDir(
                chapterName = chapterName,
                chapterUrl = chapterUrl,
                mangaTitle = mangaTitle,
                source = source,
                mangaId = mangaId,
                chapterId = chapterId,
            )
    }

    fun findLightNovelIndexFile(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        mangaTitle: String,
        source: Source,
        mangaId: Long? = null,
        chapterId: Long? = null,
    ): UniFile? {
        return findChapterDir(
            chapterName = chapterName,
            chapterScanlator = chapterScanlator,
            chapterUrl = chapterUrl,
            mangaTitle = mangaTitle,
            source = source,
            mangaId = mangaId,
            chapterId = chapterId,
        )
            ?.takeIf { it.isDirectory }
            ?.findFile(LNREADER_INDEX_FILE)
            ?.takeIf { it.isFile }
    }

    /**
     * Returns a list of downloaded directories for the chapters that exist.
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findChapterDirs(chapters: List<Chapter>, manga: Manga, source: Source): Pair<UniFile?, List<UniFile>> {
        val mangaDirs = findMangaDirs(/* SY --> */ manga.ogTitle /* SY <-- */, source).toList()
        if (mangaDirs.isEmpty()) return null to emptyList()

        val chapterDirs = chapters.mapNotNull { chapter ->
            mangaDirs.asSequence()
                .mapNotNull { mangaDir ->
                    // KMK -->
                    if (source.isLocal()) {
                        val splitUrl = chapter.url.split('/', limit = 2)
                        if (splitUrl.size < 2) {
                            null
                        } else {
                            val (mangaDirName, chapterDirName) = splitUrl
                            mangaDir.findFile(chapterDirName)
                                ?: storageManager.getLocalSourceDirectory()?.findFile(mangaDirName)?.findFile(chapterDirName)
                        }
                    } else {
                        // KMK <--
                        mangaDir.listFiles().orEmpty().asSequence()
                            .filter { it.isDirectory || it.isFile }
                            .filter {
                                isValidChapterDirName(
                                    downloadedDirName = it.name,
                                    chapterName = chapter.name,
                                    chapterScanlator = chapter.scanlator,
                                    chapterUrl = chapter.url,
                                    chapterId = chapter.id,
                                )
                            }
                            .firstOrNull()
                    }
                }
                .firstOrNull()
        }

        return mangaDirs.firstOrNull() to chapterDirs
    }

    fun findDownloadedChapterDirNames(
        mangaTitles: Collection<String>,
        source: Source,
        mangaId: Long? = null,
    ): Set<String> {
        if (mangaTitles.isEmpty()) return emptySet()

        val regularChapterDirs = mangaTitles
            .asSequence()
            .flatMap { title -> findMangaDirs(title, source) }
            .distinctBy { it.uri.toString() }
            .flatMap { mangaDir -> mangaDir.listFiles().orEmpty().asSequence() }
            .mapNotNull { it.asDownloadedChapterNameOrNull() }

        val restoredLightNovelChapterDirs = findRestoredLightNovelChapterDirNames(
            mangaTitles = mangaTitles,
            source = source,
            mangaId = mangaId,
        )

        return (regularChapterDirs + restoredLightNovelChapterDirs).toSet()
    }

    // SY -->
    /**
     * Returns a list of all files in manga directory
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findUnmatchedChapterDirs(
        chapters: List<Chapter>,
        manga: Manga,
        source: Source,
    ): List<UniFile> {
        return findMangaDirs(/* SY --> */ manga.ogTitle /* SY <-- */, source).flatMap { mangaDir ->
            mangaDir.listFiles().orEmpty().asSequence().filter {
                chapters.find { chp ->
                    isValidChapterDirName(it.name, chp.name, chp.scanlator, chp.url, chp.id)
                } == null ||
                    it.name?.endsWith(Downloader.TMP_DIR_SUFFIX) == true
            }
        }.toList()
    }
    // SY <--

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: Source): String {
        return DiskUtil.buildValidFilename(
            source.toString(),
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames().get(),
        )
    }

    /**
     * Returns the download directory name for a manga.
     *
     * @param mangaTitle the title of the manga to query.
     */
    fun getMangaDirName(mangaTitle: String): String {
        return DiskUtil.buildValidFilename(
            mangaTitle,
            disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames().get(),
        )
    }

    /**
     * Returns the chapter directory name for a chapter.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query.
     * @param chapterUrl url of the chapter to query.
     */
    fun getChapterDirName(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        disallowNonAsciiFilenames: Boolean = libraryPreferences.disallowNonAsciiFilenames().get(),
        // SY -->
        includeChapterUrlHash: Boolean = downloadPreferences.includeChapterUrlHash().get(),
        // SY <--
    ): String {
        var dirName = sanitizeChapterName(chapterName)
        if (!chapterScanlator.isNullOrBlank()) {
            dirName = chapterScanlator + "_" + dirName
        }
        // Subtract 7 bytes for hash and underscore, 4 bytes for .cbz
        dirName = DiskUtil.buildValidFilename(dirName, DiskUtil.MAX_FILE_NAME_BYTES - 11, disallowNonAsciiFilenames)
        /* SY --> */ if (includeChapterUrlHash) /* SY <-- */ dirName += "_" + md5(chapterUrl).take(6)
        return dirName
    }

    /**
     * Returns list of names that might have been previously used as
     * the directory name for a chapter.
     * Add to this list if naming pattern ever changes.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query.
     * @param chapterUrl url of the chapter to query.
     */
    private fun getLegacyChapterDirNames(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
    ): List<String> {
        val sanitizedChapterName = sanitizeChapterName(chapterName)
        val chapterNameV1 = DiskUtil.buildValidFilename(
            when {
                !chapterScanlator.isNullOrBlank() -> "${chapterScanlator}_$sanitizedChapterName"
                else -> sanitizedChapterName
            },
        )

        // Get the filename that would be generated if the user were
        // using the other value for the disallow non-ASCII or non-hash
        // filenames setting. This ensures that chapters downloaded
        // before the user changed the setting can still be found.
        val otherChapterDirName =
            getChapterDirName(
                chapterName,
                chapterScanlator,
                chapterUrl,
                !libraryPreferences.disallowNonAsciiFilenames().get(),
                // SY -->
                !downloadPreferences.includeChapterUrlHash().get(),
                // SY <--
            )

        return buildList(2) {
            // Chapter name without hash (unable to handle duplicate
            // chapter names)
            add(chapterNameV1)
            add(otherChapterDirName)
        }
    }

    /**
     * Return the new name for the chapter (in case it's empty or blank)
     *
     * @param chapterName the name of the chapter
     */
    private fun sanitizeChapterName(chapterName: String): String {
        return chapterName.ifBlank {
            "Chapter"
        }
    }

    fun isChapterDirNameChanged(oldChapter: Chapter, newChapter: Chapter): Boolean {
        return getChapterDirName(oldChapter.name, oldChapter.scanlator, oldChapter.url) !=
            getChapterDirName(newChapter.name, newChapter.scanlator, newChapter.url)
    }

    /**
     * Returns valid downloaded chapter directory names.
     *
     * @param chapterName the name of the chapter.
     * @param chapterScanlator the scanlator group for the chapter, if any.
     * @param chapterUrl the unique URL of the chapter.
     */
    fun getValidChapterDirNames(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        chapterId: Long? = null,
    ): List<String> {
        val chapterDirName = getChapterDirName(chapterName, chapterScanlator, chapterUrl)
        val legacyChapterDirNames = getLegacyChapterDirNames(chapterName, chapterScanlator, chapterUrl)
        val restoredLightNovelDirNames = getRestoredLightNovelChapterDirNames(chapterName, chapterUrl, chapterId)

        return buildList {
            // Folder of images
            add(chapterDirName)
            // Archived chapters
            add("$chapterDirName.cbz")

            // any legacy names
            legacyChapterDirNames.forEach {
                add(it)
                add("$it.cbz")
            }

            restoredLightNovelDirNames.forEach {
                add(it)
                add("$it.cbz")
            }
        }
    }

    fun isValidChapterDirName(
        downloadedDirName: String?,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        chapterId: Long? = null,
    ): Boolean {
        val actualName = downloadedDirName?.removeSuffix(".cbz") ?: return false
        if (downloadedDirName in getValidChapterDirNames(chapterName, chapterScanlator, chapterUrl, chapterId)) {
            return true
        }

        // Restored chapters can keep the same visible chapter name but receive a new source URL,
        // which changes the 6-character URL hash suffix. Treat matching base names as downloaded.
        val possibleBaseNames = buildSet {
            add(getChapterDirName(chapterName, chapterScanlator, chapterUrl, includeChapterUrlHash = false))
            add(
                getChapterDirName(
                    chapterName,
                    chapterScanlator,
                    chapterUrl,
                    disallowNonAsciiFilenames = !libraryPreferences.disallowNonAsciiFilenames().get(),
                    includeChapterUrlHash = false,
                ),
            )
            addAll(getLegacyChapterDirNames(chapterName, chapterScanlator, chapterUrl))
            addAll(getRestoredLightNovelChapterDirNames(chapterName, chapterUrl, chapterId))
        }

        return possibleBaseNames.any { baseName ->
            actualName == baseName || actualName.removePrefix("${baseName}_").let { suffix ->
                actualName.startsWith("${baseName}_") &&
                    suffix.length == 6 &&
                    suffix.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            }
        }
    }

    private fun getRestoredLightNovelChapterDirNames(
        chapterName: String,
        chapterUrl: String,
        chapterId: Long?,
    ): List<String> {
        val urlSlug = chapterUrl
            .trim('/')
            .substringAfterLast('/')
            .takeIf { it.isNotBlank() }

        return buildList {
            chapterId?.let { add(it.toString()) }
            urlSlug?.let { slug ->
                add(DiskUtil.buildValidFilename(slug))
            }
            add(DiskUtil.buildValidFilename(sanitizeChapterName(chapterName)))
        }.distinct()
    }

    private fun findRestoredLightNovelChapterDir(
        chapterName: String,
        chapterUrl: String,
        mangaTitle: String,
        source: Source,
        mangaId: Long?,
        chapterId: Long?,
    ): UniFile? {
        val root = downloadsDir?.findFile(LNREADER_NOVELS_DIR_NAME) ?: return null
        val pluginCandidates = listOf(
            getSourceDirName(source),
            source.name,
            source.id.toString(),
        ).map { DiskUtil.buildValidFilename(it).lowercase() }.toSet()
        val novelCandidates = buildSet {
            mangaId?.let { add(it.toString()) }
            add(getMangaDirName(mangaTitle))
            add(DiskUtil.buildValidFilename(mangaTitle))
        }
        val chapterCandidates = getRestoredLightNovelChapterDirNames(chapterName, chapterUrl, chapterId).toSet()

        return root.listFiles().orEmpty()
            .asSequence()
            .filter { it.isDirectory }
            .sortedByDescending { pluginDir -> pluginDir.name.orEmpty().lowercase() in pluginCandidates }
            .flatMap { pluginDir -> pluginDir.listFiles().orEmpty().asSequence() }
            .filter { novelDir ->
                novelDir.isDirectory && novelDir.name.orEmpty() in novelCandidates
            }
            .flatMap { novelDir -> novelDir.listFiles().orEmpty().asSequence() }
            .firstOrNull { chapterDir ->
                chapterDir.isDirectory &&
                    chapterDir.name.orEmpty() in chapterCandidates &&
                    chapterDir.findFile(LNREADER_INDEX_FILE)?.isFile == true
            }
    }

    private fun findRestoredLightNovelChapterDirNames(
        mangaTitles: Collection<String>,
        source: Source,
        mangaId: Long?,
    ): Sequence<String> {
        val root = downloadsDir?.findFile(LNREADER_NOVELS_DIR_NAME) ?: return emptySequence()
        val pluginCandidates = listOf(
            getSourceDirName(source),
            source.name,
            source.id.toString(),
        ).map { DiskUtil.buildValidFilename(it).lowercase() }.toSet()
        val novelCandidates = buildSet {
            mangaId?.let { add(it.toString()) }
            mangaTitles.forEach { title ->
                add(getMangaDirName(title))
                add(DiskUtil.buildValidFilename(title))
            }
        }

        return root.listFiles().orEmpty()
            .asSequence()
            .filter { it.isDirectory }
            .sortedByDescending { pluginDir -> pluginDir.name.orEmpty().lowercase() in pluginCandidates }
            .flatMap { pluginDir -> pluginDir.listFiles().orEmpty().asSequence() }
            .filter { novelDir -> novelDir.isDirectory && novelDir.name.orEmpty() in novelCandidates }
            .flatMap { novelDir -> novelDir.listFiles().orEmpty().asSequence() }
            .filter { chapterDir ->
                chapterDir.isDirectory && chapterDir.findFile(LNREADER_INDEX_FILE)?.isFile == true
            }
            .mapNotNull { it.name }
    }

    private fun UniFile.asDownloadedChapterNameOrNull(): String? {
        return when {
            name?.endsWith(Downloader.TMP_DIR_SUFFIX) == true -> null
            isDirectory -> name
            isFile && extension == "cbz" -> nameWithoutExtension
            else -> null
        }
    }

    private companion object {
        const val LNREADER_NOVELS_DIR_NAME = "Novels"
        const val LNREADER_INDEX_FILE = "index.html"
    }
}
