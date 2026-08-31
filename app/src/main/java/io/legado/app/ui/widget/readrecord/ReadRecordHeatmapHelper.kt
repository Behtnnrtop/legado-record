package io.legado.app.ui.widget.readrecord

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

object ReadRecordHeatmapHelper {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    fun level(readTime: Long): Int {
        val minutes = readTime / MINUTE
        return when {
            minutes <= 0L -> 0
            minutes < 10L -> 1
            minutes < 30L -> 2
            minutes < 60L -> 3
            minutes < 120L -> 4
            else -> 5
        }
    }

    fun formatDuration(duration: Long): String {
        val safeDuration = max(0L, duration)
        val hours = safeDuration / HOUR
        val minutes = (safeDuration % HOUR) / MINUTE
        return when {
            hours > 0L && minutes > 0L -> "${hours}小时${minutes}分钟"
            hours > 0L -> "${hours}小时"
            else -> "${minutes}分钟"
        }
    }

    fun formatDurationWithSpaces(duration: Long): String {
        val safeDuration = max(0L, duration)
        val hours = safeDuration / HOUR
        val minutes = (safeDuration % HOUR) / MINUTE
        return when {
            hours > 0L && minutes > 0L -> "$hours 小时 $minutes 分钟"
            hours > 0L -> "$hours 小时"
            else -> "$minutes 分钟"
        }
    }

    fun weekStart(date: LocalDate): LocalDate {
        return date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    }

    fun weekEnd(date: LocalDate): LocalDate {
        return weekStart(date).plusDays(6)
    }

    fun daysBetweenInclusive(start: LocalDate, end: LocalDate): Long {
        return ChronoUnit.DAYS.between(start, end) + 1
    }

}

data class HeatmapCell(
    val date: LocalDate,
    val column: Int,
    val row: Int,
    val readTime: Long
)
