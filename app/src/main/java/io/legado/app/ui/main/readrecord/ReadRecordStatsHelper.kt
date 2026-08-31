package io.legado.app.ui.main.readrecord

import io.legado.app.data.entities.BookReadSession
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

object ReadRecordStatsHelper {

    fun buildHourHistogram(
        sessions: List<BookReadSession>,
        rangeStart: Long,
        rangeEnd: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): IntArray {
        val counts = IntArray(24)
        sessions.forEach { session ->
            val clippedStart = max(session.startTime, rangeStart)
            val clippedEnd = min(session.endTime, rangeEnd)
            if (clippedEnd <= clippedStart) return@forEach

            var cursor = Instant.ofEpochMilli(clippedStart)
                .atZone(zoneId)
                .truncatedTo(ChronoUnit.HOURS)
            val lastHour = Instant.ofEpochMilli(clippedEnd - 1)
                .atZone(zoneId)
                .truncatedTo(ChronoUnit.HOURS)

            while (!cursor.isAfter(lastHour)) {
                counts[cursor.hour]++
                cursor = cursor.plusHours(1)
            }
        }
        return counts
    }

}
