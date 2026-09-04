package io.legado.app.model

import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookReadSession
import io.legado.app.data.entities.ReadRecord

object BookReadRecordMigrationModel {

    fun migrateOnBookSourceChanged(oldBook: Book?, newBook: Book?) {
        if (oldBook == null || newBook == null) return
        if (oldBook.bookUrl.isBlank() || newBook.bookUrl.isBlank()) return
        if (oldBook.bookUrl == newBook.bookUrl) return
        appDb.runInTransaction {
            migrateReviewMeta(oldBook, newBook)
            migrateReadRecord(oldBook, newBook)
            updateLegacyMappings(oldBook, newBook)
            migrateSessionsAndRebuildDayRecords(oldBook, newBook)
        }
    }

    private fun migrateReviewMeta(oldBook: Book, newBook: Book) {
        if (oldBook.ratingUpdateTime > newBook.ratingUpdateTime) {
            newBook.rating = oldBook.rating
            newBook.ratingUpdateTime = oldBook.ratingUpdateTime
        }
        if (oldBook.reviewUpdateTime > newBook.reviewUpdateTime) {
            newBook.review = oldBook.review
            newBook.reviewCreateTime = oldBook.reviewCreateTime
            newBook.reviewUpdateTime = oldBook.reviewUpdateTime
        }
    }

    private fun migrateReadRecord(oldBook: Book, newBook: Book) {
        val oldKey = ReadRecord.urlRecordKey(oldBook.bookUrl)
        val newKey = ReadRecord.urlRecordKey(newBook.bookUrl)
        val oldRecords = appDb.readRecordDao.getByRecordKey(oldKey)
        oldRecords.forEach { oldRecord ->
            val newRecord = appDb.readRecordDao.getByKey(oldRecord.deviceId, newKey)
            if (newRecord == null) {
                appDb.readRecordDao.insert(
                    oldRecord.copy(
                        recordKey = newKey,
                        bookUrl = newBook.bookUrl,
                        bookName = newBook.name,
                        bookAuthor = newBook.author
                    )
                )
            } else {
                newRecord.bookUrl = newBook.bookUrl
                newRecord.bookName = newBook.name
                newRecord.bookAuthor = newBook.author
                newRecord.readTime += oldRecord.readTime
                newRecord.lastRead = maxOf(newRecord.lastRead, oldRecord.lastRead)
                appDb.readRecordDao.insert(newRecord)
            }
        }
        if (oldRecords.isNotEmpty()) {
            appDb.readRecordDao.deleteByKey(oldKey)
        }
    }

    private fun updateLegacyMappings(oldBook: Book, newBook: Book) {
        appDb.readRecordLegacyMappingDao.updateBookUrl(
            oldBookUrl = oldBook.bookUrl,
            newBookUrl = newBook.bookUrl,
            updateTime = System.currentTimeMillis()
        )
    }

    private fun migrateSessionsAndRebuildDayRecords(oldBook: Book, newBook: Book) {
        val oldUrl = oldBook.bookUrl
        val newUrl = newBook.bookUrl
        val affectedDates = linkedSetOf<String>()
        affectedDates.addAll(appDb.bookReadSessionDao.getDatesByBook(oldUrl))
        affectedDates.addAll(appDb.bookReadSessionDao.getDatesByBook(newUrl))
        affectedDates.addAll(appDb.bookReadDayRecordDao.getDatesByBook(oldUrl))
        affectedDates.addAll(appDb.bookReadDayRecordDao.getDatesByBook(newUrl))

        val sessions = appDb.bookReadSessionDao.getByBook(oldUrl)
        sessions.forEach { session ->
            appDb.bookReadSessionDao.insert(session.toNewBookSession(newBook))
        }
        appDb.bookReadSessionDao.deleteByBook(oldUrl)
        appDb.bookReadDayRecordDao.deleteByBook(oldUrl)
        affectedDates.forEach { date ->
            BookReadDayRecordModel.rebuildDayRecord(newUrl, date)
        }
    }

    private fun BookReadSession.toNewBookSession(newBook: Book): BookReadSession {
        return copy(
            id = 0L,
            bookUrl = newBook.bookUrl,
            bookName = newBook.name,
            bookAuthor = newBook.author
        )
    }

}
