package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "readRecordLegacyMappings",
    primaryKeys = ["deviceId", "bookName"],
    indices = [Index(value = ["bookUrl"])]
)
data class ReadRecordLegacyMapping(
    val deviceId: String = "",
    val bookName: String = "",
    val bookUrl: String = "",
    val updateTime: Long = 0L
)
