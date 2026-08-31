package io.legado.app.data.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.BookReadSession
import io.legado.app.model.BookReadDayRecordModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookReadSessionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BookReadSessionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.bookReadSessionDao
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_duplicateSessionIgnored() {
        val session = session("device", "book-url", "2026-08-03", 1000L, 2000L)

        assertTrue(dao.insert(session) > 0L)
        assertEquals(-1L, dao.insert(session.copy(id = 0L)))

        assertEquals(1, dao.all.size)
    }

    @Test
    fun getByBookDate_onlyReturnTargetDate() {
        dao.insert(session("device", "book-url", "2026-08-03", 1000L, 2000L))
        dao.insert(session("device", "book-url", "2026-08-04", 3000L, 4000L))
        dao.insert(session("device", "other-book", "2026-08-03", 5000L, 6000L))

        val records = dao.getByBookDate("book-url", "2026-08-03")

        assertEquals(1, records.size)
        assertEquals("2026-08-03", records.first().date)
    }

    @Test
    fun getLastLocalSession_returnLatestForSameDeviceBookTypeDate() {
        dao.insert(session("device", "book-url", "2026-08-03", 1000L, 2000L))
        dao.insert(session("device", "book-url", "2026-08-03", 3000L, 4000L))
        dao.insert(session("other-device", "book-url", "2026-08-03", 5000L, 6000L))

        val last = dao.getLastLocalSession(
            deviceId = "device",
            bookUrl = "book-url",
            type = BookReadDayRecordModel.TYPE_TEXT,
            date = "2026-08-03"
        )

        assertEquals(4000L, last?.endTime)
    }

    private fun session(
        deviceId: String,
        bookUrl: String,
        date: String,
        startTime: Long,
        endTime: Long
    ): BookReadSession {
        return BookReadSession(
            deviceId = deviceId,
            bookUrl = bookUrl,
            bookName = "book",
            bookAuthor = "author",
            type = BookReadDayRecordModel.TYPE_TEXT,
            date = date,
            startTime = startTime,
            endTime = endTime,
            duration = endTime - startTime
        )
    }
}
