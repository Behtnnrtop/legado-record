package io.legado.app.model

import io.legado.app.data.entities.BookReadSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BookReadDayRecordModelTest {

    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun splitDurationByLocalDate_sameDay() {
        val startTime = millis(2026, 8, 3, 10, 0)
        val endTime = millis(2026, 8, 3, 10, 10)

        val segments = BookReadDayRecordModel.splitDurationByLocalDate(startTime, endTime, zoneId)

        assertEquals(1, segments.size)
        assertEquals("2026-08-03", segments[0].date)
        assertEquals(10 * 60 * 1000L, segments[0].duration)
    }

    @Test
    fun splitDurationByLocalDate_crossMidnight() {
        val startTime = millis(2026, 8, 3, 23, 50)
        val endTime = millis(2026, 8, 4, 0, 10)

        val segments = BookReadDayRecordModel.splitDurationByLocalDate(startTime, endTime, zoneId)

        assertEquals(2, segments.size)
        assertEquals("2026-08-03", segments[0].date)
        assertEquals("2026-08-04", segments[1].date)
        assertEquals(20 * 60 * 1000L, segments.sumOf { it.duration })
    }

    @Test
    fun splitDurationByLocalDate_crossMultipleDays() {
        val startTime = millis(2026, 8, 3, 23, 50)
        val endTime = millis(2026, 8, 5, 0, 10)

        val segments = BookReadDayRecordModel.splitDurationByLocalDate(startTime, endTime, zoneId)

        assertEquals(3, segments.size)
        assertEquals("2026-08-03", segments.first().date)
        assertEquals("2026-08-05", segments.last().date)
        assertEquals(endTime - startTime, segments.sumOf { it.duration })
    }

    @Test
    fun splitDurationByLocalDate_ignoreInvalidRange() {
        assertTrue(BookReadDayRecordModel.splitDurationByLocalDate(1000L, 1000L, zoneId).isEmpty())
    }

    @Test
    fun mergeSessionsForDay_overlapAcrossDevices_useWidestRange() {
        val date = "2026-08-03"
        val sessions = listOf(
            session("a", date, millis(2026, 8, 3, 10, 0), millis(2026, 8, 3, 10, 30)),
            session("b", date, millis(2026, 8, 3, 10, 10), millis(2026, 8, 3, 10, 40))
        )

        val merged = BookReadDayRecordModel.mergeSessionsForDay(date, sessions)

        assertEquals(40 * 60 * 1000L, merged?.readTime)
        assertEquals(millis(2026, 8, 3, 10, 0), merged?.startTime)
        assertEquals(millis(2026, 8, 3, 10, 40), merged?.lastRead)
    }

    @Test
    fun mergeSessionsForDay_nonOverlap_sumDurations() {
        val date = "2026-08-03"
        val sessions = listOf(
            session("a", date, millis(2026, 8, 3, 10, 0), millis(2026, 8, 3, 10, 30)),
            session("b", date, millis(2026, 8, 3, 11, 0), millis(2026, 8, 3, 11, 20))
        )

        val merged = BookReadDayRecordModel.mergeSessionsForDay(date, sessions)

        assertEquals(50 * 60 * 1000L, merged?.readTime)
        assertEquals(millis(2026, 8, 3, 10, 0), merged?.startTime)
        assertEquals(millis(2026, 8, 3, 11, 20), merged?.lastRead)
    }

    @Test
    fun mergeSessionsForDay_touchingRanges_merge() {
        val date = "2026-08-03"
        val sessions = listOf(
            session("a", date, millis(2026, 8, 3, 10, 0), millis(2026, 8, 3, 10, 30)),
            session("b", date, millis(2026, 8, 3, 10, 30), millis(2026, 8, 3, 10, 40))
        )

        val merged = BookReadDayRecordModel.mergeSessionsForDay(date, sessions)

        assertEquals(40 * 60 * 1000L, merged?.readTime)
    }

    @Test
    fun mergeSessionsForDay_shortGapAcrossDevices_notMerge() {
        val date = "2026-08-03"
        val sessions = listOf(
            session("a", date, millis(2026, 8, 3, 10, 0), millis(2026, 8, 3, 10, 30)),
            session("b", date, millis(2026, 8, 3, 10, 30) + 30_000L, millis(2026, 8, 3, 10, 40))
        )

        val merged = BookReadDayRecordModel.mergeSessionsForDay(date, sessions)

        assertEquals(39 * 60 * 1000L + 30_000L, merged?.readTime)
    }

    @Test
    fun mergeSessionsForDay_ignoreInvalidSessions() {
        val date = "2026-08-03"
        val sessions = listOf(
            session("a", date, 1000L, 1000L),
            session("b", date, 2000L, 1000L)
        )

        assertNull(BookReadDayRecordModel.mergeSessionsForDay(date, sessions))
    }

    private fun session(
        deviceId: String,
        date: String,
        startTime: Long,
        endTime: Long
    ): BookReadSession {
        return BookReadSession(
            deviceId = deviceId,
            bookUrl = "book-url",
            bookName = "book",
            bookAuthor = "author",
            type = BookReadDayRecordModel.TYPE_TEXT,
            date = date,
            startTime = startTime,
            endTime = endTime,
            duration = endTime - startTime
        )
    }

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
