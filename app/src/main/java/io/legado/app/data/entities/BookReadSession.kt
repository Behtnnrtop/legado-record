package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookReadSessions",
    indices = [
        Index(value = ["bookUrl"]),
        Index(value = ["bookUrl", "date"]),
        Index(value = ["deviceId", "bookUrl", "type", "date"]),
        Index(value = ["deviceId", "bookUrl", "type", "startTime", "endTime"], unique = true)
    ]
)
data class BookReadSession(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var deviceId: String = "",
    var bookUrl: String = "",
    var bookName: String = "",
    var bookAuthor: String = "",
    var type: String = "",
    var date: String = "",
    var startTime: Long = 0L,
    var endTime: Long = 0L,
    var duration: Long = 0L
)
