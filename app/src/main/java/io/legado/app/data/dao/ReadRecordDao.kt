package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.ReadRecord
import io.legado.app.data.entities.ReadRecordShow

@Dao
interface ReadRecordDao {

    @get:Query("select * from readRecord")
    val all: List<ReadRecord>

    @get:Query(
        """
        select bookName, sum(readTime) as readTime, max(lastRead) as lastRead 
        from readRecord 
        group by bookName 
        order by bookName collate localized"""
    )
    val allShow: List<ReadRecordShow>

    @get:Query("select sum(readTime) from readRecord")
    val allTime: Long

    @get:Query("select count(distinct recordKey) from readRecord")
    val bookCount: Int

    @Query(
        """
        select bookName, sum(readTime) as readTime, max(lastRead) as lastRead 
        from readRecord 
        where bookName like '%' || :searchKey || '%'
        group by bookName 
        order by bookName collate localized"""
    )
    fun search(searchKey: String): List<ReadRecordShow>

    @Query("select sum(readTime) from readRecord where recordKey = 'legacy:' || :bookName")
    fun getReadTime(bookName: String): Long?

    @Query("select max(lastRead) from readRecord where recordKey = 'legacy:' || :bookName")
    fun getLastRead(bookName: String): Long?

    @Query("select readTime from readRecord where deviceId = :androidId and recordKey = 'legacy:' || :bookName")
    fun getReadTime(androidId: String, bookName: String): Long?

    @Query("select * from readRecord where recordKey like 'url:%'")
    fun getUrlRecords(): List<ReadRecord>

    @Query("select * from readRecord where recordKey like 'legacy:%'")
    fun getLegacyRecords(): List<ReadRecord>

    @Query("select * from readRecord where deviceId = :deviceId and recordKey = :recordKey")
    fun getByKey(deviceId: String, recordKey: String): ReadRecord?

    @Query("select * from readRecord where recordKey = :recordKey")
    fun getByRecordKey(recordKey: String): List<ReadRecord>

    @Query("select sum(readTime) from readRecord where recordKey = :recordKey")
    fun getReadTimeByKey(recordKey: String): Long?

    @Query("select max(lastRead) from readRecord where recordKey = :recordKey")
    fun getLastReadByKey(recordKey: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg readRecord: ReadRecord)

    @Update
    fun update(vararg record: ReadRecord)

    @Delete
    fun delete(vararg record: ReadRecord)

    @Query("delete from readRecord")
    fun clear()

    @Query("delete from readRecord where recordKey = 'legacy:' || :bookName")
    fun deleteByName(bookName: String)

    @Query("delete from readRecord where recordKey = :recordKey")
    fun deleteByKey(recordKey: String)
}
