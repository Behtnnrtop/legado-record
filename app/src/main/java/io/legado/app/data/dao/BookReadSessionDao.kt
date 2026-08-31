package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.legado.app.data.entities.BookReadSession

@Dao
interface BookReadSessionDao {

    @get:Query("select * from bookReadSessions")
    val all: List<BookReadSession>

    @Query("select * from bookReadSessions where bookUrl = :bookUrl order by date, startTime")
    fun getByBook(bookUrl: String): List<BookReadSession>

    @Query("select distinct date from bookReadSessions where bookUrl = :bookUrl")
    fun getDatesByBook(bookUrl: String): List<String>

    @Query("select * from bookReadSessions where bookUrl = :bookUrl and date = :date order by startTime, endTime")
    fun getByBookDate(bookUrl: String, date: String): List<BookReadSession>

    @Query(
        """
        select * from bookReadSessions
        where startTime < :rangeEnd
          and endTime > :rangeStart
        order by startTime asc
        """
    )
    fun getByTimeRange(rangeStart: Long, rangeEnd: Long): List<BookReadSession>

    @Query(
        """
        select * from bookReadSessions
        where deviceId = :deviceId
          and bookUrl = :bookUrl
          and type = :type
          and date = :date
        order by endTime desc
        limit 1
        """
    )
    fun getLastLocalSession(
        deviceId: String,
        bookUrl: String,
        type: String,
        date: String
    ): BookReadSession?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(session: BookReadSession): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg sessions: BookReadSession): List<Long>

    @Update
    fun update(session: BookReadSession)

    @Query("delete from bookReadSessions where bookUrl = :bookUrl")
    fun deleteByBook(bookUrl: String)

    @Query("delete from bookReadSessions")
    fun clear()
}
