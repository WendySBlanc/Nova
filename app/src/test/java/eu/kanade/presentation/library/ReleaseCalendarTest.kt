package eu.kanade.presentation.library

import eu.kanade.domain.chapter.model.novaSourceReleaseTime
import eu.kanade.domain.chapter.model.withNovaSourceReleaseTime
import eu.kanade.domain.chapter.model.withoutNovaSourceReleaseTime
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class ReleaseCalendarTest {
    @Test
    fun `calendar never assigns an hour to midnight date-only metadata`() {
        val paris = ZoneId.of("Europe/Paris")
        val release = releaseLocalTime(Instant.parse("2026-11-25T00:00:00Z").toEpochMilli(), paris)

        release shouldBe ReleaseLocalTime(LocalDate.of(2026, 11, 25), null)
        releaseLocalTime(0L, paris) shouldBe null
        releaseLocalTime(Instant.parse("2026-11-24T23:00:00Z").toEpochMilli(), paris) shouldBe release
    }

    @Test
    fun `source time converts to the actual local hour and date`() {
        val paris = ZoneId.of("Europe/Paris")
        val release = releaseLocalTime(Instant.parse("2026-07-07T22:15:00Z").toEpochMilli(), paris)

        release shouldBe ReleaseLocalTime(LocalDate.of(2026, 7, 8), LocalTime.of(0, 15))
    }

    @Test
    fun `legacy detection timestamps never appear as confirmed hours`() {
        val paris = ZoneId.of("Europe/Paris")
        val legacy = Instant.parse("2026-11-25T13:10:00Z").toEpochMilli()
        val confirmed = Instant.parse("2026-11-25T18:25:00Z").toEpochMilli()

        calendarReleaseTime(legacy, null, paris) shouldBe ReleaseLocalTime(LocalDate.of(2026, 11, 25), null)
        calendarReleaseTime(legacy, 0L, paris) shouldBe null
        calendarReleaseTime(legacy, confirmed, paris)?.time shouldBe LocalTime.of(19, 25)
    }

    @Test
    fun `release provenance does not leak into extension metadata`() {
        val sourceMemo = buildJsonObject { put("page", JsonPrimitive(4)) }
        val tagged = sourceMemo.withNovaSourceReleaseTime(1234L)

        tagged.novaSourceReleaseTime() shouldBe 1234L
        tagged.withoutNovaSourceReleaseTime() shouldBe sourceMemo
        sourceMemo.novaSourceReleaseTime() shouldBe null
    }

    @Test
    fun `month grid starts Monday and includes the complete final week`() {
        val days = releaseMonthDays(YearMonth.of(2023, 10))

        days.first() shouldBe LocalDate.of(2023, 9, 25)
        days.last() shouldBe LocalDate.of(2023, 11, 5)
        days.size shouldBe 42
    }
}
