package eu.kanade.presentation.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NovaLibraryPresentation
import eu.kanade.presentation.components.NovaCountBadge
import eu.kanade.presentation.components.NovaToolbarIconButton
import eu.kanade.presentation.home.LocalNovaDockBottomInset
import eu.kanade.presentation.library.NovaLibraryShelf
import eu.kanade.presentation.library.coverSaturation
import eu.kanade.presentation.library.filterNovaLibraryShelf
import eu.kanade.presentation.library.findNovaArchiveCategory
import eu.kanade.presentation.library.novaCatalogMetrics
import eu.kanade.presentation.library.novaCollectionCategories
import eu.kanade.presentation.library.novaLibraryShelfOptions
import eu.kanade.presentation.theme.NovaIcons
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.theme.novaGlassSurface
import eu.kanade.presentation.util.LocalNovaSharedTransitionState
import eu.kanade.presentation.util.LocalNovaTransitionScreenKey
import eu.kanade.presentation.util.novaMangaSharedArtwork
import eu.kanade.presentation.util.rememberHighQualityImageRequest
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Immutable
data class NovaLibraryCategory(
    val id: Long,
    val name: String,
)

@Composable
fun NovaVikiLibraryScreen(
    libraryItems: List<NovaVikiManga>,
    lightNovelItems: List<NovaVikiManga>,
    categories: List<NovaLibraryCategory>,
    isLibraryReady: Boolean,
    isRefreshing: Boolean,
    onPlay: (Long?) -> Unit,
    onSearch: () -> Unit,
    onCalendar: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onSetArchived: (Long, Boolean) -> Unit,
    onRefresh: () -> Boolean,
    resetToTopKey: Int,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showGalleryOptions by rememberSaveable { mutableStateOf(false) }
    val displayPreference = remember { Injekt.get<UiPreferences>().novaLibraryPresentation() }
    val display by displayPreference.collectAsState()
    var selectedShelfIndex by rememberSaveable { mutableIntStateOf(NovaLibraryShelf.Current.ordinal) }
    val selectedShelf = NovaLibraryShelf.entries[selectedShelfIndex]
    var sortOption by rememberSaveable { mutableStateOf(NovaLibrarySortOption.LastRead) }
    var sortDescending by rememberSaveable { mutableStateOf(true) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val sharedTransitionState = LocalNovaSharedTransitionState.current
    val sourceScreenKey = LocalNovaTransitionScreenKey.current
    val transitionItemsById = remember(libraryItems, lightNovelItems) {
        (libraryItems + lightNovelItems).associateBy { it.id }
    }
    val selectedTransitionMangaId = sharedTransitionState?.selectedMangaId
    val openMangaWithTransition: (Long) -> Unit = { mangaId ->
        transitionItemsById[mangaId]?.let { item ->
            sharedTransitionState?.selectManga(item.id, item.coverData, item.title, sourceScreenKey)
        }
        onOpenManga(mangaId)
    }
    val playManga: (Long?) -> Unit = onPlay
    val gridState = rememberLazyGridState()
    val tabItems = remember(selectedTab, libraryItems, lightNovelItems) {
        if (selectedTab == 0) libraryItems else lightNovelItems
    }
    val archiveCategory = remember(categories) { findNovaArchiveCategory(categories) }
    val collectionCategories = remember(categories) { novaCollectionCategories(categories) }
    val currentItems = remember(tabItems, archiveCategory) {
        filterNovaLibraryShelf(tabItems, NovaLibraryShelf.Current, archiveCategory?.id)
    }
    val archivedItems = remember(tabItems, archiveCategory) {
        filterNovaLibraryShelf(tabItems, NovaLibraryShelf.Archive, archiveCategory?.id)
    }
    val shelfItems = remember(tabItems, selectedShelf, archiveCategory) {
        filterNovaLibraryShelf(tabItems, selectedShelf, archiveCategory?.id)
    }
    val categoryCounts = remember(currentItems, collectionCategories) {
        collectionCategories.associate { category ->
            category.id to currentItems.count { item -> category.id in item.categoryIds }
        }
    }
    val visibleItems = remember(shelfItems, selectedShelf, selectedCategoryId) {
        if (selectedShelf == NovaLibraryShelf.Collections) {
            selectedCategoryId?.let { categoryId ->
                shelfItems.filter { item -> categoryId in item.categoryIds }
            } ?: shelfItems
        } else {
            shelfItems
        }
    }
    val sortedVisibleItems = remember(visibleItems, sortOption, sortDescending) {
        visibleItems.sortedForLibrary(sortOption, sortDescending)
    }
    val activeFilter = listOfNotNull(
        "Light Novel".takeIf { selectedTab == 1 },
        when (selectedShelf) {
            NovaLibraryShelf.Current -> null
            NovaLibraryShelf.Archive -> stringResource(MR.strings.nova_library_archive_section)
            NovaLibraryShelf.Collections -> collectionCategories.firstOrNull { it.id == selectedCategoryId }?.name
        },
    ).joinToString(" · ").ifBlank { null }
    val unreadChapterCount = remember(libraryItems) {
        libraryItems.sumOf { it.remainingChapters.coerceAtLeast(0L) }
    }
    val librarySummary = remember(isLibraryReady, unreadChapterCount) {
        if (!isLibraryReady) {
            "Chargement de la collection"
        } else if (unreadChapterCount > 0L) {
            val chapterLabel = if (unreadChapterCount > 1L) "chapitres à lire" else "chapitre à lire"
            "$unreadChapterCount $chapterLabel"
        } else {
            "Bibliothèque à jour"
        }
    }
    LaunchedEffect(collectionCategories, selectedShelf) {
        if (selectedShelf != NovaLibraryShelf.Collections) {
            selectedCategoryId = null
            return@LaunchedEffect
        }
        val selectedId = selectedCategoryId
        if (selectedId == null || collectionCategories.none { category -> category.id == selectedId }) {
            selectedCategoryId = null
            selectedShelfIndex = NovaLibraryShelf.Current.ordinal
        }
    }
    LaunchedEffect(selectedTab, selectedShelfIndex, selectedCategoryId, sortOption, sortDescending) {
        if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
            gridState.scrollToItem(0)
        }
    }
    LaunchedEffect(resetToTopKey) {
        if (resetToTopKey > 0) {
            gridState.scrollToItem(0)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NovaLibraryGalleryHeader(
            onSearch = onSearch,
            onCalendar = onCalendar,
            onOptions = { showGalleryOptions = true },
            activeFilter = activeFilter,
        )
        PullRefresh(
            refreshing = isRefreshing,
            enabled = isLibraryReady,
            onRefresh = { onRefresh() },
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            NovaLibraryGrid(
                items = sortedVisibleItems,
                display = display,
                isLibraryReady = isLibraryReady,
                selectedTab = selectedTab,
                selectedCategoryId = selectedCategoryId,
                isFiltered = selectedShelf != NovaLibraryShelf.Current,
                archiveCategoryId = archiveCategory?.id,
                selectedTransitionMangaId = selectedTransitionMangaId,
                gridState = gridState,
                onSearch = onSearch,
                onOpenManga = openMangaWithTransition,
                onPlay = playManga,
                onSetArchived = onSetArchived,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    if (showGalleryOptions) {
        ModalBottomSheet(
            onDismissRequest = { showGalleryOptions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
            ) {
                NovaCatalogDisplayPicker(
                    selected = display,
                    onSelect = {
                        displayPreference.set(it)
                        showGalleryOptions = false
                    },
                )
                Text(
                    text = librarySummary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = NovaVikiColors.TextSecondary,
                )
                NovaLibraryControlBand(
                    selectedShelf = selectedShelf,
                    currentCount = currentItems.size,
                    archiveCount = archivedItems.size,
                    selectedTab = selectedTab,
                    allSeriesCount = libraryItems.size,
                    lightNovelCount = lightNovelItems.size,
                    categories = collectionCategories,
                    categoryCounts = categoryCounts,
                    selectedCategoryId = selectedCategoryId,
                    sortOption = sortOption,
                    sortDescending = sortDescending,
                    elevated = false,
                    onSelectShelf = { shelf ->
                        selectedShelfIndex = shelf.ordinal
                        selectedCategoryId = null
                    },
                    onSelectTab = { selectedTab = it },
                    onSelectCategory = { selectedCategoryId = it },
                    onSortOptionSelected = { sortOption = it },
                    onToggleDirection = { sortDescending = !sortDescending },
                )
            }
        }
    }
}

@Composable
private fun NovaLibraryGalleryHeader(
    onSearch: () -> Unit,
    onCalendar: () -> Unit,
    onOptions: () -> Unit,
    activeFilter: String?,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets),
        contentAlignment = Alignment.Center,
    ) {
        val gutter = novaCatalogMetrics(maxWidth.value.toInt(), NovaLibraryPresentation.Grid).sidePadding.dp
        val showLibraryIcon = maxWidth >= 370.dp
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = gutter, end = (gutter - 6.dp).coerceAtLeast(6.dp))
                .heightIn(min = 58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showLibraryIcon) {
                Icon(
                    NovaIcons.Library,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(MR.strings.label_library),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 0.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                activeFilter?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            NovaToolbarIconButton(NovaIcons.Calendar, stringResource(MR.strings.nova_release_calendar), onCalendar)
            NovaToolbarIconButton(NovaIcons.Search, stringResource(MR.strings.action_search), onSearch)
            NovaToolbarIconButton(NovaIcons.Filter, stringResource(MR.strings.action_filter), onOptions)
        }
    }
}

@Composable
private fun NovaCatalogDisplayPicker(
    selected: NovaLibraryPresentation,
    onSelect: (NovaLibraryPresentation) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(stringResource(MR.strings.pref_category_display), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NovaLibraryPresentation.entries.forEach { option ->
                val (icon, label) = when (option) {
                    NovaLibraryPresentation.Grid -> Icons.Outlined.GridView to MR.strings.nova_library_view_grid
                    NovaLibraryPresentation.List -> Icons.Outlined.ViewList to MR.strings.action_display_list
                    NovaLibraryPresentation.Cards -> Icons.Outlined.ViewAgenda to MR.strings.nova_library_view_cards
                }
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(stringResource(label)) },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun NovaLibraryGrid(
    items: List<NovaVikiManga>,
    display: NovaLibraryPresentation,
    isLibraryReady: Boolean,
    selectedTab: Int,
    selectedCategoryId: Long?,
    isFiltered: Boolean,
    archiveCategoryId: Long?,
    selectedTransitionMangaId: Long?,
    gridState: LazyGridState,
    onSearch: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onPlay: (Long?) -> Unit,
    onSetArchived: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(
                    max = when (display) {
                        NovaLibraryPresentation.Grid -> 600.dp
                        NovaLibraryPresentation.List -> 720.dp
                        NovaLibraryPresentation.Cards -> 1120.dp
                    },
                )
                .fillMaxSize(),
        ) {
            val metrics = novaCatalogMetrics(maxWidth.value.toInt(), display)
            val horizontalPadding = metrics.sidePadding.dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(metrics.columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = when (display) {
                        NovaLibraryPresentation.Grid -> 12.dp
                        NovaLibraryPresentation.List -> 18.dp
                        NovaLibraryPresentation.Cards -> 26.dp
                    },
                    end = horizontalPadding,
                    bottom = 18.dp + LocalNovaDockBottomInset.current,
                ),
                horizontalArrangement = Arrangement.spacedBy(metrics.columnGap.dp),
                verticalArrangement = Arrangement.spacedBy(metrics.rowGap.dp),
            ) {
                if (!isLibraryReady) {
                    items(
                        count = metrics.columns * if (display == NovaLibraryPresentation.Grid) 3 else 2,
                        key = { index -> "library_loading_$index" },
                        contentType = { "library_loading_card" },
                    ) {
                        NovaLibraryLoadingCard(display)
                    }
                } else if (items.isEmpty()) {
                    item(
                        key = "library_empty",
                        contentType = "library_empty_state",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        NovaLibraryEmptyState(
                            title = if (isFiltered) {
                                stringResource(MR.strings.nova_library_empty_selection)
                            } else if (selectedTab == 0) {
                                if (selectedCategoryId != null) {
                                    "Aucun manhwa dans cette catégorie"
                                } else {
                                    "Aucun manhwa dans la bibliothèque"
                                }
                            } else if (selectedCategoryId != null) {
                                "Aucun light novel dans cette catégorie"
                            } else {
                                "Aucun light novel dans la bibliothèque"
                            },
                            onSearch = onSearch,
                        )
                    }
                } else {
                    items(
                        items = items,
                        key = NovaVikiManga::id,
                        contentType = { "library_grid_card" },
                    ) { item ->
                        val archived = archiveCategoryId?.let(item.categoryIds::contains) == true
                        NovaLibraryGridCard(
                            item = item,
                            display = display,
                            archived = archived,
                            sharedTransitionActive = selectedTransitionMangaId == item.id,
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                placementSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                fadeOutSpec = null,
                            ),
                            onClick = {
                                if (item.id >= 0L) {
                                    onOpenManga(item.id)
                                }
                            },
                            onLongClick = {
                                if (item.id >= 0L) {
                                    onPlay(item.id)
                                }
                            },
                            onArchive = {
                                if (item.id >= 0L) {
                                    onSetArchived(item.id, !archived)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaLibraryControlBand(
    selectedShelf: NovaLibraryShelf,
    currentCount: Int,
    archiveCount: Int,
    selectedTab: Int,
    allSeriesCount: Int,
    lightNovelCount: Int,
    categories: List<NovaLibraryCategory>,
    categoryCounts: Map<Long, Int>,
    selectedCategoryId: Long?,
    sortOption: NovaLibrarySortOption,
    sortDescending: Boolean,
    elevated: Boolean,
    onSelectShelf: (NovaLibraryShelf) -> Unit,
    onSelectTab: (Int) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onSortOptionSelected: (NovaLibrarySortOption) -> Unit,
    onToggleDirection: () -> Unit,
    verticalLayout: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (elevated) NovaVikiColors.Surface else NovaVikiColors.Background,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "library_controls_background",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (verticalLayout) {
                    Modifier.novaGlassSurface(
                        color = background,
                        outline = NovaVikiColors.Outline,
                    )
                } else {
                    Modifier.background(background)
                },
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compact = maxWidth < 360.dp
        val expandedLayout = maxWidth >= 600.dp
        val horizontalPadding = when {
            compact -> 10.dp
            expandedLayout -> 18.dp
            else -> 14.dp
        }
        Column(
            modifier = Modifier
                .widthIn(max = NovaVikiDimensions.GridContentMaxWidth)
                .fillMaxWidth()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 2.dp,
                    bottom = 4.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (verticalLayout) {
                Text(
                    text = "ESPACE BIBLIOTHÈQUE",
                    color = NovaVikiColors.Accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                )
            }
            if (verticalLayout) {
                NovaLibraryModeSelector(
                    selectedTab = selectedTab,
                    allSeriesCount = allSeriesCount,
                    lightNovelCount = lightNovelCount,
                    onSelectTab = onSelectTab,
                    modifier = Modifier.fillMaxWidth(),
                )
                NovaLibraryShelfSelector(
                    selectedShelf = selectedShelf,
                    selectedCategoryId = selectedCategoryId,
                    currentCount = currentCount,
                    archiveCount = archiveCount,
                    categories = categories,
                    categoryCounts = categoryCounts,
                    onSelectShelf = onSelectShelf,
                    onSelectCategory = onSelectCategory,
                )
                NovaLibrarySortActions(
                    sortOption = sortOption,
                    sortDescending = sortDescending,
                    showSortLabel = false,
                    onSortOptionSelected = onSortOptionSelected,
                    onToggleDirection = onToggleDirection,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NovaLibraryModeSelector(
                        selectedTab = selectedTab,
                        allSeriesCount = allSeriesCount,
                        lightNovelCount = lightNovelCount,
                        onSelectTab = onSelectTab,
                        modifier = Modifier.weight(1f),
                    )
                    NovaLibrarySortActions(
                        sortOption = sortOption,
                        sortDescending = sortDescending,
                        showSortLabel = expandedLayout,
                        onSortOptionSelected = onSortOptionSelected,
                        onToggleDirection = onToggleDirection,
                    )
                }
                NovaLibraryShelfSelector(
                    selectedShelf = selectedShelf,
                    selectedCategoryId = selectedCategoryId,
                    currentCount = currentCount,
                    archiveCount = archiveCount,
                    categories = categories,
                    categoryCounts = categoryCounts,
                    onSelectShelf = onSelectShelf,
                    onSelectCategory = onSelectCategory,
                )
            }
        }
    }
}

@Composable
private fun NovaLibraryShelfSelector(
    selectedShelf: NovaLibraryShelf,
    selectedCategoryId: Long?,
    currentCount: Int,
    archiveCount: Int,
    categories: List<NovaLibraryCategory>,
    categoryCounts: Map<Long, Int>,
    onSelectShelf: (NovaLibraryShelf) -> Unit,
    onSelectCategory: (Long?) -> Unit,
) {
    val options = novaLibraryShelfOptions(
        currentCount = currentCount,
        archiveCount = archiveCount,
        categories = categories,
        categoryCounts = categoryCounts,
    )
    val selectedIndex = options.indexOfFirst { option ->
        option.shelf == selectedShelf && option.categoryId == selectedCategoryId
    }.coerceAtLeast(0)
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        contentColor = NovaVikiColors.TextPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEachIndexed { index, option ->
            val selected = selectedIndex == index
            Tab(
                selected = selected,
                onClick = {
                    onSelectShelf(option.shelf)
                    onSelectCategory(option.categoryId)
                },
                modifier = Modifier.height(NovaVikiDimensions.MinimumTouchTarget),
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        NovaCountBadge(
                            value = option.count,
                            selected = selected,
                            contentDescription = "${option.count} éléments",
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun NovaLibraryModeSelector(
    selectedTab: Int,
    allSeriesCount: Int,
    lightNovelCount: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        "All Series" to allSeriesCount,
        "Light Novel" to lightNovelCount,
    )
    PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = NovaVikiColors.TextPrimary,
        modifier = modifier,
    ) {
        tabs.forEachIndexed { index, (label, count) ->
            val selected = selectedTab == index
            Tab(
                selected = selected,
                onClick = { onSelectTab(index) },
                modifier = Modifier.height(NovaVikiDimensions.MinimumTouchTarget),
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        NovaCountBadge(
                            value = count,
                            selected = selected,
                            contentDescription = "$count œuvres",
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun NovaLibrarySortActions(
    sortOption: NovaLibrarySortOption,
    sortDescending: Boolean,
    showSortLabel: Boolean,
    onSortOptionSelected: (NovaLibrarySortOption) -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .height(NovaVikiDimensions.MinimumTouchTarget)
                    .clip(RoundedCornerShape(NovaVikiDimensions.ControlCorner))
                    .background(NovaVikiColors.SurfaceRaised)
                    .clickable(
                        role = Role.Button,
                        onClick = { expanded = true },
                    )
                    .semantics {
                        contentDescription = "Tri: ${sortOption.label}"
                    }
                    .padding(horizontal = if (showSortLabel) 12.dp else 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SortByAlpha,
                    contentDescription = null,
                    tint = NovaVikiColors.TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
                if (showSortLabel) {
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = sortOption.label,
                        color = NovaVikiColors.TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                NovaLibrarySortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                fontWeight = if (option == sortOption) FontWeight.Black else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onSortOptionSelected(option)
                            expanded = false
                        },
                        trailingIcon = if (option == sortOption) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = NovaVikiColors.Accent,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = onToggleDirection,
            modifier = Modifier.size(NovaVikiDimensions.MinimumTouchTarget),
        ) {
            Icon(
                imageVector = if (sortDescending) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                contentDescription = if (sortDescending) "Tri descendant" else "Tri ascendant",
                tint = NovaVikiColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun NovaLibraryGridCard(
    item: NovaVikiManga,
    display: NovaLibraryPresentation,
    archived: Boolean,
    sharedTransitionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.988f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "catalog-card-press",
    )
    var showActions by remember { mutableStateOf(false) }
    val archiveLabel = stringResource(if (archived) MR.strings.nova_library_unarchive else MR.strings.nova_library_archive)
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .novaCatalogCard()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
                onLongClickLabel = archiveLabel,
                onLongClick = { showActions = true },
            ),
    ) {
        when (display) {
            NovaLibraryPresentation.Grid -> {
                Column(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    NovaCatalogCover(item, sharedTransitionActive, Modifier.fillMaxWidth().aspectRatio(1f))
                    NovaCatalogTitle(item.title, compact = true, align = TextAlign.Center, maxLines = 1)
                    NovaCatalogSource(item.tag, Modifier.align(Alignment.CenterHorizontally))
                    NovaCatalogStatus(item.subtitle, compact = true, align = TextAlign.Center)
                }
            }
            NovaLibraryPresentation.List -> {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
                    val coverWidth = (maxWidth * 0.38f).coerceAtMost(120.dp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NovaCatalogCover(
                            item,
                            sharedTransitionActive,
                            Modifier.width(coverWidth).aspectRatio(1f),
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            NovaCatalogSource(
                                item.tag,
                                Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(colors.surfaceContainer)
                                    .border(1.dp, colors.outlineVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 3.dp),
                            )
                            NovaCatalogTitle(item.title, compact = false, align = TextAlign.Start, maxLines = 2)
                            NovaCatalogStatus(item.subtitle, compact = true, align = TextAlign.Start)
                        }
                    }
                }
            }
            NovaLibraryPresentation.Cards -> {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.surfaceContainer)
                            .border(1.dp, colors.outlineVariant, RoundedCornerShape(7.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        NovaCatalogSource(item.tag)
                    }
                    Spacer(Modifier.height(16.dp))
                    NovaCatalogCover(
                        item,
                        sharedTransitionActive,
                        Modifier.fillMaxWidth(0.54f).widthIn(max = 170.dp).aspectRatio(1f),
                    )
                    Spacer(Modifier.height(17.dp))
                    NovaCatalogTitle(item.title, compact = false, align = TextAlign.Center, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    NovaCatalogStatus(item.subtitle, compact = false, align = TextAlign.Center)
                    Spacer(Modifier.height(9.dp))
                }
            }
        }
        DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_show_manga)) },
                onClick = { showActions = false; onLongClick() },
            )
            DropdownMenuItem(
                text = { Text(archiveLabel) },
                leadingIcon = { Icon(if (archived) Icons.Outlined.BookmarkRemove else Icons.Outlined.BookmarkAdd, null) },
                onClick = { showActions = false; onArchive() },
            )
        }
    }
}

@Composable
private fun Modifier.novaCatalogCard(): Modifier {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    return shadow(3.dp, shape, clip = false, ambientColor = Color(0x250F1628), spotColor = Color(0x250F1628))
        .clip(shape)
        .background(colors.surface)
        .border(1.dp, colors.outlineVariant, shape)
}

@Composable
private fun NovaCatalogCover(item: NovaVikiManga, shared: Boolean, modifier: Modifier = Modifier) {
    val saturation = coverSaturation(item.progress, item.remainingChapters)
    val filter = remember(saturation) { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }) }
    val request = rememberHighQualityImageRequest(item.coverData, widthPx = 512, heightPx = 512, exactSize = true)
    Box(modifier.clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = filter,
            modifier = Modifier.novaMangaSharedArtwork(item.id, enabled = shared).fillMaxSize(),
        )
    }
}

@Composable
private fun NovaCatalogTitle(title: String, compact: Boolean, align: TextAlign, maxLines: Int) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth(),
        fontSize = if (compact) 15.sp else 19.sp,
        lineHeight = if (compact) 19.sp else 24.sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Bold,
        textAlign = align,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun NovaCatalogSource(source: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.LocalOffer, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = source,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            letterSpacing = 0.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NovaCatalogStatus(status: String, compact: Boolean, align: TextAlign) {
    Text(
        text = status,
        modifier = Modifier.fillMaxWidth(),
        textAlign = align,
        fontSize = if (compact) 12.sp else 16.sp,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = if (compact) 16.sp else 21.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun NovaLibraryLoadingCard(display: NovaLibraryPresentation) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(
                when (display) {
                    NovaLibraryPresentation.Grid -> 0.73f
                    NovaLibraryPresentation.List -> 2.36f
                    NovaLibraryPresentation.Cards -> 0.94f
                },
            )
            .novaCatalogCard()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth(if (display == NovaLibraryPresentation.Cards) 0.48f else 0.75f)
                .fillMaxHeight(if (display == NovaLibraryPresentation.List) 0.75f else 0.62f)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

private enum class NovaLibrarySortOption(val label: String) {
    LastRead("Dernière lecture"),
    Alphabetical("A-Z"),
    RecentlyAdded("Ajout récent"),
    LatestChapter("Nouveaux chapitres"),
    Unread("À lire"),
    TotalChapters("Total chapitres"),
}

private fun List<NovaVikiManga>.sortedForLibrary(
    option: NovaLibrarySortOption,
    descending: Boolean,
): List<NovaVikiManga> {
    val titleComparator = compareBy<NovaVikiManga, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
    val comparator: Comparator<NovaVikiManga> = when (option) {
        NovaLibrarySortOption.LastRead -> compareBy<NovaVikiManga> { it.lastRead }
        NovaLibrarySortOption.Alphabetical -> titleComparator
        NovaLibrarySortOption.RecentlyAdded -> compareBy { it.dateAdded }
        NovaLibrarySortOption.LatestChapter -> compareBy { it.latestUpload }
        NovaLibrarySortOption.Unread -> compareBy { it.remainingChapters }
        NovaLibrarySortOption.TotalChapters -> compareBy { it.totalChapters }
    }.then(titleComparator)

    return sortedWith(if (descending) comparator.reversed() else comparator)
}

@Composable
private fun NovaLibraryEmptyState(
    title: String,
    onSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = NovaVikiColors.TextSecondary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onSearch,
            modifier = Modifier.height(NovaVikiDimensions.MinimumTouchTarget),
            shape = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
        ) {
            Icon(
                imageVector = NovaIcons.Search,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text("Rechercher")
        }
    }
}
