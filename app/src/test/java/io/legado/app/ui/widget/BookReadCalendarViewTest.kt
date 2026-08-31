package io.legado.app.ui.widget

import io.legado.app.data.entities.BookReadDayRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReadCalendarViewTest {

    @Test
    fun sameMonthRecords_highlightMatchedDays() {
        val months = BookReadCalendarView.buildMonths(
            listOf(
                record("2026-08-02"),
                record("2026-08-12"),
                record("2026-08-12")
            )
        )

        assertEquals(1, months.size)
        assertEquals(2026, months[0].year)
        assertEquals(8, months[0].month)
        assertEquals(setOf(2, 12), months[0].readDays)
    }

    @Test
    fun crossMonthRecords_createContinuousMonths() {
        val months = BookReadCalendarView.buildMonths(
            listOf(
                record("2026-08-31"),
                record("2026-09-01")
            )
        )

        assertEquals(listOf(8, 9), months.map { it.month })
    }

    @Test
    fun mondayFirstOffset_matchesWeekStart() {
        val mondayStart = BookReadCalendarView.buildMonths(listOf(record("2026-06-02"))).first()
        val sundayStart = BookReadCalendarView.buildMonths(listOf(record("2026-02-02"))).first()

        assertEquals(0, mondayStart.firstDayOffset)
        assertEquals(6, sundayStart.firstDayOffset)
    }

    @Test
    fun ignoreInvalidAndZeroReadTimeRecords() {
        val months = BookReadCalendarView.buildMonths(
            listOf(
                record("2026-08-02", readTime = 0L),
                record("bad-date"),
                record("2026-08-03")
            )
        )

        assertEquals(1, months.size)
        assertEquals(setOf(3), months[0].readDays)
    }

    @Test
    fun emptyRecords_returnEmptyMonths() {
        assertTrue(BookReadCalendarView.buildMonths(emptyList()).isEmpty())
    }

    private fun record(date: String, readTime: Long = 1L): BookReadDayRecord {
        return BookReadDayRecord(
            bookUrl = "book-url",
            bookName = "book",
            bookAuthor = "author",
            date = date,
            readTime = readTime,
            startTime = 1L,
            lastRead = 2L
        )
    }
}
