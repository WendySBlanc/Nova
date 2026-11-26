package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.chapter.interactor.getChapterListWithRecovery
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.LibraryUpdateStatus
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.track.TrackStatus
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import exh.log.xLogE
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import exh.source.LIBRARY_UPDATE_EXCLUDED_SOURCES
import exh.source.MERGED_SOURCE_ID
import exh.source.mangaDexSourceIds
import exh.util.WorkerUtil
import exh.util.nullIfBlank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.NOVA_AUTO_UPDATE_30_MINUTES
import tachiyomi.domain.libraryUpdateError.interactor.DeleteLibraryUpdateErrors
import tachiyomi.domain.libraryUpdateError.interactor.InsertLibraryUpdateErrors
import tachiyomi.domain.libraryUpdateError.model.LibraryUpdateError
import tachiyomi.domain.libraryUpdateErrorMessage.interactor.InsertLibraryUpdateErrorMessages
import tachiyomi.domain.libraryUpdateErrorMessage.model.LibraryUpdateErrorMessage
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedMangaForDownloading
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val sourceManager: SourceManager = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val downloadManager: DownloadManager = Injekt.get()
    private val coverCache: CoverCache = Injekt.get()
    private val getLibraryManga: GetLibraryManga = Injekt.get()
    private val getManga: GetManga = Injekt.get()
    private val updateManga: UpdateManga = Injekt.get()
    private val syncChaptersWithSource: SyncChaptersWithSource = Injekt.get()
    private val fetchInterval: FetchInterval = Injekt.get()
    private val filterChaptersForDownload: FilterChaptersForDownload = Injekt.get()

    // SY -->
    private val getFavorites: GetFavorites = Injekt.get()
    private val insertFlatMetadata: InsertFlatMetadata = Injekt.get()
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get()
    private val getMergedMangaForDownloading: GetMergedMangaForDownloading = Injekt.get()
    private val getTracks: GetTracks = Injekt.get()
    private val insertTrack: InsertTrack = Injekt.get()
    private val trackerManager: TrackerManager = Injekt.get()
    private val mdList = trackerManager.mdList
    // SY <--

    private val notifier = LibraryUpdateNotifier(context)

    // KMK -->
    private val libraryUpdateStatus: LibraryUpdateStatus = Injekt.get()
    private val deleteLibraryUpdateErrors: DeleteLibraryUpdateErrors = Injekt.get()
    private val insertLibraryUpdateErrors: InsertLibraryUpdateErrors = Injekt.get()
    private val insertLibraryUpdateErrorMessages: InsertLibraryUpdateErrorMessages = Injekt.get()
    // KMK <--

    private var mangaToUpdate: List<LibraryManga> = mutableListOf()

    override suspend fun doWork(): Result {
        if (tags.contains(WORK_NAME_AUTO)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val preferences = Injekt.get<LibraryPreferences>()
                val restrictions = preferences.autoUpdateDeviceRestrictions().get()
                if ((DEVICE_ONLY_ON_WIFI in restrictions) && !context.isConnectedToWifi()) {
                    return Result.retry()
                }
            }

            // Find a running manual worker. If exists, try again later
            val manualWorkStates = context.workManager
                .getWorkInfosForUniqueWork(WORK_NAME_MANUAL)
                .get()
                .map { it.state }
            if (hasActiveManualLibraryWork(manualWorkStates)) {
                return Result.retry()
            }
        }

        return runLibraryUpdateLifecycle(
            onStart = {
                libraryUpdateStatus.updateProgress(0f)
                libraryUpdateStatus.start()
            },
            onFinish = {
                notifier.cancelProgressNotification()
                libraryUpdateStatus.stop()
            },
        ) {
            withIOContext {
                try {
                    deleteLibraryUpdateErrors.cleanUnrelevantMangaErrors()
                    setForegroundSafely()

                    val target = inputData.getString(KEY_TARGET)?.let { Target.valueOf(it) } ?: Target.CHAPTERS

                    if (target == Target.CHAPTERS) {
                        libraryPreferences.lastUpdatedTimestamp().set(Instant.now().toEpochMilli())
                    }

                    val categoryId = inputData.getLong(KEY_CATEGORY, -1L)
                    val group = inputData.getInt(KEY_GROUP, LibraryGroup.BY_DEFAULT)
                    val groupExtra = inputData.getString(KEY_GROUP_EXTRA)
                    val forceUpdate = inputData.getBoolean(KEY_FORCE_UPDATE, false)
                    addMangaToQueue(categoryId, group, groupExtra, forceUpdate)

                    when (target) {
                        Target.CHAPTERS -> updateChapterList(forceUpdate)
                        // SY -->
                        Target.SYNC_FOLLOWS -> syncFollows()
                        Target.PUSH_FAVORITES -> pushFavorites()
                        // SY <--
                    }
                    Result.success()
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        // Assume success although cancelled
                        Result.success()
                    } else {
                        logcat(LogPriority.ERROR, e)
                        Result.failure()
                    }
                }
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notifier = LibraryUpdateNotifier(context)
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    /**
     * Adds list of manga to be updated.
     *
     * @param categoryId the ID of the category to update, or -1 if no category specified.
     */
    private suspend fun addMangaToQueue(categoryId: Long, group: Int, groupExtra: String?, forceUpdate: Boolean) {
        val libraryManga = getLibraryManga.await()
        // SY -->
        val groupLibraryUpdateType = libraryPreferences.groupLibraryUpdateType().get()
        // SY <--

        // KMK -->
        // Check if specific manga IDs are provided for targeted update
        val targetMangaIds = inputData.getLongArray(KEY_MANGA_IDS)?.toSet()
        if (targetMangaIds != null) {
            // Filter to only the specified manga IDs
            mangaToUpdate = libraryManga
                .filter {
                    it.manga.id in targetMangaIds &&
                        when {
                            // Apply update restrictions even for targeted updates
                            shouldSkipLibraryUpdate(
                                updateStrategy = it.manga.updateStrategy,
                                totalChapters = it.totalChapters,
                                forceUpdate = forceUpdate,
                            ) -> false
                            // Skip other restrictions for targeted updates to allow forced refresh
                            else -> true
                        }
                }

            notifier.showQueueSizeWarningNotificationIfNeeded(mangaToUpdate)
            return
        }
        // KMK <--

        val listToUpdate = if (forceUpdate && categoryId == -1L && group == LibraryGroup.BY_DEFAULT) {
            libraryManga
        } else if (categoryId != -1L) {
            libraryManga.filter { categoryId in it.categories }
            // SY -->
        } else if (
            group == LibraryGroup.BY_DEFAULT ||
            groupLibraryUpdateType == GroupLibraryMode.GLOBAL ||
            (groupLibraryUpdateType == GroupLibraryMode.ALL_BUT_UNGROUPED && group == LibraryGroup.UNGROUPED)
        ) {
            // SY <--
            val includedCategories = libraryPreferences.updateCategories().get().map { it.toLong() }.toSet()
            val excludedCategories = libraryPreferences.updateCategoriesExclude().get().map { it.toLong() }.toSet()

            libraryManga.filter {
                val included = includedCategories.isEmpty() || it.categories.intersect(includedCategories).isNotEmpty()
                val excluded = it.categories.intersect(excludedCategories).isNotEmpty()
                included && !excluded
            }
            // SY -->
        } else {
            when (group) {
                LibraryGroup.BY_TRACK_STATUS -> {
                    val trackingExtra = groupExtra?.toIntOrNull() ?: -1
                    val tracks = getTracks.await().groupBy { it.mangaId }

                    libraryManga.filter { (manga) ->
                        val status = tracks[manga.id]?.firstNotNullOfOrNull { track ->
                            TrackStatus.parseTrackerStatus(trackerManager, track.trackerId, track.status)
                        } ?: TrackStatus.OTHER
                        status.int == trackingExtra
                    }
                }

                LibraryGroup.BY_SOURCE -> {
                    val sourceExtra = groupExtra?.nullIfBlank()?.toIntOrNull()
                    val source = libraryManga.map { it.manga.source }
                        .distinct()
                        .sorted()
                        .getOrNull(sourceExtra ?: -1)

                    if (source != null) libraryManga.filter { it.manga.source == source } else emptyList()
                }

                LibraryGroup.BY_STATUS -> {
                    val statusExtra = groupExtra?.toLongOrNull() ?: -1
                    libraryManga.filter {
                        it.manga.status == statusExtra
                    }
                }

                LibraryGroup.UNGROUPED -> libraryManga
                else -> libraryManga
            }
            // SY <--
        }

        val restrictions = if (forceUpdate) {
            emptySet()
        } else {
            libraryPreferences.autoUpdateMangaRestrictions().get()
        }
        val skippedUpdates = mutableListOf<Pair<Manga, String?>>()
        val (_, fetchWindowUpperBound) = fetchInterval.getWindow(ZonedDateTime.now())

        mangaToUpdate = listToUpdate
            // SY -->
            .distinctBy { it.manga.id }
            // SY <--
            .filter {
                when {
                    shouldSkipLibraryUpdate(
                        updateStrategy = it.manga.updateStrategy,
                        totalChapters = it.totalChapters,
                        forceUpdate = forceUpdate,
                    ) -> {
                        skippedUpdates.add(
                            it.manga to
                                context.stringResource(MR.strings.skipped_reason_not_always_update),
                        )
                        false
                    }

                    MANGA_NON_COMPLETED in restrictions && it.manga.status.toInt() == SManga.COMPLETED -> {
                        skippedUpdates.add(it.manga to context.stringResource(MR.strings.skipped_reason_completed))
                        false
                    }

                    MANGA_HAS_UNREAD in restrictions && it.unreadCount != 0L -> {
                        skippedUpdates.add(it.manga to context.stringResource(MR.strings.skipped_reason_not_caught_up))
                        false
                    }

                    MANGA_NON_READ in restrictions && it.totalChapters > 0L && !it.hasStarted -> {
                        skippedUpdates.add(it.manga to context.stringResource(MR.strings.skipped_reason_not_started))
                        false
                    }

                    MANGA_OUTSIDE_RELEASE_PERIOD in restrictions && it.manga.nextUpdate > fetchWindowUpperBound -> {
                        skippedUpdates.add(
                            it.manga to
                                context.stringResource(MR.strings.skipped_reason_not_in_release_period),
                        )
                        false
                    }

                    else -> true
                }
            }
            .sortedWith(
                compareByDescending<LibraryManga> {
                    // Prefer manga updating today
                    it.manga.nextUpdate <= fetchWindowUpperBound
                }.thenByDescending {
                    // Prefer manga the user has started
                    it.lastRead > 0L
                }.thenByDescending {
                    // Prefer manga that the user has most recently read
                    it.lastRead
                }.thenByDescending {
                    // Default sorting
                    it.manga.title
                },
            )

        notifier.showQueueSizeWarningNotificationIfNeeded(mangaToUpdate)

        if (skippedUpdates.isNotEmpty()) {
            // TODO: surface skipped reasons to user?
            logcat {
                skippedUpdates
                    .groupBy { it.second }
                    .map { (reason, entries) ->
                        "$reason: [${entries.map { it.first.title }.sorted().joinToString()}]"
                    }
                    .joinToString()
            }
        }
    }

    /**
     * Method that updates manga in [mangaToUpdate]. It's called in a background thread, so it's safe
     * to do heavy operations or network calls here.
     * For each manga it calls [updateManga] and updates the notification showing the current
     * progress.
     *
     * @return an observable delivering the progress of each update.
     */
    private suspend fun updateChapterList(forceUpdate: Boolean) {
        val semaphore = Semaphore(5)
        val sourceGroups = mangaToUpdate
            .groupBy { it.manga.source }
            .filterNot { it.key in LIBRARY_UPDATE_EXCLUDED_SOURCES }
            .values
        val progress = LibraryRefreshProgress<Manga>(
            total = sourceGroups.sumOf { it.size },
            publish = notifier::showProgressNotification,
        )
        val newUpdates = CopyOnWriteArrayList<Pair<Manga, Array<Chapter>>>()
        val failedUpdates = CopyOnWriteArrayList<Pair<Manga, String?>>()
        val hasDownloads = AtomicBoolean(false)
        // SY -->
        val mdlistLogged = mdList.isLoggedIn
        // SY <--

        val fetchWindow = fetchInterval.getWindow(ZonedDateTime.now())

        coroutineScope {
            sourceGroups
                .map { mangaInSource ->
                    async {
                        semaphore.withPermit {
                            if (
                                mdlistLogged &&
                                mangaInSource.firstOrNull()
                                    ?.let { it.manga.source in mangaDexSourceIds } == true
                            ) {
                                launch {
                                    withTimeoutOrNull(TRACKING_TIMEOUT_MS) {
                                        mangaInSource.forEach { (manga) ->
                                            try {
                                                val tracks = getTracks.await(manga.id)
                                                if (tracks.isEmpty() ||
                                                    tracks.none { it.trackerId == TrackerManager.MDLIST }
                                                ) {
                                                    val track = mdList.createInitialTracker(manga)
                                                    val refreshed = withLibrarySourceDeadline(TRACKING_REQUEST_TIMEOUT_MS) {
                                                        mdList.refresh(track)
                                                    }
                                                    insertTrack.await(refreshed.toDomainTrack(false)!!)
                                                }
                                            } catch (e: Exception) {
                                                if (e is CancellationException) throw e
                                                xLogE("Error adding initial track for ${manga.title}", e)
                                            }
                                        }
                                    }
                                }
                            }
                            mangaInSource.forEach { libraryManga ->
                                val manga = libraryManga.manga
                                ensureActive()

                                progress.track(manga) {
                                    // Removed titles still count as processed.
                                    if (getManga.await(manga.id)?.favorite != true) return@track
                                    try {
                                        var partialFailure: Exception? = null
                                        val newChapters = updateManga(manga, fetchWindow, forceUpdate) { partialFailure = it }
                                            .sortedByDescending { it.sourceOrder }

                                        if (newChapters.isNotEmpty()) {
                                            val chaptersToDownload = filterChaptersForDownload.await(manga, newChapters)

                                            if (chaptersToDownload.isNotEmpty()) {
                                                downloadChapters(manga, chaptersToDownload)
                                                hasDownloads.store(true)
                                            }

                                            libraryPreferences.newUpdatesCount().getAndSet { it + newChapters.size }

                                            // Convert to the manga that contains new chapters
                                            newUpdates.add(manga to newChapters.toTypedArray())
                                        }
                                        partialFailure?.let { throw it }
                                        clearErrorFromDB(mangaId = manga.id)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Throwable) {
                                        val errorMessage = when (e) {
                                            is LibrarySourceTimeoutException ->
                                                context.stringResource(MR.strings.nova_library_update_source_timeout)

                                            is NoChaptersException ->
                                                context.stringResource(MR.strings.no_chapters_error)
                                            // failedUpdates will already have the source,
                                            // don't need to copy it into the message
                                            is SourceNotInstalledException -> context.stringResource(
                                                MR.strings.loader_not_implemented_error,
                                            )

                                            else -> e.message
                                        }
                                        writeErrorToDB(manga to errorMessage)
                                        failedUpdates.add(manga to errorMessage)
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()

        if (newUpdates.isNotEmpty()) {
            notifier.showUpdateNotifications(newUpdates)
            if (hasDownloads.load()) {
                downloadManager.startDownloads()
            }
        }

        if (failedUpdates.isNotEmpty()) {
            notifier.showUpdateErrorNotification(
                failedUpdates.size,
            )
        }
    }

    private fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        // SY -->
        if (manga.source == MERGED_SOURCE_ID) {
            val downloadingManga = runBlocking { getMergedMangaForDownloading.await(manga.id) }
                .associateBy { it.id }
            chapters.groupBy { it.mangaId }
                .forEach {
                    downloadManager.downloadChapters(
                        downloadingManga[it.key] ?: return@forEach,
                        it.value,
                        false,
                    )
                }

            return
        }
        // SY <--
        downloadManager.downloadChapters(manga, chapters, false)
    }

    /**
     * Updates the chapters for the given manga and adds them to the database.
     *
     * @param manga the manga to update.
     * @return a pair of the inserted and removed chapters.
     */
    private suspend fun updateManga(
        manga: Manga,
        fetchWindow: Pair<Long, Long>,
        forceUpdate: Boolean,
        onPartialFailure: (Exception) -> Unit,
    ): List<Chapter> {
        val source = sourceManager.getOrStub(manga.source)
        var currentManga = manga

        // Update manga metadata if needed
        if (shouldRefreshLibraryMetadata(libraryPreferences.autoUpdateMetadata().get(), forceUpdate)) {
            try {
                val networkManga = withLibrarySourceDeadline(METADATA_TIMEOUT_MS) {
                    source.getMangaDetails(currentManga.toSManga())
                }
                updateManga.awaitUpdateFromSource(currentManga, networkManga, manualFetch = forceUpdate, coverCache)
                currentManga = getManga.await(manga.id) ?: currentManga
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Chapter recovery below can repair entries whose source URL changed.
            }
        }

        if (source is MergedSource) {
            return source.fetchChaptersAndSync(
                manga = currentManga,
                downloadChapters = false,
                timeoutMillis = CHAPTERS_TIMEOUT_MS,
                onFailure = onPartialFailure,
            )
        }

        val result = withLibrarySourceDeadline(CHAPTERS_TIMEOUT_MS) { source.getChapterListWithRecovery(currentManga) }
        result.details?.let {
            updateManga.awaitUpdateFromSource(currentManga, it, manualFetch = forceUpdate, coverCache)
        }
        result.resolvedUrl
            ?.takeIf { it != currentManga.url }
            ?.let { updateManga.await(MangaUpdate(id = currentManga.id, url = it)) }

        // Get manga from database to account for if it was removed during the update and
        // to get latest data so it doesn't get overwritten later on
        val dbManga = getManga.await(manga.id)?.takeIf { it.favorite } ?: return emptyList()

        return syncChaptersWithSource.await(result.chapters, dbManga, source, false, fetchWindow)
    }

    // SY -->
    /**
     * filter all follows from Mangadex and only add reading or rereading manga to library
     */
    private suspend fun syncFollows() = coroutineScope {
        val preferences = Injekt.get<SourcePreferences>()
        var count = 0
        val mangaDex = MdUtil.getEnabledMangaDex(preferences, sourceManager = sourceManager)
            ?: return@coroutineScope
        val syncFollowStatusInts = preferences.mangadexSyncToLibraryIndexes().get().map { it.toInt() }

        val size: Int
        mangaDex.fetchAllFollows()
            .filter { (_, metadata) ->
                syncFollowStatusInts.contains(metadata.followStatus)
            }
            .also { size = it.size }
            .forEach { (networkManga, metadata) ->
                ensureActive()

                count++
                notifier.showProgressNotification(
                    listOf(Manga.create().copy(ogTitle = networkManga.title)),
                    count,
                    size,
                )

                var dbManga = getManga.await(networkManga.url, mangaDex.id)

                if (dbManga == null) {
                    dbManga = networkToLocalManga(
                        Manga.create().copy(
                            url = networkManga.url,
                            ogTitle = networkManga.title,
                            source = mangaDex.id,
                            favorite = true,
                            dateAdded = System.currentTimeMillis(),
                        ),
                    )
                } else if (!dbManga.favorite) {
                    updateManga.awaitUpdateFavorite(dbManga.id, true)
                }

                updateManga.awaitUpdateFromSource(dbManga, networkManga, true)
                metadata.mangaId = dbManga.id
                insertFlatMetadata.await(metadata)
            }

        notifier.cancelProgressNotification()
    }

    /**
     * Method that updates the all mangas which are not tracked as "reading" on mangadex
     */
    private suspend fun pushFavorites() = coroutineScope {
        var count = 0
        val listManga = getFavorites.await().filter { it.source in mangaDexSourceIds }

        // filter all follows from Mangadex and only add reading or rereading manga to library
        if (mdList.isLoggedIn) {
            listManga.forEach { manga ->
                ensureActive()

                count++
                notifier.showProgressNotification(listOf(manga), count, listManga.size)

                // Get this manga's trackers from the database
                val dbTracks = getTracks.await(manga.id)

                // find the mdlist entry if its unfollowed the follow it
                var tracker = dbTracks.firstOrNull { it.trackerId == TrackerManager.MDLIST }
                    ?: mdList.createInitialTracker(manga).toDomainTrack(idRequired = false)

                if (tracker?.status == FollowStatus.UNFOLLOWED.long) {
                    tracker = tracker.copy(
                        status = FollowStatus.READING.long,
                    )
                    val updatedTrack = mdList.update(tracker.toDbTrack())
                    insertTrack.await(updatedTrack.toDomainTrack(false)!!)
                }
            }
        }

        notifier.cancelProgressNotification()
    }
    // SY <--

    // KMK -->
    private suspend fun clearErrorFromDB(mangaId: Long) {
        deleteLibraryUpdateErrors.deleteMangaError(mangaIds = listOf(mangaId))
    }

    private suspend fun writeErrorToDB(error: Pair<Manga, String?>) {
        val errorMessage = error.second ?: context.stringResource(MR.strings.unknown_error)
        val errorMessageId = insertLibraryUpdateErrorMessages.insert(
            libraryUpdateErrorMessage = LibraryUpdateErrorMessage(-1L, errorMessage),
        )

        insertLibraryUpdateErrors.upsert(
            LibraryUpdateError(id = -1L, mangaId = error.first.id, messageId = errorMessageId),
        )
    }

    private suspend fun writeErrorsToDB(errors: List<Pair<Manga, String?>>) {
        val libraryErrors = errors.groupBy({ it.second }, { it.first })
        val errorMessages = insertLibraryUpdateErrorMessages.insertAll(
            libraryUpdateErrorMessages = libraryErrors.keys.map { errorMessage ->
                LibraryUpdateErrorMessage(-1L, errorMessage.orEmpty())
            },
        )
        val errorList = mutableListOf<LibraryUpdateError>()
        errorMessages.forEach {
            libraryErrors[it.second]?.forEach { manga ->
                errorList.add(LibraryUpdateError(id = -1L, mangaId = manga.id, messageId = it.first))
            }
        }
        insertLibraryUpdateErrors.insertAll(errorList)
    }
    // KMK <--

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        CHAPTERS, // Manga chapters

        // SY -->
        SYNC_FOLLOWS, // MangaDex specific, pull mangadex manga in reading, rereading

        PUSH_FAVORITES, // MangaDex specific, push mangadex manga to mangadex
        // SY <--
    }

    companion object {
        private const val TAG = "LibraryUpdate"
        private const val METADATA_TIMEOUT_MS = 12_000L
        private const val CHAPTERS_TIMEOUT_MS = 45_000L
        private const val TRACKING_TIMEOUT_MS = 30_000L
        private const val TRACKING_REQUEST_TIMEOUT_MS = 8_000L
        private const val WORK_NAME_AUTO = "LibraryUpdate-auto"
        private const val WORK_NAME_MANUAL = "LibraryUpdate-manual"

        private const val ERROR_LOG_HELP_URL = "https://komikku-app.github.io/docs/guides/troubleshooting/"

        /**
         * Key for category to update.
         */
        private const val KEY_CATEGORY = "category"

        /**
         * Key that defines what should be updated.
         */
        private const val KEY_TARGET = "target"

        // SY -->
        /**
         * Key for group to update.
         */
        const val KEY_GROUP = "group"
        const val KEY_GROUP_EXTRA = "group_extra"
        // SY <--

        // KMK -->
        /**
         * Key for specific manga IDs to update.
         */
        private const val KEY_MANGA_IDS = "manga_ids"
        private const val KEY_FORCE_UPDATE = "force_update"
        // KMK <--

        fun cancelAllWorks(context: Context) {
            context.workManager.cancelAllWorkByTag(TAG)
        }

        fun setupTask(
            context: Context,
            prefInterval: Int? = null,
        ) {
            val preferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: preferences.autoUpdateInterval().get()
            if (interval > 0) {
                val intervalDuration = if (interval == NOVA_AUTO_UPDATE_30_MINUTES) {
                    NOVA_AUTO_UPDATE_30_MINUTES.toLong() to TimeUnit.MINUTES
                } else {
                    interval.toLong() to TimeUnit.HOURS
                }
                val restrictions = preferences.autoUpdateDeviceRestrictions().get()
                val networkType = if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }
                val networkRequestBuilder = NetworkRequest.Builder()
                if (DEVICE_ONLY_ON_WIFI in restrictions) {
                    networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                }
                if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                    networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                }
                val constraints = Constraints.Builder()
                    // 'networkRequest' only applies to Android 9+, otherwise 'networkType' is used
                    .setRequiredNetworkRequest(networkRequestBuilder.build(), networkType)
                    .setRequiresCharging(DEVICE_CHARGING in restrictions)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val request = PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                    intervalDuration.first,
                    intervalDuration.second,
                    10,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG)
                    .addTag(WORK_NAME_AUTO)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(
                    WORK_NAME_AUTO,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            } else {
                context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
            }
        }

        fun startNow(
            context: Context,
            category: Category? = null,
            target: Target = Target.CHAPTERS,
            // SY -->
            group: Int = LibraryGroup.BY_DEFAULT,
            groupExtra: String? = null,
            // SY <--
            // KMK -->
            mangaIds: List<Long>? = null,
            forceUpdate: Boolean = false,
            // KMK <--
        ): Boolean {
            val wm = context.workManager
            val manualWorkStates = wm.getWorkInfosForUniqueWork(WORK_NAME_MANUAL).get().map { it.state }
            val automaticWorkStates = wm.getWorkInfosByTag(WORK_NAME_AUTO).get().map { it.state }
            if (
                hasActiveManualLibraryWork(manualWorkStates) ||
                hasRunningAutomaticLibraryWork(automaticWorkStates)
            ) {
                return false
            }

            val inputData = workDataOf(
                KEY_CATEGORY to category?.id,
                KEY_TARGET to target.name,
                // SY -->
                KEY_GROUP to group,
                KEY_GROUP_EXTRA to groupExtra,
                // SY <--
                // KMK -->
                KEY_MANGA_IDS to mangaIds?.toLongArray(),
                KEY_FORCE_UPDATE to forceUpdate,
                // KMK <--
            )

            val syncPreferences: SyncPreferences = Injekt.get()
            val syncEnabled = syncPreferences.isSyncEnabled()

            // Always sync the data before library update if syncing is enabled.
            if (syncEnabled && SyncDataJob.isRunning(context)) {
                return false
            }

            val libraryUpdateStatus: LibraryUpdateStatus = Injekt.get()
            libraryUpdateStatus.start()
            try {
                if (syncEnabled) {
                    val syncDataJob = OneTimeWorkRequestBuilder<SyncDataJob>()
                        .addTag(SyncDataJob.TAG_MANUAL)
                        .build()

                    val libraryUpdateJob = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                        .addTag(TAG)
                        .addTag(WORK_NAME_MANUAL)
                        .setInputData(inputData)
                        .build()

                    wm.beginUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, syncDataJob)
                        .then(libraryUpdateJob)
                        .enqueue()
                } else {
                    val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                        .addTag(TAG)
                        .addTag(WORK_NAME_MANUAL)
                        .setInputData(inputData)
                        .build()

                    wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)
                }

                return true
            } catch (error: Throwable) {
                libraryUpdateStatus.stop()
                throw error
            }
        }

        fun stop(context: Context) {
            val wm = context.workManager
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)
                    // KMK -->
                    val libraryUpdateStatus: LibraryUpdateStatus = Injekt.get()
                    runBlocking { libraryUpdateStatus.stop() }
                    // KMK <--

                    // Re-enqueue cancelled scheduled work
                    if (it.tags.contains(WORK_NAME_AUTO)) {
                        setupTask(context)
                    }
                }
        }

        // KMK -->
        /**
         * Returns true if a periodic job is currently scheduled.
         * @param context The application context.
         * @return True if a periodic job is scheduled, false otherwise.
         * @throws Exception If there is an error retrieving the work info.
         */
        suspend fun isPeriodicUpdateScheduled(context: Context): Boolean {
            return WorkerUtil.isPeriodicJobScheduled(context, WORK_NAME_AUTO)
        }
        // KMK <--
    }
}

internal fun shouldSkipLibraryUpdate(
    updateStrategy: UpdateStrategy,
    totalChapters: Long,
    forceUpdate: Boolean,
): Boolean {
    return !forceUpdate && updateStrategy == UpdateStrategy.ONLY_FETCH_ONCE && totalChapters > 0L
}

internal fun shouldRefreshLibraryMetadata(
    autoUpdateMetadata: Boolean,
    forceUpdate: Boolean,
): Boolean = forceUpdate || autoUpdateMetadata

internal fun isActiveLibraryWorkState(state: WorkInfo.State): Boolean =
    state == WorkInfo.State.ENQUEUED ||
        state == WorkInfo.State.RUNNING ||
        state == WorkInfo.State.BLOCKED

internal fun hasActiveManualLibraryWork(states: Iterable<WorkInfo.State>): Boolean =
    states.any(::isActiveLibraryWorkState)

internal fun hasRunningAutomaticLibraryWork(states: Iterable<WorkInfo.State>): Boolean =
    states.any { it == WorkInfo.State.RUNNING }

internal suspend fun <T> runLibraryUpdateLifecycle(
    onStart: suspend () -> Unit,
    onFinish: suspend () -> Unit,
    block: suspend () -> T,
): T {
    onStart()
    return try {
        block()
    } finally {
        onFinish()
    }
}
