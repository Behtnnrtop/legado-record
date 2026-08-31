package io.legado.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "bookReadDayRecords",
    primaryKeys = ["bookUrl", "date"],
    indices = [
        Index(value = ["date"]),
        Index(value = ["bookUrl"]),
        Index(value = ["bookUrl", "date"])
    ]
)
data class BookReadDayRecord(
    var bookUrl: String = "",
    var bookName: String = "",
    var bookAuthor: String = "",
    var date: String = "",
    @ColumnInfo(defaultValue = "0")
    var readTime: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    var startTime: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    var lastRead: Long = 0L
)
