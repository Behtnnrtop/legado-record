package io.legado.app.ui.book.info

import io.legado.app.data.entities.BookReadDayRecord

data class BookReadStats(
    val totalReadTime: Long = 0L,
    val readDayCount: Int = 0,
    val firstReadTime: Long? = null,
    val lastReadTime: Long? = null,
    val dayRecords: List<BookReadDayRecord> = emptyList(),
    val hasDayRecords: Boolean = false
)
