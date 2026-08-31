package io.legado.app.model

import io.legado.app.constant.AppConst
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookReadDayRecord
import io.legado.app.data.entities.ReadRecord
import io.legado.app.data.entities.ReadRecordLegacyMapping
import io.legado.app.data.entities.ReadRecordTopBook

data class ReadRecordDisplayItem(
    val displayKey: String,
    val bookUrl: String?,
    val bookName: String,
    val bookAuthor: String,
    val readTime: Long,
    val lastRead: Long,
    val recordKeys: List<String>,
    val legacySources: List<LegacyReadRecordSource> = emptyList(),
    val needsMerge: Boolean = false,
) {
    val isLegacy: Boolean get() = bookUrl.isNullOrBlank()
}

data class LegacyReadRecordSource(
    val deviceId: String,
    val bookName: String,
    val recordKey: String,
    val readTime: Long,
    val lastRead: Long,
)

data class ReadRecordTotalSummary(
    val totalReadTime: Long,
    val bookCount: Int,
    val items: List<ReadRecordDisplayItem>,
    val pendingLegacyItems: List<ReadRecordDisplayItem>,
)

data class ReadRecordBookStats(
    val totalReadTime: Long,
    val lastRead: Long?
)

object ReadRecordTotalModel {

    const val LEGACY_AUTHOR_PLACEHOLDER = "--"

    fun buildSummary(searchKey: String = ""): ReadRecordTotalSummary {
        val books = appDb.bookDao.all
        val booksByUrl = books.associateBy { it.bookUrl }
        val booksByName = books.groupBy { it.name }
        val mappings = appDb.readRecordLegacyMappingDao.all
            .associateBy { normalizedDeviceId(it.deviceId) to it.bookName }
        val urlRecords = appDb.readRecordDao.getUrlRecords()
        val legacyRecords = appDb.readRecordDao.getLegacyRecords()

        val urlItems = mutableMapOf<String, ReadRecordDisplayItem>()
        urlRecords
            .filter { it.readTime > 0L && it.bookUrl.isNotBlank() }
            .forEach { record ->
                val book = booksByUrl[record.bookUrl]
                addToUrlItems(
                    urlItems = urlItems,
                    bookUrl = record.bookUrl,
                    bookName = book?.name?.takeIf { it.isNotBlank() } ?: record.bookName,
                    bookAuthor = book?.author?.takeIf { it.isNotBlank() } ?: record.bookAuthor,
                    readTime = record.readTime,
                    lastRead = record.lastRead,
                    recordKey = record.recordKey
                )
            }

        val legacyItems = mutableMapOf<String, ReadRecordDisplayItem>()
        legacyRecords
            .filter { it.readTime > 0L }
            .forEach { record ->
                val legacySource = LegacyReadRecordSource(
                    deviceId = normalizedDeviceId(record.deviceId),
                    bookName = record.bookName,
                    recordKey = record.recordKey,
                    readTime = record.readTime,
                    lastRead = record.lastRead
                )
                val key = normalizedDeviceId(record.deviceId) to record.bookName
                val mappedBook = mappings[key]?.bookUrl?.let { booksByUrl[it] }
                if (mappedBook != null) {
                    addToUrlItems(
                        urlItems = urlItems,
                        bookUrl = mappedBook.bookUrl,
                        bookName = mappedBook.name,
                        bookAuthor = mappedBook.author,
                        readTime = record.readTime,
                        lastRead = record.lastRead,
                        recordKey = record.recordKey,
                        legacySource = legacySource
                    )
                    return@forEach
                }

                val candidates = booksByName[record.bookName].orEmpty()
                if (candidates.size == 1) {
                    val book = candidates.first()
                    appDb.readRecordLegacyMappingDao.insert(
                        ReadRecordLegacyMapping(
                            deviceId = legacySource.deviceId,
                            bookName = record.bookName,
                            bookUrl = book.bookUrl,
                            updateTime = System.currentTimeMillis()
                        )
                    )
                    addToUrlItems(
                        urlItems = urlItems,
                        bookUrl = book.bookUrl,
                        bookName = book.name,
                        bookAuthor = book.author,
                        readTime = record.readTime,
                        lastRead = record.lastRead,
                        recordKey = record.recordKey,
                        legacySource = legacySource
                    )
                } else {
                    val displayKey = ReadRecord.legacyRecordKey(record.bookName)
                    val current = legacyItems[displayKey]
                    legacyItems[displayKey] = if (current == null) {
                        ReadRecordDisplayItem(
                            displayKey = displayKey,
                            bookUrl = null,
                            bookName = record.bookName,
                            bookAuthor = LEGACY_AUTHOR_PLACEHOLDER,
                            readTime = record.readTime,
                            lastRead = record.lastRead,
                            recordKeys = listOf(record.recordKey),
                            legacySources = listOf(legacySource),
                            needsMerge = candidates.size > 1
                        )
                    } else {
                        current.copy(
                            readTime = current.readTime + record.readTime,
                            lastRead = maxOf(current.lastRead, record.lastRead),
                            recordKeys = current.recordKeys + record.recordKey,
                            legacySources = current.legacySources + legacySource,
                            needsMerge = current.needsMerge || candidates.size > 1
                        )
                    }
                }
            }

        val mergedItems = (urlItems.values + legacyItems.values)
            .filter { item ->
                searchKey.isBlank() ||
                    item.bookName.contains(searchKey, ignoreCase = true) ||
                    item.bookAuthor.contains(searchKey, ignoreCase = true)
            }

        val pendingLegacyItems = legacyItems.values.filter { it.needsMerge }
        val urlBookCount = urlItems.values.count { it.readTime > 0L }
        val unmappedLegacyBookCount = legacyItems.values.count { !it.needsMerge && it.readTime > 0L }

        return ReadRecordTotalSummary(
            totalReadTime = mergedItems.sumOf { it.readTime },
            bookCount = urlBookCount + unmappedLegacyBookCount,
            items = mergedItems,
            pendingLegacyItems = pendingLegacyItems
        )
    }

    fun getPendingLegacyItems(): List<ReadRecordDisplayItem> {
        return buildSummary().pendingLegacyItems
    }

    fun getCandidates(bookName: String): List<Book> {
        return appDb.bookDao.findByName(bookName)
    }

    fun mergeLegacyToBook(legacyItem: ReadRecordDisplayItem, book: Book) {
        appDb.runInTransaction {
            val sources = legacyItem.legacySources.ifEmpty {
                listOf(
                    LegacyReadRecordSource(
                        deviceId = AppConst.androidId,
                        bookName = legacyItem.bookName,
                        recordKey = ReadRecord.legacyRecordKey(legacyItem.bookName),
                        readTime = legacyItem.readTime,
                        lastRead = legacyItem.lastRead
                    )
                )
            }
            val updateTime = System.currentTimeMillis()
            sources.forEach { source ->
                appDb.readRecordLegacyMappingDao.insert(
                    ReadRecordLegacyMapping(
                            deviceId = normalizedDeviceId(source.deviceId),
                        bookName = source.bookName,
                        bookUrl = book.bookUrl,
                        updateTime = updateTime
                    )
                )
            }
        }
    }

    fun getBookStats(book: Book): ReadRecordBookStats {
        val booksByName = appDb.bookDao.findByName(book.name)
        val mappings = appDb.readRecordLegacyMappingDao.all
            .associateBy { normalizedDeviceId(it.deviceId) to it.bookName }
        val recordKey = ReadRecord.urlRecordKey(book.bookUrl)
        var totalReadTime = appDb.readRecordDao.getReadTimeByKey(recordKey) ?: 0L
        var lastRead = appDb.readRecordDao.getLastReadByKey(recordKey)
        appDb.readRecordDao.getLegacyRecords()
            .filter { it.readTime > 0L && it.bookName == book.name }
            .forEach { record ->
                val mappedUrl = mappings[normalizedDeviceId(record.deviceId) to record.bookName]?.bookUrl
                val belongsToBook = when {
                    mappedUrl != null -> mappedUrl == book.bookUrl
                    booksByName.size == 1 -> booksByName.first().bookUrl == book.bookUrl
                    else -> false
                }
                if (belongsToBook) {
                    totalReadTime += record.readTime
                    lastRead = maxOf(lastRead ?: 0L, record.lastRead).takeIf { it > 0L }
                }
            }
        return ReadRecordBookStats(totalReadTime, lastRead)
    }

    fun buildTopBooksInPeriod(startDate: String, endDate: String, limit: Int): List<ReadRecordTopBook> {
        val records = appDb.bookReadDayRecordDao.getRecordsInDateRange(startDate, endDate)
        val resolver = TopBookResolver(appDb.bookDao.all)
        val items = linkedMapOf<String, TopBookAccumulator>()
        records
            .filter { it.readTime > 0L }
            .forEach { record ->
                val resolvedBook = resolver.resolve(record)
                val key = resolvedBook?.bookUrl ?: "orphan:${record.bookUrl}"
                val current = items[key]
                if (current == null) {
                    items[key] = TopBookAccumulator(record, resolvedBook)
                } else {
                    current.readTime += record.readTime
                    current.record = current.record.copy(
                        bookName = current.record.bookName.ifBlank { record.bookName },
                        bookAuthor = current.record.bookAuthor.ifBlank { record.bookAuthor }
                    )
                }
            }
        return items.values
            .sortedByDescending { it.readTime }
            .take(limit)
            .map { it.toTopBook() }
    }

    fun getResolvedPeriodBookCount(startDate: String, endDate: String): Int {
        val records = appDb.bookReadDayRecordDao.getRecordsInDateRange(startDate, endDate)
        val resolver = TopBookResolver(appDb.bookDao.all)
        return records
            .asSequence()
            .filter { it.readTime > 0L }
            .map { record ->
                resolver.resolve(record)?.bookUrl ?: "orphan:${record.bookUrl}"
            }
            .toSet()
            .size
    }

    private fun addToUrlItems(
        urlItems: MutableMap<String, ReadRecordDisplayItem>,
        bookUrl: String,
        bookName: String,
        bookAuthor: String,
        readTime: Long,
        lastRead: Long,
        recordKey: String,
        legacySource: LegacyReadRecordSource? = null
    ) {
        val urlKey = ReadRecord.urlRecordKey(bookUrl)
        val current = urlItems[urlKey]
        val sourceList = legacySource?.let { listOf(it) }.orEmpty()
        urlItems[urlKey] = if (current == null) {
            ReadRecordDisplayItem(
                displayKey = urlKey,
                bookUrl = bookUrl,
                bookName = bookName,
                bookAuthor = bookAuthor,
                readTime = readTime,
                lastRead = lastRead,
                recordKeys = listOf(recordKey),
                legacySources = sourceList
            )
        } else {
            current.copy(
                readTime = current.readTime + readTime,
                lastRead = maxOf(current.lastRead, lastRead),
                recordKeys = current.recordKeys + recordKey,
                legacySources = current.legacySources + sourceList
            )
        }
    }

    private fun normalizedDeviceId(deviceId: String): String {
        return deviceId.ifBlank { AppConst.androidId }
    }

    private data class TopBookAccumulator(
        var record: BookReadDayRecord,
        val book: Book?,
        var readTime: Long = record.readTime
    ) {
        fun toTopBook(): ReadRecordTopBook {
            return ReadRecordTopBook(
                bookUrl = book?.bookUrl ?: record.bookUrl,
                bookName = book?.name?.takeIf { it.isNotBlank() } ?: record.bookName,
                author = book?.author?.takeIf { it.isNotBlank() }
                    ?: record.bookAuthor.ifBlank { LEGACY_AUTHOR_PLACEHOLDER },
                coverUrl = book?.coverUrl,
                customCoverUrl = book?.customCoverUrl,
                type = book?.type ?: 8,
                readTime = readTime
            )
        }
    }

    private class TopBookResolver(books: List<Book>) {
        private val booksByUrl = books.associateBy { it.bookUrl }
        private val booksByName = books.groupBy { it.name }

        fun resolve(record: BookReadDayRecord): Book? {
            return booksByUrl[record.bookUrl]
                ?: booksByName[record.bookName].orEmpty().singleOrNull()
        }
    }
}
