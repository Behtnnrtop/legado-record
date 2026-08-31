package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.ReadRecordLegacyMapping

@Dao
interface ReadRecordLegacyMappingDao {

    @get:Query("select * from readRecordLegacyMappings")
    val all: List<ReadRecordLegacyMapping>

    @Query("select bookUrl from readRecordLegacyMappings where deviceId = :deviceId and bookName = :bookName")
    fun getBookUrl(deviceId: String, bookName: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(mapping: ReadRecordLegacyMapping)

    @Query("delete from readRecordLegacyMappings where deviceId = :deviceId and bookName = :bookName")
    fun delete(deviceId: String, bookName: String)

    @Query("update readRecordLegacyMappings set bookUrl = :newBookUrl, updateTime = :updateTime where bookUrl = :oldBookUrl")
    fun updateBookUrl(oldBookUrl: String, newBookUrl: String, updateTime: Long)

    @Query("delete from readRecordLegacyMappings where bookUrl = :bookUrl")
    fun deleteByBookUrl(bookUrl: String)

    @Query("delete from readRecordLegacyMappings")
    fun clear()
}
