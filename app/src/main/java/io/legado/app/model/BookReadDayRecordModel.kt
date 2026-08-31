package io.legado.app.model

import io.legado.app.constant.AppConst
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReadRecord
import io.legado.app.data.entities.BookReadDayRecord
import io.legado.app.data.entities.BookReadSession
import io.legado.app.help.config.AppConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object BookReadDayRecordModel {

    const val TYPE_TEXT = "text"
    const val TYPE_MANGA = "manga"
    private const val LOCAL_SESSION_MERGE_INTERVAL = 60_000L

    data class DailyDuration(
        val date: String,
        val duration: Long,
        val segmentStartTime: Long,
        val segmentEndTime: Long
    )

    data class MergedReadDay(
        val date: String,
        val readTime: Long,
        val startTime: Long,
        val lastRead: Long
    )

    fun commitReadDuration(
        book: Book,
        readRecord: ReadRecord,
        startTime: Long,
        endTime: Long,
        type: String = TYPE_TEXT
    ): Boolean {
        if (!AppConfig.enableReadRecord || book.bookUrl.isBlank() || endTime <= startTime) {
            return false
        }
        val duration = endTime - startTime
        readRecord.deviceId = AppConst.androidId
        readRecord.recordKey = ReadRecord.urlRecordKey(book.bookUrl)
        readRecord.bookUrl = book.bookUrl
        readRecord.bookName = book.name
        readRecord.bookAuthor = book.author
        readRecord.readTime += duration
        readRecord.lastRead = endTime
        appDb.readRecordDao.insert(readRecord)
        val affectedDates = linkedSetOf<String>()
        splitDurationByLocalDate(startTime, endTime).forEach { segment ->
            insertOrMergeLocalSession(
                BookReadSession(
                    deviceId = AppConst.androidId,
                    bookUrl = book.bookUrl,
                    bookName = book.name,
                    bookAuthor = book.author,
                    type = type,
                    date = segment.date,
                    startTime = segment.segmentStartTime,
                    endTime = segment.segmentEndTime,
                    duration = segment.duration
                )
            )
            affectedDates.add(segment.date)
        }
        rebuildDayRecords(book.bookUrl, affectedDates)
        return true
    }

    fun restoreSessions(sessions: List<BookReadSession>) {
        val affectedDays = linkedSetOf<Pair<String, String>>()
        sessions.forEach { session ->
            if (session.bookUrl.isBlank() || session.date.isBlank() || session.endTime <= session.startTime) {
                return@forEach
            }
            session.id = 0L
            session.duration = session.endTime - session.startTime
            val insertedId = appDb.bookReadSessionDao.insert(session)
            if (insertedId != -1L) {
                affectedDays.add(session.bookUrl to session.date)
            }
        }
        affectedDays.forEach { (bookUrl, date) ->
            rebuildDayRecord(bookUrl, date)
        }
    }

    fun splitDurationByLocalDate(
        startTime: Long,
        endTime: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyDuration> {
        if (endTime <= startTime) return emptyList()
        val segments = arrayListOf<DailyDuration>()
        var segmentStart = startTime
        while (segmentStart < endTime) {
            val date = Instant.ofEpochMilli(segmentStart).atZone(zoneId).toLocalDate()
            val nextMidnight = date.plusDays(1).startMillis(zoneId)
            val segmentEnd = minOf(endTime, nextMidnight)
            if (segmentEnd > segmentStart) {
                segments.add(
                    DailyDuration(
                        date = date.toString(),
                        duration = segmentEnd - segmentStart,
                        segmentStartTime = segmentStart,
                        segmentEndTime = segmentEnd
                    )
                )
            }
            segmentStart = segmentEnd
        }
        return segments
    }

    private fun insertOrMergeLocalSession(session: BookReadSession) {
        val last = appDb.bookReadSessionDao.getLastLocalSession(
            deviceId = session.deviceId,
            bookUrl = session.bookUrl,
            type = session.type,
            date = session.date
        )
        if (last != null && session.startTime >= last.endTime &&
            session.startTime - last.endTime < LOCAL_SESSION_MERGE_INTERVAL
        ) {
            last.bookName = session.bookName
            last.bookAuthor = session.bookAuthor
            last.endTime = maxOf(last.endTime, session.endTime)
            last.duration = last.endTime - last.startTime
            appDb.bookReadSessionDao.update(last)
        } else {
            appDb.bookReadSessionDao.insert(session)
        }
    }

    fun rebuildDayRecords(bookUrl: String, dates: Collection<String>) {
        dates.forEach { date ->
            rebuildDayRecord(bookUrl, date)
        }
    }

    fun rebuildDayRecord(bookUrl: String, date: String) {
        val sessions = appDb.bookReadSessionDao.getByBookDate(bookUrl, date)
        val mergedReadDay = mergeSessionsForDay(date, sessions)
        if (mergedReadDay == null) {
            appDb.bookReadDayRecordDao.deleteByBookDate(bookUrl, date)
            return
        }
        val firstSession = sessions.firstOrNull()
        appDb.bookReadDayRecordDao.insert(
            BookReadDayRecord(
                bookUrl = bookUrl,
                bookName = firstSession?.bookName.orEmpty(),
                bookAuthor = firstSession?.bookAuthor.orEmpty(),
                date = date,
                readTime = mergedReadDay.readTime,
                startTime = mergedReadDay.startTime,
                lastRead = mergedReadDay.lastRead
            )
        )
    }

    fun mergeSessionsForDay(
        date: String,
        sessions: List<BookReadSession>
    ): MergedReadDay? {
        val sortedSessions = sessions
            .filter { it.date == date && it.duration > 0L && it.endTime > it.startTime }
            .sortedWith(compareBy<BookReadSession> { it.startTime }.thenBy { it.endTime })
        if (sortedSessions.isEmpty()) {
            return null
        }
        val mergedRanges = arrayListOf<Pair<Long, Long>>()
        var rangeStart = sortedSessions.first().startTime
        var rangeEnd = sortedSessions.first().endTime
        sortedSessions.drop(1).forEach { session ->
            if (session.startTime <= rangeEnd) {
                rangeEnd = maxOf(rangeEnd, session.endTime)
            } else {
                mergedRanges.add(rangeStart to rangeEnd)
                rangeStart = session.startTime
                rangeEnd = session.endTime
            }
        }
        mergedRanges.add(rangeStart to rangeEnd)
        return MergedReadDay(
            date = date,
            readTime = mergedRanges.sumOf { (start, end) -> end - start },
            startTime = mergedRanges.minOf { it.first },
            lastRead = mergedRanges.maxOf { it.second }
        )
    }

    private fun LocalDate.startMillis(zoneId: ZoneId): Long {
        return atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
