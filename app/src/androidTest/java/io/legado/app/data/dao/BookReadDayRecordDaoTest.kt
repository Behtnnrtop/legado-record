package io.legado.app.data.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookReadDayRecord
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookReadDayRecordDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BookReadDayRecordDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.bookReadDayRecordDao
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_replaceSameBookDateRecord() {
        dao.insert(record("book-url", "2026-08-03", 60_000L, 1000L, 61_000L))
        dao.insert(record("book-url", "2026-08-03", 30_000L, 80_000L, 110_000L))

        val record = dao.getRecord("book-url", "2026-08-03")
        assertNotNull(record)
        assertEquals(30_000L, record!!.readTime)
        assertEquals(80_000L, record.startTime)
        assertEquals(110_000L, record.lastRead)
        assertEquals(1, dao.getByBook("book-url").size)
    }

    @Test
    fun insert_createDifferentDayRecords() {
        dao.insert(record("book-url", "2026-08-03", 60_000L, 1000L, 61_000L))
        dao.insert(record("book-url", "2026-08-04", 30_000L, 80_000L, 110_000L))

        assertEquals(2, dao.getByBook("book-url").size)
        assertEquals(2, dao.getReadDayCount("book-url"))
    }

    @Test
    fun queries_returnBookAggregates() {
        dao.insert(record("book-url", "2026-08-01", 10L, 100L, 200L))
        dao.insert(record("book-url", "2026-08-03", 30L, 300L, 500L))
        dao.insert(record("other-book", "2026-08-03", 50L, 600L, 700L))

        assertEquals(40L, dao.getTotalReadTime("book-url") ?: 0L)
        assertEquals(2, dao.getReadDayCount("book-url"))
        assertEquals(100L, dao.getFirstReadTime("book-url") ?: 0L)
        assertEquals(500L, dao.getLastReadTime("book-url") ?: 0L)
        assertEquals(1, dao.getByBookInDateRange("book-url", "2026-08-02", "2026-08-04").size)
    }

    @Test
    fun getHeatmapDays_returnsDateSumsAcrossBooks() {
        dao.insert(record("book-url", "2026-08-03", 10L, 100L, 200L))
        dao.insert(record("other-book", "2026-08-03", 20L, 300L, 400L))

        val days = dao.getHeatmapDays("2026-08-03", "2026-08-03")

        assertEquals(1, days.size)
        assertEquals("2026-08-03", days[0].date)
        assertEquals(30L, days[0].readTime)
    }

    @Test
    fun getHeatmapDays_filtersByDateRangeAndOrdersByDate() {
        dao.insert(record("book-url", "2026-08-02", 10L, 100L, 200L))
        dao.insert(record("book-url", "2026-08-05", 30L, 300L, 400L))
        dao.insert(record("book-url", "2026-08-03", 20L, 500L, 600L))
        dao.insert(record("book-url", "2026-08-10", 40L, 700L, 800L))

        val days = dao.getHeatmapDays("2026-08-03", "2026-08-09")

        assertEquals(listOf("2026-08-03", "2026-08-05"), days.map { it.date })
    }

    @Test
    fun deleteBook_keepDayRecords() {
        db.bookDao.insert(Book(bookUrl = "book-url", name = "book", author = "author"))
        dao.insert(record("book-url", "2026-08-03", 60_000L, 1000L, 61_000L))

        db.bookDao.delete(Book(bookUrl = "book-url", name = "book", author = "author"))

        assertTrue(dao.getByBook("book-url").isNotEmpty())
    }

    @Test
    fun deleteByBookDate_onlyDeleteOneDay() {
        dao.insert(record("book-url", "2026-08-03", 60_000L, 1000L, 61_000L))
        dao.insert(record("book-url", "2026-08-04", 30_000L, 80_000L, 110_000L))

        dao.deleteByBookDate("book-url", "2026-08-03")

        assertEquals(listOf("2026-08-04"), dao.getByBook("book-url").map { it.date })
    }

    private fun record(
        bookUrl: String,
        date: String,
        readTime: Long,
        startTime: Long,
        lastRead: Long
    ): BookReadDayRecord {
        return BookReadDayRecord(
            bookUrl = bookUrl,
            bookName = "book",
            bookAuthor = "author",
            date = date,
            readTime = readTime,
            startTime = startTime,
            lastRead = lastRead
        )
    }
}
