package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.BookReadDayRecord
import io.legado.app.data.entities.ReadHeatmapDay
import io.legado.app.data.entities.ReadRecordPeriodSummary
import io.legado.app.data.entities.ReadRecordTopBook

@Dao
interface BookReadDayRecordDao {

    @get:Query("select * from bookReadDayRecords")
    val all: List<BookReadDayRecord>

    @Query("select * from bookReadDayRecords where bookUrl = :bookUrl order by date")
    fun getByBook(bookUrl: String): List<BookReadDayRecord>

    @Query("select distinct date from bookReadDayRecords where bookUrl = :bookUrl")
    fun getDatesByBook(bookUrl: String): List<String>

    @Query("select * from bookReadDayRecords where bookUrl = :bookUrl and date >= :startDate and date <= :endDate order by date")
    fun getByBookInDateRange(
        bookUrl: String,
        startDate: String,
        endDate: String
    ): List<BookReadDayRecord>

    @Query("select * from bookReadDayRecords where date >= :startDate and date <= :endDate")
    fun getRecordsInDateRange(startDate: String, endDate: String): List<BookReadDayRecord>

    @Query(
        """
        select date, sum(readTime) as readTime
        from bookReadDayRecords
        where date >= :startDate and date <= :endDate
        group by date
        order by date
        """
    )
    fun getHeatmapDays(startDate: String, endDate: String): List<ReadHeatmapDay>

    @Query(
        """
        select
            coalesce(sum(readTime), 0) as totalReadTime,
            count(distinct case when readTime > 0 then date end) as readDayCount,
            count(distinct case when readTime > 0 then bookUrl end) as bookCount
        from bookReadDayRecords
        where date >= :startDate and date <= :endDate
        """
    )
    fun getPeriodSummary(startDate: String, endDate: String): ReadRecordPeriodSummary

    @Query(
        """
        select
            record.bookUrl as bookUrl,
            coalesce(nullif(books.name, ''), nullif(record.bookName, '')) as bookName,
            coalesce(nullif(books.author, ''), nullif(record.bookAuthor, '')) as author,
            books.coverUrl as coverUrl,
            books.customCoverUrl as customCoverUrl,
            coalesce(books.type, 8) as type,
            record.readTime as readTime
        from (
            select
                bookUrl,
                max(bookName) as bookName,
                max(bookAuthor) as bookAuthor,
                sum(readTime) as readTime
            from bookReadDayRecords
            where date >= :startDate and date <= :endDate and readTime > 0
            group by bookUrl
            order by readTime desc
            limit :limit
        ) record
        left join books on books.bookUrl = record.bookUrl
        order by record.readTime desc
        """
    )
    fun getTopBooksInPeriod(
        startDate: String,
        endDate: String,
        limit: Int
    ): List<ReadRecordTopBook>

    @Query("select sum(readTime) from bookReadDayRecords where bookUrl = :bookUrl")
    fun getTotalReadTime(bookUrl: String): Long?

    @Query("select count(1) from bookReadDayRecords where bookUrl = :bookUrl and readTime > 0")
    fun getReadDayCount(bookUrl: String): Int

    @Query("select min(startTime) from bookReadDayRecords where bookUrl = :bookUrl and startTime > 0")
    fun getFirstReadTime(bookUrl: String): Long?

    @Query("select max(lastRead) from bookReadDayRecords where bookUrl = :bookUrl")
    fun getLastReadTime(bookUrl: String): Long?

    @Query("select * from bookReadDayRecords where bookUrl = :bookUrl and date = :date")
    fun getRecord(bookUrl: String, date: String): BookReadDayRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: BookReadDayRecord)

    @Query("delete from bookReadDayRecords where bookUrl = :bookUrl")
    fun deleteByBook(bookUrl: String)

    @Query("delete from bookReadDayRecords where bookUrl = :bookUrl and date = :date")
    fun deleteByBookDate(bookUrl: String, date: String)

    @Query("delete from bookReadDayRecords")
    fun clear()
}
