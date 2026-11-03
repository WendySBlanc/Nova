package eu.kanade.presentation.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.presentation.theme.NovaVikiColors
import eu.kanade.presentation.theme.NovaVikiDimensions
import eu.kanade.presentation.library.coverSaturation
import eu.kanade.presentation.util.rememberHighQualityImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

@Immutable
data class NovaLibraryCategory(
    val id: Long,
    val name: String,
)

@Composable
fun NovaVikiLibraryScreen(
    libraryItems: List<NovaVikiManga>,
    lightNovelItems: List<NovaVikiManga>,
    heroItems: List<NovaVikiManga>,
    categories: List<NovaLibraryCategory>,
    isLibraryReady: Boolean,
    onPlay: (Long?) -> Unit,
    onSearch: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onRefresh: () -> Boolean,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var sortOption by rememberSaveable { mutableStateOf(NovaLibrarySortOption.LastRead) }
    var sortDescending by rememberSaveable { mutableStateOf(true) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val hasScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
        }
    }
    val tabItems = remember(selectedTab, libraryItems, lightNovelItems) {
        if (selectedTab == 0) libraryItems else lightNovelItems
    }
    val categoryCounts = remember(tabItems, categories) {
        categories.associate { category ->
            category.id to tabItems.count { item -> category.id in item.categoryIds }
        }
    }
    val visibleItems = remember(tabItems, selectedCategoryId) {
        selectedCategoryId?.let { categoryId ->
            tabItems.filter { item -> categoryId in item.categoryIds }
        } ?: tabItems
    }
    val sortedVisibleItems = remember(visibleItems, sortOption, sortDescending) {
        visibleItems.sortedForLibrary(sortOption, sortDescending)
    }
    val spotlightItems = remember(heroItems, visibleItems) {
        val visibleIds = visibleItems.mapTo(mutableSetOf()) { item -> item.id }
        heroItems
            .filter { item -> item.id in visibleIds }
            .ifEmpty { visibleItems.take(5) }
    }
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
    LaunchedEffect(categories) {
        val selectedId = selectedCategoryId ?: return@LaunchedEffect
        if (categories.none { category -> category.id == selectedId }) {
            selectedCategoryId = null
        }
    }
    LaunchedEffect(selectedTab, selectedCategoryId, sortOption, sortDescending) {
        if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
            gridState.scrollToItem(0)
        }
    }

    PullRefresh(
        refreshing = isRefreshing,
        enabled = true,
        onRefresh = {
            val started = onRefresh()
            if (!started) return@PullRefresh
            scope.launch {
                isRefreshing = true
                delay(1000)
                isRefreshing = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(NovaVikiColors.Background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            NovaVikiSearchHeader(
                title = stringResource(MR.strings.label_library),
                subtitle = librarySummary,
                onSearch = onSearch,
            )
            NovaLibraryControlBand(
                selectedTab = selectedTab,
                allSeriesCount = libraryItems.size,
                lightNovelCount = lightNovelItems.size,
                categories = categories,
                categoryCounts = categoryCounts,
                selectedCategoryId = selectedCategoryId,
                sortOption = sortOption,
                sortDescending = sortDescending,
                elevated = hasScrolled,
                onSelectTab = { selectedTab = it },
                onSelectCategory = { selectedCategoryId = it },
                onSortOptionSelected = { sortOption = it },
                onToggleDirection = { sortDescending = !sortDescending },
            )
            val dividerColor by animateColorAsState(
                targetValue = if (hasScrolled) NovaVikiColors.Outline else Color.Transparent,
                label = "library_controls_divider",
            )
            HorizontalDivider(color = dividerColor)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .widthIn(max = NovaVikiDimensions.GridContentMaxWidth)
                        .fillMaxSize(),
                ) {
                    val minimumCardWidth = when {
                        maxWidth < 360.dp -> 92.dp
                        maxWidth < 600.dp -> 104.dp
                        maxWidth < 840.dp -> 124.dp
                        else -> 148.dp
                    }
                    val loadingColumns = (maxWidth / minimumCardWidth).toInt().coerceAtLeast(2)
                    val horizontalPadding = when {
                        maxWidth < 360.dp -> 8.dp
                        maxWidth < 600.dp -> 12.dp
                        else -> 18.dp
                    }
                    val horizontalGap = when {
                        maxWidth < 360.dp -> 6.dp
                        maxWidth < 600.dp -> 8.dp
                        else -> 12.dp
                    }
                    val verticalGap = when {
                        maxWidth < 360.dp -> 14.dp
                        maxWidth < 600.dp -> 18.dp
                        else -> 22.dp
                    }
                    val cardWidthFraction = 1f

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = minimumCardWidth),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = horizontalPadding,
                            top = 8.dp,
                            end = horizontalPadding,
                            bottom = 24.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
                        verticalArrangement = Arrangement.spacedBy(verticalGap),
                    ) {
                        if (isLibraryReady && visibleItems.isNotEmpty()) {
                            item(
                                key = "library_hero",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                NovaVikiHero(
                                    heroes = spotlightItems,
                                    onPlay = onPlay,
                                    onOpenManga = onOpenManga,
                                    compactLayout = true,
                                )
                            }
                        }
                        if (!isLibraryReady) {
                            items(
                                count = loadingColumns * 2,
                                key = { index -> "library_loading_$index" },
                                contentType = { "library_loading_card" },
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    NovaLibraryLoadingCard(widthFraction = cardWidthFraction)
                                }
                            }
                        } else if (sortedVisibleItems.isEmpty()) {
                            item(
                                key = "library_empty",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                NovaLibraryEmptyState(
                                    title = if (selectedTab == 0) {
                                        if (selectedCategoryId != null) {
                                            "Aucun manhwa dans cette catégorie"
                                        } else {
                                            "Aucun manhwa dans la bibliothèque"
                                        }
                                    } else {
                                        if (selectedCategoryId != null) {
                                            "Aucun light novel dans cette catégorie"
                                        } else {
                                            "Aucun light novel dans la bibliothèque"
                                        }
                                    },
                                    onSearch = onSearch,
                                )
                            }
                        } else {
                            items(
                                items = sortedVisibleItems,
                                key = { item -> item.id },
                                contentType = { "library_grid_card" },
                            ) { item ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    NovaLibraryGridCard(
                                        item = item,
                                        widthFraction = cardWidthFraction,
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
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaLibraryControlBand(
    selectedTab: Int,
    allSeriesCount: Int,
    lightNovelCount: Int,
    categories: List<NovaLibraryCategory>,
    categoryCounts: Map<Long, Int>,
    selectedCategoryId: Long?,
    sortOption: NovaLibrarySortOption,
    sortDescending: Boolean,
    elevated: Boolean,
    onSelectTab: (Int) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onSortOptionSelected: (NovaLibrarySortOption) -> Unit,
    onToggleDirection: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (elevated) NovaVikiColors.Surface else NovaVikiColors.Background,
        label = "library_controls_background",
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(background),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compact = maxWidth < 360.dp
        val expandedLayout = maxWidth >= 600.dp
        val showCompactSortLabel = !compact && maxWidth >= 520.dp
        val activeTotalCount = if (selectedTab == 0) allSeriesCount else lightNovelCount
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
                    top = 6.dp,
                    bottom = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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
                    modifier = if (expandedLayout) {
                        Modifier
                            .widthIn(max = 460.dp)
                            .weight(1f)
                    } else if (categories.isEmpty()) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                )
                if (expandedLayout || categories.isEmpty()) {
                    NovaLibrarySortActions(
                        sortOption = sortOption,
                        sortDescending = sortDescending,
                        showSortLabel = expandedLayout,
                        onSortOptionSelected = onSortOptionSelected,
                        onToggleDirection = onToggleDirection,
                    )
                }
            }
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NovaLibraryCategoryFilters(
                        categories = categories,
                        categoryCounts = categoryCounts,
                        allCount = activeTotalCount,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = onSelectCategory,
                        modifier = Modifier.weight(1f),
                    )
                    if (!expandedLayout) {
                        NovaLibrarySortActions(
                            sortOption = sortOption,
                            sortDescending = sortDescending,
                            showSortLabel = showCompactSortLabel,
                            onSortOptionSelected = onSortOptionSelected,
                            onToggleDirection = onToggleDirection,
                        )
                    }
                }
            }
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
    Row(
        modifier = modifier
            .height(NovaVikiDimensions.MinimumTouchTarget)
            .clip(RoundedCornerShape(NovaVikiDimensions.ControlCorner))
            .background(NovaVikiColors.Surface)
            .border(
                width = 1.dp,
                color = NovaVikiColors.Outline,
                shape = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
            )
            .selectableGroup(),
    ) {
        tabs.forEachIndexed { index, (label, count) ->
            val selected = selectedTab == index
            val background by animateColorAsState(
                targetValue = if (selected) NovaVikiColors.SurfaceInteractive else Color.Transparent,
                label = "library_tab_background",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(background)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelectTab(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$label  $count",
                    color = if (selected) NovaVikiColors.TextPrimary else NovaVikiColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(28.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(NovaVikiColors.Accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaLibraryCategoryFilters(
    categories: List<NovaLibraryCategory>,
    categoryCounts: Map<Long, Int>,
    allCount: Int,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NovaLibraryCategoryChip(
            label = "Toutes",
            count = allCount,
            selected = selectedCategoryId == null,
            onClick = { onSelectCategory(null) },
        )
        categories.forEach { category ->
            NovaLibraryCategoryChip(
                label = category.name,
                count = categoryCounts[category.id] ?: 0,
                selected = selectedCategoryId == category.id,
                onClick = { onSelectCategory(category.id) },
            )
        }
    }
}

@Composable
private fun NovaLibraryCategoryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) NovaVikiColors.SurfaceInteractive else NovaVikiColors.Surface,
        label = "library_category_background",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) NovaVikiColors.Accent else NovaVikiColors.Outline,
        label = "library_category_border",
    )
    Box(
        modifier = Modifier
            .height(NovaVikiDimensions.MinimumTouchTarget)
            .widthIn(max = 220.dp)
            .clip(RoundedCornerShape(NovaVikiDimensions.ControlCorner))
            .background(background)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(NovaVikiDimensions.ControlCorner),
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$label  $count",
            color = if (selected) NovaVikiColors.TextPrimary else NovaVikiColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    widthFraction: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val coverRequest = rememberHighQualityImageRequest(
        data = item.coverData,
        widthPx = NOVA_LIBRARY_COVER_WIDTH_PX,
        heightPx = NOVA_LIBRARY_COVER_HEIGHT_PX,
        exactSize = true,
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "library_card_scale",
    )
    val progressPercent = item.progress
        ?.coerceIn(0f, 1f)
        ?.let { (it * 100f).roundToInt() }
    val saturation = coverSaturation(item.progress)
    val coverColorFilter = remember(saturation) {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(saturation) },
        )
    }
    val readingState = when {
        item.remainingChapters > 1L -> "${item.remainingChapters} chapitres à lire"
        item.remainingChapters == 1L -> "1 chapitre à lire"
        progressPercent != null -> "progression $progressPercent pour cent"
        else -> "à jour"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics(mergeDescendants = true) {
                contentDescription = listOf(item.title, item.subtitle, readingState)
                    .filter { it.isNotBlank() }
                    .joinToString()
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
                .background(Brush.linearGradient(item.colors)),
        ) {
            if (item.coverData != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    colorFilter = coverColorFilter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.58f),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    text = item.tag,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NovaChapterBadges(
                item = item,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
            if (progressPercent != null && progressPercent > 0 && item.totalChapters > 0L) {
                Text(
                    text = "$progressPercent%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 7.dp),
                )
            }
            val progress = item.progress
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.22f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(NovaVikiColors.Accent),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            color = NovaVikiColors.TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            color = NovaVikiColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val NOVA_LIBRARY_COVER_WIDTH_PX = 480
private const val NOVA_LIBRARY_COVER_HEIGHT_PX = 667

@Composable
private fun NovaLibraryLoadingCard(
    widthFraction: Float,
) {
    Column(
        modifier = Modifier.fillMaxWidth(widthFraction),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(NovaVikiDimensions.CardCorner))
                .background(NovaVikiColors.SurfaceRaised),
        )
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(13.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NovaVikiColors.SurfaceStrong),
        )
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NovaVikiColors.SurfaceRaised),
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
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text("Rechercher")
        }
    }
}
