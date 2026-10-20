package com.sijan.kharcha.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration8To9Test {

    @Test
    fun migrate8To9PreservesDataAndCreatesAnnualArchiveSummariesTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `categories` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `accounts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `institutionName` TEXT,
                            `accountNumberLast4` TEXT,
                            `openingBalancePaisa` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `ledger_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `amountPaisa` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `categoryId` INTEGER NOT NULL,
                            `accountId` INTEGER NOT NULL DEFAULT 1,
                            `toAccountId` INTEGER,
                            `note` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `source` TEXT NOT NULL,
                            `receiptPath` TEXT,
                            `rawOcrText` TEXT,
                            `transactionId` TEXT,
                            `isTaxDeductible` INTEGER NOT NULL DEFAULT 0,
                            `taxCategory` TEXT DEFAULT NULL,
                            `documentType` TEXT DEFAULT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                            FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                            FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = openHelper.writableDatabase

        // Insert baseline v8 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Groceries', 'EXPENSE')")
        db.execSQL("INSERT INTO accounts (id, name, type, openingBalancePaisa, isActive, createdAt, updatedAt) VALUES (1, 'Cash', 'CASH', 0, 1, 1000, 1000)")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, documentType, createdAt, updatedAt)
            VALUES (1, 120000, 'EXPENSE', 1, 1, 'Vegetables', 1761800000000, 'manual', 'IMAGE', 1000, 1000)
        """.trimIndent())

        // Run migration 8 to 9
        MIGRATION_8_9.migrate(db)

        // Verify pre-existing data
        val entryCursor = db.query("SELECT id, amountPaisa, documentType FROM ledger_entries WHERE id = 1")
        assertTrue(entryCursor.moveToFirst())
        assertEquals(1L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("id")))
        assertEquals(120000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals("IMAGE", entryCursor.getString(entryCursor.getColumnIndexOrThrow("documentType")))
        entryCursor.close()

        // Verify annual_archive_summaries table created and writable
        db.execSQL("""
            INSERT INTO annual_archive_summaries (year, calendarSystem, totalIncomePaisa, totalExpensePaisa, netSavingsPaisa, categoryBreakdownJson, monthlyTotalsJson, entryCount, archivedAt)
            VALUES (2080, 'BS', 50000000, 35000000, 15000000, '{}', '{}', 142, 1761800000000)
        """.trimIndent())

        val summaryCursor = db.query("SELECT year, calendarSystem, netSavingsPaisa, entryCount FROM annual_archive_summaries WHERE year = 2080 AND calendarSystem = 'BS'")
        assertTrue(summaryCursor.moveToFirst())
        assertEquals(2080, summaryCursor.getInt(summaryCursor.getColumnIndexOrThrow("year")))
        assertEquals("BS", summaryCursor.getString(summaryCursor.getColumnIndexOrThrow("calendarSystem")))
        assertEquals(15000000L, summaryCursor.getLong(summaryCursor.getColumnIndexOrThrow("netSavingsPaisa")))
        assertEquals(142, summaryCursor.getInt(summaryCursor.getColumnIndexOrThrow("entryCount")))
        summaryCursor.close()

        db.close()
    }
}
