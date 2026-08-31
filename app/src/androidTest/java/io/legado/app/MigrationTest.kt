package io.legado.app

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.legado.app.data.AppDatabase
import io.legado.app.data.DatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    private val ALL_MIGRATIONS = arrayOf<Migration>(
        DatabaseMigrations.migration_89_90,
        DatabaseMigrations.migration_90_91
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(TEST_DB, 50).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS)
            .build().apply {
                openHelper.writableDatabase
                close()
            }
    }

    @Test
    @Throws(IOException::class)
    fun migrate89To90_createBookReadDayRecords() {
        helper.createDatabase(TEST_DB, 89).apply {
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            90,
            true,
            DatabaseMigrations.migration_89_90
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate90To91_createBookReadSessionsAndRebuildDayRecordPrimaryKey() {
        helper.createDatabase(TEST_DB, 90).apply {
            execSQL(
                """
                    INSERT INTO `bookReadDayRecords` (
                        `deviceId`, `bookUrl`, `bookName`, `bookAuthor`, `date`,
                        `readTime`, `startTime`, `lastRead`
                    ) VALUES (
                        'device-a', 'book-url', 'book', 'author', '2026-08-03',
                        60000, 1000, 61000
                    )
                """.trimIndent()
            )
            execSQL(
                """
                    INSERT INTO `bookReadDayRecords` (
                        `deviceId`, `bookUrl`, `bookName`, `bookAuthor`, `date`,
                        `readTime`, `startTime`, `lastRead`
                    ) VALUES (
                        'device-b', 'book-url', 'book', 'author', '2026-08-03',
                        30000, 2000, 90000
                    )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            91,
            true,
            DatabaseMigrations.migration_90_91
        ).apply {
            query("select count(1) from bookReadSessions").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
            query("select readTime, startTime, lastRead from bookReadDayRecords where bookUrl = 'book-url' and date = '2026-08-03'").use {
                it.moveToFirst()
                assertEquals(60000L, it.getLong(0))
                assertEquals(1000L, it.getLong(1))
                assertEquals(90000L, it.getLong(2))
            }
            close()
        }
    }
}
