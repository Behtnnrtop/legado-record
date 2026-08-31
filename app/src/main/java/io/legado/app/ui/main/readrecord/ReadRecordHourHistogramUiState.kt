package io.legado.app.ui.main.readrecord

import java.time.LocalDate

data class ReadRecordHourHistogramUiState(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val counts: IntArray
)
