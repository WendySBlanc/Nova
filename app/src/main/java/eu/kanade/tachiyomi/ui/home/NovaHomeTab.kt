package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.home.deriveNovaHomeSections
import eu.kanade.presentation.library.components.NovaVikiHomeScreen
import eu.kanade.presentation.library.components.NovaVikiManga
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object NovaHomeTab : Tab {
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
        // Home owns its reset-to-top behavior; reselecting must never open a new route.
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val libraryScreenModel = rememberScreenModel { LibraryScreenModel() }
        val libraryState by libraryScreenModel.state.collectAsStateWithLifecycle()
        val libraryData = libraryState.libraryData
        val favoriteItems = if (libraryData.isInitialized) {
            libraryData.favorites
        } else {
            emptyList()
        }
        val startedFavoriteItems = remember(favoriteItems) {
            favoriteItems
                .filter { item -> item.libraryManga.hasStarted }
                .sortedByDescending { item -> item.libraryManga.lastRead }
        }
        val nextChapterLookupItems = remember(startedFavoriteItems) {
            startedFavoriteItems.take(NEXT_CHAPTER_LOOKUP_LIMIT)
        }
        val nextChapterLabels = remember { mutableStateMapOf<Long, String>() }
        val nextChapterKey = remember(nextChapterLookupItems) {
            nextChapterLookupItems.joinToString("|") { item ->
                "${item.id}:${item.libraryManga.readCount}:${item.libraryManga.unreadCount}:${item.libraryManga.lastRead}"
            }
        }

        LaunchedEffect(libraryData.isInitialized, nextChapterKey) {
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
                    val nextChapter = runCatching {
                        libraryScreenModel.getNextUnreadChapter(item)
                    }.getOrNull()
                    nextChapterLabels[item.id] = nextChapter?.name ?: "Tous les chapitres lus"
                }
            }
        }
        val nextChapterLabelsSnapshot = nextChapterLabels.toMap()

        fun nextChapterSubtitle(item: LibraryItem): String {
            return when {
                !item.libraryManga.hasStarted -> "Prêt à lire"
                item.libraryManga.unreadCount <= 0L -> "Tous les chapitres lus"
                else -> nextChapterLabelsSnapshot[item.id]?.let { chapter -> "À suivre : $chapter" }
                    ?: "Prochain chapitre"
            }
        }

        fun toNovaManga(item: LibraryItem, index: Int): NovaVikiManga {
            val manga = item.libraryManga.manga
            val unreadCount = item.libraryManga.unreadCount.coerceAtLeast(0L)
            return NovaVikiManga(
                id = manga.id,
                title = manga.title,
                subtitle = when {
                    item.libraryManga.hasStarted -> nextChapterSubtitle(item)
                    unreadCount > 1 -> "$unreadCount chapitres à lire"
                    unreadCount == 1L -> "1 chapitre à lire"
                    else -> "Prêt à lire"
                },
                tag = item.source?.name?.take(12) ?: if (item.isLocal) "Local" else "NOVA",
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
        fun refreshLibrary(): Boolean {
            val started = LibraryUpdateJob.startNow(context, forceUpdate = true)
            context.toast(if (started) MR.strings.updating_library else MR.strings.update_already_running)
            return started
        }

        NovaVikiHomeScreen(
            sections = homeSections,
            onSearch = { navigator.push(GlobalSearchScreen("")) },
            onOpenManga = { mangaId -> navigator.push(MangaScreen(mangaId)) },
            onRefresh = ::refreshLibrary,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }

    private const val NEXT_CHAPTER_LOOKUP_LIMIT = 8

    private fun novaPalette(index: Int) = when (index % 6) {
        0 -> listOf(androidx.compose.ui.graphics.Color(0xFF111B33), androidx.compose.ui.graphics.Color(0xFF235BB9), androidx.compose.ui.graphics.Color(0xFF58E3D2))
        1 -> listOf(androidx.compose.ui.graphics.Color(0xFF1E1026), androidx.compose.ui.graphics.Color(0xFF8A2D60), androidx.compose.ui.graphics.Color(0xFFFFC857))
        2 -> listOf(androidx.compose.ui.graphics.Color(0xFF101816), androidx.compose.ui.graphics.Color(0xFF1D6D5D), androidx.compose.ui.graphics.Color(0xFFA9F971))
        3 -> listOf(androidx.compose.ui.graphics.Color(0xFF10233E), androidx.compose.ui.graphics.Color(0xFF2B8CFF), androidx.compose.ui.graphics.Color(0xFF9FE7FF))
        4 -> listOf(androidx.compose.ui.graphics.Color(0xFF2B1430), androidx.compose.ui.graphics.Color(0xFFB4295B), androidx.compose.ui.graphics.Color(0xFFFF6F91))
        else -> listOf(androidx.compose.ui.graphics.Color(0xFF171421), androidx.compose.ui.graphics.Color(0xFF4E3B87), androidx.compose.ui.graphics.Color(0xFFCAB8FF))
    }
}
