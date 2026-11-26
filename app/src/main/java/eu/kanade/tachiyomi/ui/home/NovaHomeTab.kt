package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.home.deriveNovaHomeSections
import eu.kanade.presentation.home.selectNovaResumeChapter
import eu.kanade.presentation.library.components.NovaVikiHomeScreen
import eu.kanade.presentation.library.components.NovaVikiManga
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.LibraryUpdateStatus
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object NovaHomeTab : Tab {
    private val reselectEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Suppress("unused")
    private fun readResolve(): Any = NovaHomeTab

    override val options: TabOptions
        @Composable
        get() {
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_home_nova),
                icon = painterResource(R.drawable.ic_home_24dp),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        reselectEvents.emit(Unit)
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val libraryScreenModel = rememberScreenModel { LibraryScreenModel() }
        val getHistory = remember { Injekt.get<GetHistory>() }
        val getNextChapters = remember { Injekt.get<GetNextChapters>() }
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val autoAdvance by uiPreferences.homeAutoAdvance().collectAsState()
        val showUpdates by uiPreferences.homeShowUpdates().collectAsState()
        val showRecent by uiPreferences.homeShowRecent().collectAsState()
        val recentFirst by uiPreferences.homeRecentFirst().collectAsState()
        val updateStatus = remember { Injekt.get<LibraryUpdateStatus>() }
        val isRefreshing by updateStatus.isRunning.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        var openingChapter by remember { mutableStateOf(false) }
        var resetToTopKey by remember { mutableIntStateOf(0) }
        val libraryState by libraryScreenModel.state.collectAsStateWithLifecycle()
        val libraryData = libraryState.libraryData
        val favoriteItems = if (libraryData.isInitialized) {
            libraryData.favorites
        } else {
            emptyList()
        }
        val startedFavoriteItems = remember(favoriteItems) {
            favoriteItems
                .filter { item -> item.libraryManga.lastRead > 0L && item.libraryManga.unreadCount > 0L }
                .sortedByDescending { item -> item.libraryManga.lastRead }
        }
        val nextChapterLookupItems = remember(startedFavoriteItems) {
            startedFavoriteItems.take(NEXT_CHAPTER_LOOKUP_LIMIT)
        }
        val nextChapterLabels = remember { mutableStateMapOf<Long, String>() }
        suspend fun findNextChapter(item: LibraryItem): Chapter? = try {
            withIOContext {
                val lastRead = getHistory.await(item.id)
                    .maxByOrNull { it.readAt?.time ?: 0L }
                    ?.takeIf { (it.readAt?.time ?: 0L) > 0L }
                val fromHistory = lastRead?.let {
                    selectNovaResumeChapter(
                        getNextChapters.await(item.id, onlyUnread = false),
                        lastReadChapterId = it.chapterId,
                    )
                }
                fromHistory ?: libraryScreenModel.getNextUnreadChapter(item)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Unable to resolve the next chapter from Home" }
            null
        }

        LaunchedEffect(libraryData.isInitialized, nextChapterLookupItems) {
            if (!libraryData.isInitialized) {
                nextChapterLabels.clear()
                return@LaunchedEffect
            }

            val activeIds = nextChapterLookupItems.map { it.id }.toSet()
            nextChapterLabels.keys
                .toList()
                .filterNot(activeIds::contains)
                .forEach(nextChapterLabels::remove)

            nextChapterLookupItems.forEach { item ->
                launch {
                    val nextChapter = findNextChapter(item)
                    if (nextChapter != null) nextChapterLabels[item.id] = nextChapter.name
                    else nextChapterLabels.remove(item.id)
                }
            }
        }
        val nextChapterLabelsSnapshot = nextChapterLabels.toMap()

        LaunchedEffect(Unit) {
            reselectEvents.collect {
                resetToTopKey += 1
            }
        }

        fun toNovaManga(item: LibraryItem, index: Int): NovaVikiManga {
            val manga = item.libraryManga.manga
            val unreadCount = item.libraryManga.unreadCount.coerceAtLeast(0L)
            return NovaVikiManga(
                id = manga.id,
                title = manga.title,
                subtitle = nextChapterLabelsSnapshot[item.id].orEmpty(),
                tag = item.source?.name.orEmpty(),
                coverData = manga.asMangaCover(),
                colors = novaPalette(index),
                progress = item.libraryManga.totalChapters
                    .takeIf { it > 0L }
                    ?.let { total ->
                        ((total - unreadCount).toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    },
                remainingChapters = unreadCount,
                totalChapters = item.libraryManga.totalChapters,
                lastRead = item.libraryManga.lastRead,
                dateAdded = manga.dateAdded,
                latestUpload = item.libraryManga.latestUpload,
                chapterFetchedAt = item.libraryManga.chapterFetchedAt,
                categoryIds = item.libraryManga.categories
                    .asSequence()
                    .filterNot { categoryId -> categoryId == 0L }
                    .toSet(),
            )
        }
        val libraryItems = remember(favoriteItems, nextChapterLabelsSnapshot) {
            favoriteItems.mapIndexed { index, item ->
                toNovaManga(item, index)
            }
        }
        val homeSections = remember(libraryItems) {
            deriveNovaHomeSections(libraryItems)
        }
        val entriesById = remember(favoriteItems) { favoriteItems.associateBy { it.id } }
        fun refreshLibrary() {
            val started = LibraryUpdateJob.startNow(context, forceUpdate = true)
            context.toast(if (started) MR.strings.updating_library else MR.strings.update_already_running)
        }

        NovaVikiHomeScreen(
            sections = homeSections,
            autoAdvance = autoAdvance,
            showUpdates = showUpdates,
            showRecent = showRecent,
            recentFirst = recentFirst,
            libraryReady = libraryData.isInitialized,
            isRefreshing = isRefreshing,
            onSearch = { navigator.push(GlobalSearchScreen("")) },
            onOpenManga = { mangaId -> navigator.push(MangaScreen(mangaId)) },
            onResumeManga = { mangaId ->
                if (!openingChapter) {
                    openingChapter = true
                    scope.launch {
                        try {
                            val chapter = entriesById[mangaId]?.let { findNextChapter(it) }
                            if (chapter != null) {
                                context.startActivity(ReaderActivity.newIntent(context, mangaId, chapter.id))
                            } else {
                                navigator.push(MangaScreen(mangaId))
                            }
                        } finally {
                            openingChapter = false
                        }
                    }
                }
            },
            onRefresh = ::refreshLibrary,
            resetToTopKey = resetToTopKey,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }

    private const val NEXT_CHAPTER_LOOKUP_LIMIT = 5

    private fun novaPalette(index: Int) = when (index % 6) {
        0 -> listOf(androidx.compose.ui.graphics.Color(0xFF111B33), androidx.compose.ui.graphics.Color(0xFF235BB9), androidx.compose.ui.graphics.Color(0xFF58E3D2))
        1 -> listOf(androidx.compose.ui.graphics.Color(0xFF1E1026), androidx.compose.ui.graphics.Color(0xFF8A2D60), androidx.compose.ui.graphics.Color(0xFFFFC857))
        2 -> listOf(androidx.compose.ui.graphics.Color(0xFF101816), androidx.compose.ui.graphics.Color(0xFF1D6D5D), androidx.compose.ui.graphics.Color(0xFFA9F971))
        3 -> listOf(androidx.compose.ui.graphics.Color(0xFF10233E), androidx.compose.ui.graphics.Color(0xFF2B8CFF), androidx.compose.ui.graphics.Color(0xFF9FE7FF))
        4 -> listOf(androidx.compose.ui.graphics.Color(0xFF2B1430), androidx.compose.ui.graphics.Color(0xFFB4295B), androidx.compose.ui.graphics.Color(0xFFFF6F91))
        else -> listOf(androidx.compose.ui.graphics.Color(0xFF171421), androidx.compose.ui.graphics.Color(0xFF4E3B87), androidx.compose.ui.graphics.Color(0xFFCAB8FF))
    }
}
