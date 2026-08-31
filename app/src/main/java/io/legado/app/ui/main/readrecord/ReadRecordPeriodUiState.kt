package io.legado.app.ui.main.readrecord

import java.time.LocalDate

enum class ReadRecordPeriodMode {
    YEAR, MONTH, WEEK
}

data class ReadRecordPeriodUiState(
    val mode: ReadRecordPeriodMode,
    val cursorDate: LocalDate,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalReadTime: Long,
    val readDayCount: Int,
    val bookCount: Int,
    val canGoNext: Boolean
)
