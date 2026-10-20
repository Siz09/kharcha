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
class Migration9To10Test {

    @Test
    fun migrate9To10PreservesDataAndCreatesSplitsAndReconciliationsTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
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

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `annual_archive_summaries` (
                            `year` INTEGER NOT NULL,
                            `calendarSystem` TEXT NOT NULL,
                            `totalIncomePaisa` INTEGER NOT NULL,
                            `totalExpensePaisa` INTEGER NOT NULL,
                            `netSavingsPaisa` INTEGER NOT NULL,
                            `categoryBreakdownJson` TEXT NOT NULL,
                            `monthlyTotalsJson` TEXT NOT NULL,
                            `entryCount` INTEGER NOT NULL,
                            `archivedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`year`, `calendarSystem`)
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = openHelper.writableDatabase

        // Insert baseline v9 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Supermarket', 'EXPENSE')")
        db.execSQL("INSERT INTO accounts (id, name, type, openingBalancePaisa, isActive, createdAt, updatedAt) VALUES (1, 'eSewa', 'WALLET', 500000, 1, 1000, 1000)")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, createdAt, updatedAt)
            VALUES (1, 250000, 'EXPENSE', 1, 1, 'Bhatbhateni Shopping', 1761800000000, 'manual', 1000, 1000)
        """.trimIndent())

        // Run migration 9 to 10
        MIGRATION_9_10.migrate(db)

        // Verify pre-existing entry preserved and isReconciled is 0
        val entryCursor = db.query("SELECT id, amountPaisa, isReconciled FROM ledger_entries WHERE id = 1")
        assertTrue(entryCursor.moveToFirst())
        assertEquals(1L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("id")))
        assertEquals(250000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals(0, entryCursor.getInt(entryCursor.getColumnIndexOrThrow("isReconciled")))
        entryCursor.close()

        // Verify inserting split entries
        db.execSQL("""
            INSERT INTO ledger_entry_splits (entryId, categoryId, amountPaisa, note)
            VALUES (1, 1, 150000, 'Groceries')
        """.trimIndent())
        db.execSQL("""
            INSERT INTO ledger_entry_splits (entryId, categoryId, amountPaisa, note)
            VALUES (1, 1, 100000, 'Kitchen Supplies')
        """.trimIndent())

        val splitCursor = db.query("SELECT SUM(amountPaisa) AS totalSplit FROM ledger_entry_splits WHERE entryId = 1")
        assertTrue(splitCursor.moveToFirst())
        assertEquals(250000L, splitCursor.getLong(splitCursor.getColumnIndexOrThrow("totalSplit")))
        splitCursor.close()

        // Verify account_reconciliations table
        db.execSQL("""
            INSERT INTO account_reconciliations (accountId, statementDate, statementBalancePaisa, computedBalancePaisa, differencePaisa, isReconciled, reconciledAt, notes)
            VALUES (1, 1761800000000, 500000, 500000, 0, 1, 1761805000000, 'Clean monthly balance check')
        """.trimIndent())

        val recCursor = db.query("SELECT accountId, statementBalancePaisa, differencePaisa, isReconciled FROM account_reconciliations WHERE accountId = 1")
        assertTrue(recCursor.moveToFirst())
        assertEquals(1L, recCursor.getLong(recCursor.getColumnIndexOrThrow("accountId")))
        assertEquals(500000L, recCursor.getLong(recCursor.getColumnIndexOrThrow("statementBalancePaisa")))
        assertEquals(0L, recCursor.getLong(recCursor.getColumnIndexOrThrow("differencePaisa")))
        assertEquals(1, recCursor.getInt(recCursor.getColumnIndexOrThrow("isReconciled")))
        recCursor.close()

        db.close()
    }
}
