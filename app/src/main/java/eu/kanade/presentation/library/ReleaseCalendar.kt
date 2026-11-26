package eu.kanade.presentation.library

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

internal data class ReleaseLocalTime(val date: LocalDate, val time: LocalTime?)

internal fun calendarReleaseTime(legacyTime: Long, sourceTime: Long?, zone: ZoneId): ReleaseLocalTime? {
    val release = releaseLocalTime(sourceTime ?: legacyTime, zone) ?: return null
    return if (sourceTime == null) release.copy(time = null) else release
}

internal fun releaseLocalTime(publishedAt: Long, zone: ZoneId): ReleaseLocalTime? {
    if (publishedAt <= 0L) return null
    val published = Instant.ofEpochMilli(publishedAt)
    val utc = published.atOffset(ZoneOffset.UTC)
    val local = published.atZone(zone)
    // Source APIs provide a timestamp, not precision. Midnight is commonly used for a date alone.
    if (utc.toLocalTime() == LocalTime.MIDNIGHT) return ReleaseLocalTime(utc.toLocalDate(), null)
    return ReleaseLocalTime(local.toLocalDate(), local.toLocalTime().takeUnless { it == LocalTime.MIDNIGHT })
}

internal fun releaseWeekStart(day: LocalDate): LocalDate =
    day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

internal fun releaseMonthDays(month: YearMonth): List<LocalDate> {
    val first = releaseWeekStart(month.atDay(1))
    val last = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val count = (last.toEpochDay() - first.toEpochDay()).toInt() + 1
    return List(count) { first.plusDays(it.toLong()) }
}
