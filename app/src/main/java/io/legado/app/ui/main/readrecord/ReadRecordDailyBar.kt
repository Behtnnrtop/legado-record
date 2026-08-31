package io.legado.app.ui.main.readrecord

import io.legado.app.data.entities.ReadHeatmapDay
import io.legado.app.ui.widget.readrecord.ReadRecordHeatmapHelper
import java.time.LocalDate

data class ReadRecordDailyBar(
    val date: LocalDate,
    val weekLabel: String,
    val readTime: Long,
    val isToday: Boolean
)

data class ReadRecordWeekBarUiState(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val maxReadTime: Long,
    val canGoNext: Boolean,
    val bars: List<ReadRecordDailyBar>
)

object ReadRecordWeekBarStateFactory {

    private val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    fun build(
        cursorDate: LocalDate,
        today: LocalDate,
        days: List<ReadHeatmapDay>
    ): ReadRecordWeekBarUiState {
        val startDate = ReadRecordHeatmapHelper.weekStart(cursorDate)
        val endDate = ReadRecordHeatmapHelper.weekEnd(cursorDate)
        val dayMap = days.associate { it.date to it.readTime.coerceAtLeast(0L) }
        val bars = weekLabels.mapIndexed { index, weekLabel ->
            val date = startDate.plusDays(index.toLong())
            ReadRecordDailyBar(
                date = date,
                weekLabel = weekLabel,
                readTime = dayMap[date.toString()] ?: 0L,
                isToday = date == today
            )
        }
        return ReadRecordWeekBarUiState(
            startDate = startDate,
            endDate = endDate,
            maxReadTime = bars.maxOfOrNull { it.readTime } ?: 0L,
            canGoNext = endDate < today,
            bars = bars
        )
    }

}
