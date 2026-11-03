package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.library.NovaLibraryEntry
import eu.kanade.presentation.library.NovaLibraryScreen
import eu.kanade.presentation.library.NovaMediaType
import eu.kanade.presentation.library.components.NovaLibraryCategory
import eu.kanade.presentation.util.Tab
import eu.kanade.presentation.util.isLikelyLightNovel
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

data object NovaLibraryTab : Tab {
    @Suppress("unused")
    private fun readResolve(): Any = NovaLibraryTab

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 1u,
                title = stringResource(MR.strings.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalSearchScreen(""))
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
        val categoryNamesById = remember(libraryData.categories) {
            libraryData.categories.associate { it.id to it.name }
        }
        val libraryCategories = remember(libraryData.categories) {
            libraryData.categories
                .asSequence()
                .filterNot { category -> category.isSystemCategory || category.hidden }
                .sortedBy { category -> category.order }
                .map { category ->
                    NovaLibraryCategory(
                        id = category.id,
                        name = category.name,
                    )
                }
                .toList()
        }
        val libraryItems = remember(favoriteItems) {
            favoriteItems.mapIndexed { index, item ->
                item.toNovaManga(index)
            }
        }
        val lightNovelItems = remember(favoriteItems, categoryNamesById) {
            favoriteItems
                .filter { item -> item.isLightNovel(categoryNamesById) }
                .mapIndexed { index, item ->
                    item.toNovaManga(index, mediaType = NovaMediaType.LightNovel)
                }
        }
        val heroItems = remember(favoriteItems, nextChapterLabelsSnapshot) {
            favoriteItems
                .filter { item -> item.libraryManga.lastRead > 0L }
                .sortedByDescending { item -> item.libraryManga.lastRead }
                .take(5)
                .mapIndexed { index, item ->
                    item.toNovaManga(index, subtitle = nextChapterSubtitle(item))
                }
        }
        fun refreshLibrary(): Boolean {
            val started = LibraryUpdateJob.startNow(context, forceUpdate = true)
            context.toast(if (started) MR.strings.updating_library else MR.strings.update_already_running)
            return started
        }

        NovaLibraryScreen(
            libraryItems = libraryItems,
            lightNovelItems = lightNovelItems,
            heroItems = heroItems,
            categories = libraryCategories,
            isLibraryReady = libraryData.isInitialized,
            onPlay = { mangaId ->
                if (mangaId != null) {
                    navigator.push(MangaScreen(mangaId))
                } else {
                    navigator.push(GlobalSearchScreen(""))
                }
            },
            onSearch = { navigator.push(GlobalSearchScreen("")) },
            onOpenManga = { mangaId -> navigator.push(MangaScreen(mangaId)) },
            onRefresh = ::refreshLibrary,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }

    private fun LibraryItem.toNovaManga(
        index: Int,
        subtitle: String? = null,
        mediaType: NovaMediaType = NovaMediaType.Manhwa,
    ): NovaLibraryEntry {
        val manga = libraryManga.manga
        val unreadCount = libraryManga.unreadCount.coerceAtLeast(0L)
        return NovaLibraryEntry(
            id = manga.id,
            title = manga.title,
            subtitle = subtitle ?: when {
                unreadCount > 1 -> "$unreadCount chapitres à lire"
                unreadCount == 1L -> "1 chapitre à lire"
                libraryManga.hasStarted -> "À jour"
                else -> "Prêt à lire"
            },
            tag = source?.name?.take(12) ?: if (isLocal) "Local" else "NOVA",
            coverData = manga.asMangaCover(),
            colors = novaPalette(index),
            progress = libraryManga.totalChapters
                .takeIf { it > 0L }
                ?.let { total ->
                    ((total - unreadCount).toFloat() / total.toFloat()).coerceIn(0f, 1f)
                },
            remainingChapters = unreadCount,
            totalChapters = libraryManga.totalChapters,
            lastRead = libraryManga.lastRead,
            dateAdded = manga.dateAdded,
            latestUpload = libraryManga.latestUpload,
            chapterFetchedAt = libraryManga.chapterFetchedAt,
            mediaType = mediaType,
            categoryIds = libraryManga.categories
                .asSequence()
                .filterNot { it == 0L }
                .toSet(),
        )
    }

    private const val NEXT_CHAPTER_LOOKUP_LIMIT = 8

    private fun LibraryItem.isLightNovel(categoryNamesById: Map<Long, String>): Boolean {
        return libraryManga.manga.isLikelyLightNovel(
            sourceName = source?.name,
            categoryNames = libraryManga.categories.map { categoryNamesById[it] },
        )
    }

    private fun novaPalette(index: Int) = when (index % 6) {
        0 -> listOf(Color(0xFF111B33), Color(0xFF235BB9), Color(0xFF58E3D2))
        1 -> listOf(Color(0xFF1E1026), Color(0xFF8A2D60), Color(0xFFFFC857))
        2 -> listOf(Color(0xFF101816), Color(0xFF1D6D5D), Color(0xFFA9F971))
        3 -> listOf(Color(0xFF10233E), Color(0xFF2B8CFF), Color(0xFF9FE7FF))
        4 -> listOf(Color(0xFF2B1430), Color(0xFFB4295B), Color(0xFFFF6F91))
        else -> listOf(Color(0xFF171421), Color(0xFF4E3B87), Color(0xFFCAB8FF))
    }
}
