package io.legado.app.ui.main.readrecord

import io.legado.app.data.entities.BookReadSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReadRecordStatsHelperTest {

    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun buildHourHistogram_countsSingleHourSession() {
        val counts = build(
            session("2026-08-03T21:10", "2026-08-03T21:40")
        )

        assertEquals(1, counts[21])
        assertEquals(1, counts.sum())
    }

    @Test
    fun buildHourHistogram_countsEveryCoveredHour() {
        val counts = build(
            session("2026-08-03T21:20", "2026-08-03T22:10")
        )

        assertEquals(1, counts[21])
        assertEquals(1, counts[22])
        assertEquals(2, counts.sum())
    }

    @Test
    fun buildHourHistogram_clipsSessionToRange() {
        val counts = ReadRecordStatsHelper.buildHourHistogram(
            sessions = listOf(session("2026-08-02T23:30", "2026-08-03T01:10")),
            rangeStart = millis("2026-08-03T00:00"),
            rangeEnd = millis("2026-08-10T00:00"),
            zoneId = zoneId
        )

        assertEquals(1, counts[0])
        assertEquals(1, counts[1])
        assertEquals(2, counts.sum())
    }

    @Test
    fun buildHourHistogram_ignoresInvalidSession() {
        val counts = build(
            session("2026-08-03T21:40", "2026-08-03T21:10")
        )

        assertEquals(0, counts.sum())
    }

    private fun build(vararg sessions: BookReadSession): IntArray {
        return ReadRecordStatsHelper.buildHourHistogram(
            sessions = sessions.toList(),
            rangeStart = millis("2026-08-03T00:00"),
            rangeEnd = millis("2026-08-10T00:00"),
            zoneId = zoneId
        )
    }

    private fun session(start: String, end: String): BookReadSession {
        return BookReadSession(
            startTime = millis(start),
            endTime = millis(end)
        )
    }

    private fun millis(value: String): Long {
        return LocalDateTime.parse(value)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

}
