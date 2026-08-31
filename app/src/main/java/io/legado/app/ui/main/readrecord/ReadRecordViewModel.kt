package io.legado.app.ui.main.readrecord

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReadRecordTopBook
import io.legado.app.model.ReadRecordTotalModel
import io.legado.app.ui.widget.readrecord.ReadRecordHeatmapHelper
import java.time.LocalDate
import java.time.YearMonth

class ReadRecordViewModel(application: Application) : BaseViewModel(application) {

    val heatmapLiveData = MutableLiveData<ReadRecordHeatmapUiState>()
    val recentReadBooksLiveData = MutableLiveData<List<Book>>()
    val periodSummaryLiveData = MutableLiveData<ReadRecordPeriodUiState>()
    val topBooksLiveData = MutableLiveData<List<ReadRecordTopBook>>()
    val weekBarLiveData = MutableLiveData<ReadRecordWeekBarUiState>()
    val hourHistogramLiveData = MutableLiveData<ReadRecordHourHistogramUiState>()

    private var periodMode = ReadRecordPeriodMode.MONTH
    private var cursorDate: LocalDate = LocalDate.now()
    private var weekBarCursorDate: LocalDate = LocalDate.now()

    fun loadHeatmap() {
        execute {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(364)
            val days = appDb.bookReadDayRecordDao
                .getHeatmapDays(startDate.toString(), endDate.toString())
                .asSequence()
                .filter { it.readTime > 0L }
                .associate { it.date to it.readTime }
            val totalSummary = ReadRecordTotalModel.buildSummary()
            ReadRecordHeatmapUiState(
                startDate = startDate,
                endDate = endDate,
                totalReadTime = totalSummary.totalReadTime,
                bookCount = totalSummary.bookCount,
                readDayCount = days.size,
                maxDayReadTime = days.values.maxOrNull() ?: 0L,
                days = days
            )
        }.onSuccess {
            heatmapLiveData.value = it
        }
    }

    fun loadRecentReadBooks() {
        execute {
            appDb.bookDao.getRecentReadBooks(4)
        }.onSuccess {
            recentReadBooksLiveData.value = it
        }
    }

    fun loadPeriodSummary() {
        execute {
            val today = LocalDate.now()
            val startDate = periodStartDate(cursorDate, periodMode)
            val endDate = periodEndDate(cursorDate, periodMode)
            val summary = appDb.bookReadDayRecordDao
                .getPeriodSummary(startDate.toString(), endDate.toString())
            ReadRecordPeriodUiState(
                mode = periodMode,
                cursorDate = cursorDate,
                startDate = startDate,
                endDate = endDate,
                totalReadTime = summary.totalReadTime,
                readDayCount = summary.readDayCount,
                bookCount = ReadRecordTotalModel.getResolvedPeriodBookCount(
                    startDate.toString(),
                    endDate.toString()
                ),
                canGoNext = endDate < today
            )
        }.onSuccess {
            periodSummaryLiveData.value = it
        }
        loadTopBooks()
    }

    fun loadTopBooks() {
        execute {
            val startDate = periodStartDate(cursorDate, periodMode)
            val endDate = periodEndDate(cursorDate, periodMode)
            ReadRecordTotalModel.buildTopBooksInPeriod(
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                limit = 4
            )
        }.onSuccess {
            topBooksLiveData.value = it
        }
    }

    fun loadWeekBars() {
        execute {
            val today = LocalDate.now()
            val startDate = ReadRecordHeatmapHelper.weekStart(weekBarCursorDate)
            val endDate = ReadRecordHeatmapHelper.weekEnd(weekBarCursorDate)
            val days = appDb.bookReadDayRecordDao.getHeatmapDays(
                startDate.toString(),
                endDate.toString()
            )
            ReadRecordWeekBarStateFactory.build(
                cursorDate = weekBarCursorDate,
                today = today,
                days = days
            )
        }.onSuccess {
            weekBarLiveData.value = it
        }
        loadHourHistogram()
    }

    fun previousWeekBar() {
        weekBarCursorDate = weekBarCursorDate.minusWeeks(1)
        loadWeekBars()
    }

    fun nextWeekBar() {
        val nextCursorDate = weekBarCursorDate.plusWeeks(1)
        if (ReadRecordHeatmapHelper.weekStart(nextCursorDate) > LocalDate.now()) return
        weekBarCursorDate = nextCursorDate
        loadWeekBars()
    }

    fun loadHourHistogram() {
        execute {
            val startDate = ReadRecordHeatmapHelper.weekStart(weekBarCursorDate)
            val endDate = ReadRecordHeatmapHelper.weekEnd(weekBarCursorDate)
            val zoneId = java.time.ZoneId.systemDefault()
            val rangeStart = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val rangeEnd = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val sessions = appDb.bookReadSessionDao.getByTimeRange(rangeStart, rangeEnd)
            ReadRecordHourHistogramUiState(
                startDate = startDate,
                endDate = endDate,
                counts = ReadRecordStatsHelper.buildHourHistogram(
                    sessions = sessions,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    zoneId = zoneId
                )
            )
        }.onSuccess {
            hourHistogramLiveData.value = it
        }
    }

    fun switchPeriodMode(mode: ReadRecordPeriodMode) {
        if (periodMode == mode) return
        periodMode = mode
        cursorDate = LocalDate.now()
        loadPeriodSummary()
    }

    fun previousPeriod() {
        cursorDate = when (periodMode) {
            ReadRecordPeriodMode.YEAR -> cursorDate.minusYears(1)
            ReadRecordPeriodMode.MONTH -> cursorDate.minusMonths(1)
            ReadRecordPeriodMode.WEEK -> cursorDate.minusWeeks(1)
        }
        loadPeriodSummary()
    }

    fun nextPeriod() {
        val nextCursorDate = when (periodMode) {
            ReadRecordPeriodMode.YEAR -> cursorDate.plusYears(1)
            ReadRecordPeriodMode.MONTH -> cursorDate.plusMonths(1)
            ReadRecordPeriodMode.WEEK -> cursorDate.plusWeeks(1)
        }
        if (periodStartDate(nextCursorDate, periodMode) > LocalDate.now()) return
        cursorDate = nextCursorDate
        loadPeriodSummary()
    }

    private fun periodStartDate(date: LocalDate, mode: ReadRecordPeriodMode): LocalDate {
        return when (mode) {
            ReadRecordPeriodMode.YEAR -> LocalDate.of(date.year, 1, 1)
            ReadRecordPeriodMode.MONTH -> date.withDayOfMonth(1)
            ReadRecordPeriodMode.WEEK -> ReadRecordHeatmapHelper.weekStart(date)
        }
    }

    private fun periodEndDate(date: LocalDate, mode: ReadRecordPeriodMode): LocalDate {
        return when (mode) {
            ReadRecordPeriodMode.YEAR -> LocalDate.of(date.year, 12, 31)
            ReadRecordPeriodMode.MONTH -> YearMonth.from(date).atEndOfMonth()
            ReadRecordPeriodMode.WEEK -> ReadRecordHeatmapHelper.weekEnd(date)
        }
    }

}
