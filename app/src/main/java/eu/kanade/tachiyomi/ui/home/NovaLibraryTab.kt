package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.library.NOVA_ARCHIVE_CATEGORY_NAME
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
import kotlinx.coroutines.flow.MutableSharedFlow
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object NovaLibraryTab : Tab {
    private val reselectEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Suppress("unused")
    private fun readResolve(): Any = NovaLibraryTab

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 2u,
                title = stringResource(MR.strings.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
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
        var resetToTopKey by remember { mutableIntStateOf(0) }
        val libraryState by libraryScreenModel.state.collectAsStateWithLifecycle()
        val libraryData = libraryState.libraryData
        val favoriteItems = if (libraryData.isInitialized) {
            libraryData.favorites
        } else {
            emptyList()
        }
        val categoryNamesById = remember(libraryData.categories) {
            libraryData.categories.associate { it.id to it.name }
        }
        val libraryCategories = remember(libraryData.categories) {
            libraryData.categories
                .asSequence()
                .filter { category ->
                    !category.isSystemCategory &&
                        (!category.hidden || category.name.equals(NOVA_ARCHIVE_CATEGORY_NAME, ignoreCase = true))
                }
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
        LaunchedEffect(Unit) {
            reselectEvents.collect {
                resetToTopKey += 1
            }
        }
        val lightNovelItems = remember(favoriteItems, categoryNamesById) {
            favoriteItems
                .filter { item -> item.isLightNovel(categoryNamesById) }
                .mapIndexed { index, item ->
                    item.toNovaManga(index, mediaType = NovaMediaType.LightNovel)
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
            onSetArchived = libraryScreenModel::setNovaArchived,
            onRefresh = ::refreshLibrary,
            resetToTopKey = resetToTopKey,
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
