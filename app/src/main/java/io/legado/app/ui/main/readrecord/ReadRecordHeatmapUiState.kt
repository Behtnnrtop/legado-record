package io.legado.app.ui.main.readrecord

import java.time.LocalDate

data class ReadRecordHeatmapUiState(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalReadTime: Long = 0L,
    val bookCount: Int = 0,
    val readDayCount: Int = 0,
    val maxDayReadTime: Long = 0L,
    val days: Map<String, Long> = emptyMap()
)
