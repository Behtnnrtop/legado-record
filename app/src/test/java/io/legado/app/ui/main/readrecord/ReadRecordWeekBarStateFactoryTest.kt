package io.legado.app.ui.main.readrecord

import io.legado.app.data.entities.ReadHeatmapDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReadRecordWeekBarStateFactoryTest {

    @Test
    fun build_returnsSevenFixedDaysFromMondayToSunday() {
        val state = ReadRecordWeekBarStateFactory.build(
            cursorDate = LocalDate.parse("2026-08-05"),
            today = LocalDate.parse("2026-08-05"),
            days = emptyList()
        )

        assertEquals(LocalDate.parse("2026-08-03"), state.startDate)
        assertEquals(LocalDate.parse("2026-08-09"), state.endDate)
        assertEquals(7, state.bars.size)
        assertEquals(
            listOf(
                "2026-08-03",
                "2026-08-04",
                "2026-08-05",
                "2026-08-06",
                "2026-08-07",
                "2026-08-08",
                "2026-08-09"
            ),
            state.bars.map { it.date.toString() }
        )
        assertEquals(listOf("一", "二", "三", "四", "五", "六", "日"), state.bars.map { it.weekLabel })
    }

    @Test
    fun build_fillsMissingDaysWithZero() {
        val state = ReadRecordWeekBarStateFactory.build(
            cursorDate = LocalDate.parse("2026-08-05"),
            today = LocalDate.parse("2026-08-05"),
            days = listOf(ReadHeatmapDay("2026-08-05", 30_000L))
        )

        assertEquals(listOf(0L, 0L, 30_000L, 0L, 0L, 0L, 0L), state.bars.map { it.readTime })
        assertEquals(30_000L, state.maxReadTime)
    }

    @Test
    fun build_coercesNegativeReadTimeToZero() {
        val state = ReadRecordWeekBarStateFactory.build(
            cursorDate = LocalDate.parse("2026-08-05"),
            today = LocalDate.parse("2026-08-05"),
            days = listOf(ReadHeatmapDay("2026-08-05", -1L))
        )

        assertEquals(0L, state.bars[2].readTime)
        assertEquals(0L, state.maxReadTime)
    }

    @Test
    fun build_marksOnlyToday() {
        val state = ReadRecordWeekBarStateFactory.build(
            cursorDate = LocalDate.parse("2026-08-05"),
            today = LocalDate.parse("2026-08-05"),
            days = emptyList()
        )

        assertEquals(listOf(false, false, true, false, false, false, false), state.bars.map { it.isToday })
    }

    @Test
    fun build_disablesNextForCurrentWeek() {
        val state = ReadRecordWeekBarStateFactory.build(
            cursorDate = LocalDate.parse("2026-08-05"),
            today = LocalDate.parse("2026-08-05"),
            days = emptyList()
        )

        assertFalse(state.canGoNext)
    }

    @Test
    fun build_enablesNextForPastWeek() {
        val state = ReadRecordWeekBarStateFactory.build(
            cursorDate = LocalDate.parse("2026-07-29"),
            today = LocalDate.parse("2026-08-05"),
            days = emptyList()
        )

        assertTrue(state.canGoNext)
    }
}
