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

internal fun <T> selectUnambiguousChapterDownload(
    candidates: List<T>,
    isExact: (T) -> Boolean,
    isCompatible: (T) -> Boolean,
): T? {
    candidates.firstOrNull(isExact)?.let { return it }
    return candidates.filter(isCompatible).singleOrNull()
}

internal fun <T> selectUnambiguousChapterDownloadAcrossDirectories(
    candidateGroups: Iterable<Iterable<T>>,
    isExact: (T) -> Boolean,
    isCompatible: (T) -> Boolean,
): T? {
    return selectUnambiguousChapterDownload(
        candidates = candidateGroups.flatten(),
        isExact = isExact,
        isCompatible = isCompatible,
    )
}

internal fun <C, D, K> resolveUnambiguousChapterDownloads(
    chapters: List<C>,
    downloads: List<D>,
    chapterKey: (C) -> K,
    isExact: (C, D) -> Boolean,
    isCompatible: (C, D) -> Boolean,
): Set<K> {
    val exactChapterKeys = buildSet {
        chapters.forEach { chapter ->
            if (downloads.any { download -> isExact(chapter, download) }) {
                add(chapterKey(chapter))
            }
        }
    }
    val exactDownloadIndexes = downloads.indices.filterTo(mutableSetOf()) { index ->
        chapters.any { chapter -> isExact(chapter, downloads[index]) }
    }
    val unmatchedChapters = chapters.filter { chapterKey(it) !in exactChapterKeys }
    val compatibleIndexesByChapter = unmatchedChapters.associateWith { chapter ->
        downloads.indices.filter { index ->
            index !in exactDownloadIndexes && isCompatible(chapter, downloads[index])
        }
    }

    return buildSet {
        addAll(exactChapterKeys)
        compatibleIndexesByChapter.forEach { (chapter, indexes) ->
            val index = indexes.singleOrNull() ?: return@forEach
            val matchingChapterCount = compatibleIndexesByChapter.count { (_, candidateIndexes) ->
                index in candidateIndexes
            }
            if (matchingChapterCount == 1) {
                add(chapterKey(chapter))
            }
        }
    }
}

internal fun <C, D, K> resolveUnambiguousChapterDownloadMap(
    chapters: List<C>,
    downloads: List<D>,
    chapterKey: (C) -> K,
    isExact: (C, D) -> Boolean,
    isCompatible: (C, D) -> Boolean,
): Map<K, D> {
    val resolvedChapterKeys = resolveUnambiguousChapterDownloads(
        chapters = chapters,
        downloads = downloads,
        chapterKey = chapterKey,
        isExact = isExact,
        isCompatible = isCompatible,
    )
    return buildMap {
        chapters.forEach { chapter ->
            val key = chapterKey(chapter)
            if (key !in resolvedChapterKeys) return@forEach

            selectUnambiguousChapterDownload(
                candidates = downloads,
                isExact = { download -> isExact(chapter, download) },
                isCompatible = { download -> isCompatible(chapter, download) },
            )?.let { download -> put(key, download) }
        }
    }
}

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
        allowRestoredMatch: Boolean = false,
    ): UniFile? {
        val candidateGroups = findMangaDirs(mangaTitle, source)
            .map { mangaDir -> mangaDir.listFiles().orEmpty().filter { it.isDirectory || it.isFile } }
            .toList()
        return selectUnambiguousChapterDownloadAcrossDirectories(
            candidateGroups = candidateGroups,
            isExact = {
                isExactChapterDirName(it.name, chapterName, chapterScanlator, chapterUrl, chapterId)
            },
            isCompatible = {
                allowRestoredMatch && isCompatibleRestoredChapterDirName(
                    it.name,
                    chapterName,
                    chapterScanlator,
                    chapterUrl,
                    chapterId,
                )
            },
        )
            ?: findRestoredLightNovelChapterDir(
                chapterName = chapterName,
                chapterUrl = chapterUrl,
                mangaTitle = mangaTitle,
                source = source,
                mangaId = mangaId,
                chapterId = chapterId,
                allowRestoredMatch = allowRestoredMatch,
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
        allowRestoredMatch: Boolean = false,
    ): UniFile? {
        return findChapterDir(
            chapterName = chapterName,
            chapterScanlator = chapterScanlator,
            chapterUrl = chapterUrl,
            mangaTitle = mangaTitle,
            source = source,
            mangaId = mangaId,
            chapterId = chapterId,
            allowRestoredMatch = allowRestoredMatch,
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
    fun findChapterDirs(
        chapters: List<Chapter>,
        mangaChapters: List<Chapter>,
        manga: Manga,
        source: Source,
        allowRestoredMatch: Boolean,
    ): Pair<UniFile?, List<UniFile>> {
        val mangaDirs = findMangaDirs(/* SY --> */ manga.ogTitle /* SY <-- */, source).toList()
        if (mangaDirs.isEmpty()) return null to emptyList()

        val chapterDirs = if (source.isLocal()) {
            chapters.mapNotNull { chapter ->
                mangaDirs.asSequence()
                    .mapNotNull { mangaDir ->
                        val splitUrl = chapter.url.split('/', limit = 2)
                        if (splitUrl.size < 2) {
                            null
                        } else {
                            val (mangaDirName, chapterDirName) = splitUrl
                            mangaDir.findFile(chapterDirName)
                                ?: storageManager.getLocalSourceDirectory()?.findFile(mangaDirName)?.findFile(chapterDirName)
                        }
                    }
                    .firstOrNull()
            }
        } else {
            // KMK -->
            val candidates = mangaDirs.flatMap { mangaDir ->
                mangaDir.listFiles().orEmpty().filter { it.isDirectory || it.isFile }
            }
            val resolvedDownloads = resolveUnambiguousChapterDownloadMap(
                chapters = mangaChapters,
                downloads = candidates,
                chapterKey = Chapter::id,
                isExact = { chapter, download ->
                    isExactChapterDirName(
                        downloadedDirName = download.name,
                        chapterName = chapter.name,
                        chapterScanlator = chapter.scanlator,
                        chapterUrl = chapter.url,
                        chapterId = chapter.id,
                    )
                },
                isCompatible = { chapter, download ->
                    allowRestoredMatch && isCompatibleRestoredChapterDirName(
                        downloadedDirName = download.name,
                        chapterName = chapter.name,
                        chapterScanlator = chapter.scanlator,
                        chapterUrl = chapter.url,
                        chapterId = chapter.id,
                    )
                },
            )
            chapters.mapNotNull { chapter -> resolvedDownloads[chapter.id] }
            // KMK <--
        }

        return mangaDirs.firstOrNull() to chapterDirs
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
                    isValidChapterDirName(
                        it.name,
                        chp.name,
                        chp.scanlator,
                        chp.url,
                        chp.id,
                        allowRestoredMatch = true,
                    )
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
        allowRestoredMatch: Boolean = false,
    ): Boolean {
        return isExactChapterDirName(downloadedDirName, chapterName, chapterScanlator, chapterUrl, chapterId) ||
            allowRestoredMatch && isCompatibleRestoredChapterDirName(
                downloadedDirName,
                chapterName,
                chapterScanlator,
                chapterUrl,
                chapterId,
            )
    }

    fun findValidChapterDirName(
        downloadedDirNames: Iterable<String>,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        chapterId: Long? = null,
        allowRestoredMatch: Boolean = false,
    ): String? {
        return selectUnambiguousChapterDownload(
            candidates = downloadedDirNames.toList(),
            isExact = {
                isExactChapterDirName(it, chapterName, chapterScanlator, chapterUrl, chapterId)
            },
            isCompatible = {
                allowRestoredMatch &&
                    isCompatibleRestoredChapterDirName(it, chapterName, chapterScanlator, chapterUrl, chapterId)
            },
        )
    }

    fun resolveDownloadedChapterIds(
        chapters: List<Chapter>,
        downloadedDirNames: List<String>,
    ): Set<Long> {
        return resolveUnambiguousChapterDownloads(
            chapters = chapters,
            downloads = downloadedDirNames,
            chapterKey = Chapter::id,
            isExact = { chapter, downloadedDirName ->
                isExactChapterDirName(
                    downloadedDirName,
                    chapter.name,
                    chapter.scanlator,
                    chapter.url,
                    chapter.id,
                )
            },
            isCompatible = { chapter, downloadedDirName ->
                isCompatibleRestoredChapterDirName(
                    downloadedDirName,
                    chapter.name,
                    chapter.scanlator,
                    chapter.url,
                    chapter.id,
                )
            },
        )
    }

    fun isRestoredChapterIdentityUnique(chapter: Chapter, chapters: List<Chapter>): Boolean {
        val chapterBaseNames = getCompatibleRestoredChapterBaseNames(
            chapter.name,
            chapter.scanlator,
            chapter.url,
            chapter.id,
        )
        return chapters.count { candidate ->
            getCompatibleRestoredChapterBaseNames(
                candidate.name,
                candidate.scanlator,
                candidate.url,
                candidate.id,
            ).any { it in chapterBaseNames }
        } == 1
    }

    private fun isExactChapterDirName(
        downloadedDirName: String?,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        chapterId: Long? = null,
    ): Boolean {
        val exactNames = buildSet {
            listOf(
                libraryPreferences.disallowNonAsciiFilenames().get(),
                !libraryPreferences.disallowNonAsciiFilenames().get(),
            ).distinct().forEach { disallowNonAscii ->
                val hashedName = getChapterDirName(
                    chapterName = chapterName,
                    chapterScanlator = chapterScanlator,
                    chapterUrl = chapterUrl,
                    disallowNonAsciiFilenames = disallowNonAscii,
                    includeChapterUrlHash = true,
                )
                add(hashedName)
                add("$hashedName.cbz")
            }
            getExactRestoredLightNovelChapterDirNames(chapterUrl, chapterId).forEach {
                add(it)
                add("$it.cbz")
            }
        }
        return downloadedDirName in exactNames
    }

    private fun isCompatibleRestoredChapterDirName(
        downloadedDirName: String?,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        chapterId: Long? = null,
    ): Boolean {
        val actualName = downloadedDirName?.removeSuffix(".cbz") ?: return false

        // Restored chapters can keep the same visible chapter name but receive a new source URL,
        // which changes the 6-character URL hash suffix. Treat matching base names as downloaded.
        val possibleBaseNames = getCompatibleRestoredChapterBaseNames(
            chapterName,
            chapterScanlator,
            chapterUrl,
            chapterId,
        )

        return possibleBaseNames.any { baseName ->
            actualName == baseName || actualName.removePrefix("${baseName}_").let { suffix ->
                actualName.startsWith("${baseName}_") &&
                    suffix.length == 6 &&
                    suffix.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            }
        }
    }

    private fun getCompatibleRestoredChapterBaseNames(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        chapterId: Long?,
    ): Set<String> {
        return buildSet {
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
    }

    private fun getExactRestoredLightNovelChapterDirNames(
        chapterUrl: String,
        chapterId: Long?,
    ): List<String> {
        val urlSlug = chapterUrl
            .trim('/')
            .substringAfterLast('/')
            .takeIf { it.isNotBlank() }
        return buildList {
            chapterId?.let { add(it.toString()) }
            urlSlug?.let { add(DiskUtil.buildValidFilename(it)) }
        }.distinct()
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
        allowRestoredMatch: Boolean,
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
        val exactChapterCandidates = getExactRestoredLightNovelChapterDirNames(chapterUrl, chapterId).toSet()
        val compatibleChapterCandidates = getRestoredLightNovelChapterDirNames(
            chapterName,
            chapterUrl,
            chapterId,
        ).toSet()

        val candidates = root.listFiles().orEmpty()
            .asSequence()
            .filter { it.isDirectory }
            .sortedByDescending { pluginDir -> pluginDir.name.orEmpty().lowercase() in pluginCandidates }
            .flatMap { pluginDir -> pluginDir.listFiles().orEmpty().asSequence() }
            .filter { novelDir ->
                novelDir.isDirectory && novelDir.name.orEmpty() in novelCandidates
            }
            .flatMap { novelDir -> novelDir.listFiles().orEmpty().asSequence() }
            .filter { chapterDir ->
                chapterDir.isDirectory && chapterDir.findFile(LNREADER_INDEX_FILE)?.isFile == true
            }
            .toList()

        return selectUnambiguousChapterDownload(
            candidates = candidates,
            isExact = { it.name.orEmpty() in exactChapterCandidates },
            isCompatible = { allowRestoredMatch && it.name.orEmpty() in compatibleChapterCandidates },
        )
    }

    fun findDownloadedLightNovelChapterDirNames(
        mangaTitles: Collection<String>,
        source: Source,
        mangaId: Long?,
    ): List<String> {
        val root = downloadsDir?.findFile(LNREADER_NOVELS_DIR_NAME) ?: return emptyList()
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
            .toList()
    }

    private companion object {
        const val LNREADER_NOVELS_DIR_NAME = "Novels"
        const val LNREADER_INDEX_FILE = "index.html"
    }
}
