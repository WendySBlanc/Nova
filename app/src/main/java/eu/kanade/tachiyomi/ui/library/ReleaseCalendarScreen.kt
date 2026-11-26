package eu.kanade.tachiyomi.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.chapter.model.novaSourceReleaseTime
import eu.kanade.presentation.components.NovaToolbarIconButton
import eu.kanade.presentation.library.ReleaseLocalTime
import eu.kanade.presentation.library.calendarReleaseTime
import eu.kanade.presentation.library.releaseMonthDays
import eu.kanade.presentation.library.releaseWeekStart
import eu.kanade.presentation.theme.NovaIcons
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

class ReleaseCalendarScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val database = remember { Injekt.get<DatabaseHandler>() }
        val sources = remember { Injekt.get<SourceManager>() }
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val locale = LocalConfiguration.current.locales[0]
        var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
        var mode by rememberSaveable { mutableStateOf(CalendarMode.Month) }
        val selectedDay = LocalDate.ofEpochDay(selectedEpochDay)
        val month = YearMonth.from(selectedDay)
        val week = releaseWeekStart(selectedDay)
        val days = remember(mode, month, week) {
            if (mode == CalendarMode.Month) releaseMonthDays(month) else List(7) { week.plusDays(it.toLong()) }
        }
        val releasesFlow = remember(database, days, zone, sources) {
            val from = days.first().minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val until = days.last().plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            database.subscribeToList { novaCalendarQueries.selectLibraryReleases(from, until) }
                .map { rows ->
                    val now = Instant.now().toEpochMilli()
                    val entries = rows.mapNotNull { row ->
                        val confirmedTimestamp = row.releaseMetadata.novaSourceReleaseTime()
                        val local = calendarReleaseTime(row.publishedAt, confirmedTimestamp, zone)
                            ?: return@mapNotNull null
                        CalendarEntry(
                            mangaId = row.mangaId,
                            chapterId = row.chapterId,
                            title = row.mangaTitle,
                            chapter = row.chapterName,
                            source = sources.getOrStub(row.sourceId).name,
                            local = local,
                            confirmedDate = confirmedTimestamp != null,
                            announced = confirmedTimestamp != null && confirmedTimestamp > now,
                        )
                    }
                    CalendarResult(entries.groupBy { it.local.date })
                }
                .catch { emit(CalendarResult(failed = true)) }
        }
        val result by releasesFlow.collectAsStateWithLifecycle(initialValue = null)
        val entries = result?.days.orEmpty()
        val selectedEntries = entries[selectedDay].orEmpty().sortedBy { it.local.time ?: LocalTime.MAX }
        val dateTitle = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        val scroll = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val shortDate = DateTimeFormatter.ofPattern("d MMM", locale)
        val periodTitle = if (mode == CalendarMode.Month) {
            month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        } else {
            "${days.first().format(shortDate)} – ${days.last().format(shortDate)}"
        }.replaceFirstChar { it.titlecase(locale) }
        val openEntry: (CalendarEntry) -> Unit = { navigator.push(MangaScreen(it.mangaId)) }
        fun goToPeriod(direction: Long) {
            selectedEpochDay = when (mode) {
                CalendarMode.Month -> selectedDay.plusMonths(direction)
                CalendarMode.Week -> selectedDay.plusWeeks(direction)
            }.toEpochDay()
            scope.launch { scroll.scrollToItem(0) }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            periodTitle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        NovaToolbarIconButton(NovaIcons.Back, stringResource(MR.strings.nova_home_back)) {
                            navigator.pop()
                        }
                    },
                    actions = {
                        NovaToolbarIconButton(Icons.Outlined.Today, stringResource(MR.strings.nova_calendar_today)) {
                            selectedEpochDay = today.toEpochDay()
                            scope.launch { scroll.scrollToItem(0) }
                        }
                        NovaToolbarIconButton(
                            NovaIcons.Previous,
                            stringResource(MR.strings.nova_calendar_previous),
                        ) { goToPeriod(-1) }
                        NovaToolbarIconButton(
                            NovaIcons.Next,
                            stringResource(MR.strings.nova_calendar_next),
                        ) { goToPeriod(1) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    CalendarMode.entries.forEach { option ->
                        val title = stringResource(
                            if (option == CalendarMode.Month) MR.strings.nova_calendar_month else MR.strings.nova_calendar_week,
                        )
                        NavigationBarItem(
                            selected = mode == option,
                            onClick = {
                                mode = option
                                scope.launch { scroll.scrollToItem(0) }
                            },
                            icon = {
                                Icon(
                                    if (option == CalendarMode.Month) NovaIcons.Calendar else Icons.Outlined.GridView,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(title) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            },
        ) { insets ->
            LazyColumn(
                state = scroll,
                modifier = Modifier.fillMaxSize().padding(insets),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "calendar") {
                    if (mode == CalendarMode.Month) {
                        MonthGrid(days, month, selectedDay, entries, locale, dateTitle) { selectedEpochDay = it.toEpochDay() }
                    } else {
                        WeekGrid(
                            days, selectedDay, entries, locale, dateTitle, timeFormat, openEntry,
                            onDay = { selectedEpochDay = it.toEpochDay() },
                            onGroup = {
                                selectedEpochDay = it.toEpochDay()
                                scope.launch { scroll.animateScrollToItem(1) }
                            },
                        )
                    }
                }
                item(key = "day") {
                    Text(
                        selectedDay.format(dateTitle).replaceFirstChar { it.titlecase(locale) },
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                when {
                    result == null -> item { LoadingIndicator() }
                    result?.failed == true -> item { CalendarMessage(stringResource(MR.strings.nova_calendar_load_error)) }
                    selectedEntries.isEmpty() -> item { CalendarMessage(stringResource(MR.strings.nova_calendar_no_releases)) }
                    else -> items(selectedEntries, key = { "${it.mangaId}:${it.chapterId}" }) { entry ->
                        CalendarEntryCard(entry, timeFormat) { openEntry(entry) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    days: List<LocalDate>,
    month: YearMonth,
    selected: LocalDate,
    releases: Map<LocalDate, List<CalendarEntry>>,
    locale: Locale,
    dateTitle: DateTimeFormatter,
    onDay: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            days.take(7).forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CalendarWeekday(day, locale) }
            }
        }
        Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, colors.outlineVariant, shape)) {
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        val events = releases[day].orEmpty()
                        Box(
                            Modifier.weight(1f).height(64.dp),
                        ) {
                            CalendarDay(
                                day, selected, events, dateTitle, onDay,
                                Modifier.fillMaxSize().background(colors.surface).border(0.5.dp, colors.outlineVariant),
                                inPeriod = YearMonth.from(day) == month,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekGrid(
    days: List<LocalDate>,
    selected: LocalDate,
    releases: Map<LocalDate, List<CalendarEntry>>,
    locale: Locale,
    dateTitle: DateTimeFormatter,
    timeFormat: DateTimeFormatter,
    openEntry: (CalendarEntry) -> Unit,
    onDay: (LocalDate) -> Unit,
    onGroup: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val timed = remember(days, releases) {
        days.associateWith { day -> releases[day].orEmpty().filter { it.local.time != null }.groupBy { it.local.time!!.hour } }
    }
    val start = minOf(8, timed.values.minOfOrNull { it.keys.minOrNull() ?: 8 } ?: 8)
    val end = maxOf(17, timed.values.maxOfOrNull { it.keys.maxOrNull() ?: 17 } ?: 17)
    val withoutTime = remember(days, releases) {
        days.associateWith { day -> releases[day].orEmpty().count { it.local.time == null } }
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(39.dp))
            days.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    CalendarWeekday(day, locale)
                    CalendarDay(day, selected, releases[day].orEmpty(), dateTitle, onDay, Modifier.fillMaxWidth().height(48.dp))
                }
            }
        }
        if (withoutTime.any { it.value > 0 }) {
            val noTimeLabel = stringResource(MR.strings.nova_calendar_unknown_time)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(39.dp), contentAlignment = Alignment.CenterStart) {
                    Icon(Icons.Outlined.Today, noTimeLabel, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                days.forEach { day ->
                    val count = withoutTime[day] ?: 0
                    Box(
                        Modifier.weight(1f).heightIn(min = 48.dp).then(
                            if (count > 0) Modifier.clickable(role = Role.Button) { onGroup(day) } else Modifier,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (count > 0) {
                            Text(
                                stringResource(MR.strings.nova_calendar_more_releases, count),
                                color = colors.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
        for (hour in start..end) {
            Row(Modifier.fillMaxWidth().height(60.dp)) {
                Text(
                    LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("HH:mm")),
                    modifier = Modifier.width(39.dp).padding(top = 7.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = colors.onSurfaceVariant,
                )
                days.forEach { day ->
                    val events = timed[day]?.get(hour).orEmpty()
                    val first = events.firstOrNull()
                    Box(Modifier.weight(1f).fillMaxHeight().border(0.5.dp, colors.outlineVariant)) {
                        if (first != null) {
                            val accent = first.accentOn(colors.surface)
                            val shape = RoundedCornerShape(8.dp)
                            val label = if (events.size == 1) first.chapter else stringResource(
                                MR.strings.nova_calendar_more_releases,
                                events.size,
                            )
                            Column(
                                Modifier.padding(horizontal = 2.dp)
                                    .offset(y = 12.dp * ((first.local.time?.minute ?: 0) / 60f))
                                    .height(48.dp).fillMaxWidth().clip(shape)
                                    .background(accent.copy(alpha = 0.13f))
                                    .clickable(role = Role.Button) {
                                        if (events.size == 1) openEntry(first) else onGroup(day)
                                    }
                                    .padding(3.dp),
                            ) {
                                Text(
                                    first.local.time?.format(timeFormat).orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                )
                                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarWeekday(day: LocalDate, locale: Locale) {
    Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale), style = MaterialTheme.typography.labelSmall,
        maxLines = 1, overflow = TextOverflow.Clip, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CalendarDay(
    day: LocalDate,
    selected: LocalDate,
    entries: List<CalendarEntry>,
    dateTitle: DateTimeFormatter,
    onDay: (LocalDate) -> Unit,
    modifier: Modifier,
    inPeriod: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val active = day == selected
    val label = stringResource(MR.strings.nova_calendar_day_count, day.format(dateTitle), entries.size)
    Column(
        modifier.selectable(active, role = Role.Tab, onClick = { onDay(day) })
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(30.dp).background(if (active) colors.primary else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, color = when {
                active -> colors.onPrimary
                inPeriod -> colors.onSurface
                else -> colors.onSurfaceVariant
            })
        }
        if (entries.isNotEmpty()) {
            Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                entries.take(3).forEach { Box(Modifier.size(5.dp).background(it.accentOn(colors.surface), CircleShape)) }
            }
        }
    }
}

@Composable
private fun CalendarEntryCard(entry: CalendarEntry, timeFormat: DateTimeFormatter, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(shape).background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).height(56.dp).background(entry.accentOn(colors.surface)))
        Text(
            text = when {
                !entry.confirmedDate -> stringResource(MR.strings.nova_calendar_unverified_date)
                entry.local.time != null -> entry.local.time.format(timeFormat)
                else -> stringResource(MR.strings.nova_calendar_unknown_time)
            },
            modifier = Modifier.width(93.dp).padding(start = 10.dp, end = 5.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = colors.onSurfaceVariant,
        )
        Column(Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 10.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(entry.chapter, entry.source, if (entry.announced) stringResource(MR.strings.nova_calendar_announced) else null)
                    .filter(String::isNotBlank).joinToString(" · "),
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CalendarMessage(message: String) {
    Text(message, modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private enum class CalendarMode { Month, Week }

private data class CalendarResult(val days: Map<LocalDate, List<CalendarEntry>> = emptyMap(), val failed: Boolean = false)

private data class CalendarEntry(
    val mangaId: Long,
    val chapterId: Long,
    val title: String,
    val chapter: String,
    val source: String,
    val local: ReleaseLocalTime,
    val confirmedDate: Boolean,
    val announced: Boolean,
) {
    fun accentOn(surface: Color): Color {
        val accents = if (surface.luminance() < 0.5f) calendarDarkAccents else calendarLightAccents
        return accents[Math.floorMod(source.hashCode(), accents.size)]
    }
}

private val calendarLightAccents = listOf(
    Color(0xFF117A9B), Color(0xFF9D541B), Color(0xFF6650B7), Color(0xFF0B7763), Color(0xFFB63663),
)

private val calendarDarkAccents = listOf(
    Color(0xFF76D4EA), Color(0xFFFFBB77), Color(0xFFBDB0FF), Color(0xFF83DCC2), Color(0xFFFFA5BD),
)
