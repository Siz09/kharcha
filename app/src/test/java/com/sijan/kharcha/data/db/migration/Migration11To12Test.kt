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
class Migration11To12Test {

    @Test
    fun migrate11To12PreservesDataAndCreatesFestivalBudgetsTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
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
                            `isReconciled` INTEGER NOT NULL DEFAULT 0,
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

        // Insert baseline v11 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Travel', 'EXPENSE')")
        db.execSQL("INSERT INTO accounts (id, name, type, openingBalancePaisa, isActive, createdAt, updatedAt) VALUES (1, 'Cash', 'CASH', 500000, 1, 1000, 1000)")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, isReconciled, createdAt, updatedAt)
            VALUES (1, 150000, 'EXPENSE', 1, 1, 'Bus Ticket Home', 1761800000000, 'manual', 0, 1000, 1000)
        """.trimIndent())

        // Run migration 11 to 12
        MIGRATION_11_12.migrate(db)

        // Verify pre-existing entry preserved and festivalBudgetId is null
        val entryCursor = db.query("SELECT id, amountPaisa, festivalBudgetId FROM ledger_entries WHERE id = 1")
        assertTrue(entryCursor.moveToFirst())
        assertEquals(1L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("id")))
        assertEquals(150000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        assertTrue(entryCursor.isNull(entryCursor.getColumnIndexOrThrow("festivalBudgetId")))
        entryCursor.close()

        // Verify festival_budgets table created and writable
        db.execSQL("""
            INSERT INTO festival_budgets (name, festivalType, targetBudgetPaisa, startDateEpoch, endDateEpoch, notes, isArchived, createdAt, updatedAt)
            VALUES ('Dashain 2081', 'DASHAIN', 5000000, 1761800000000, 1762000000000, 'Family celebration budget', 0, 1000, 1000)
        """.trimIndent())

        val budgetCursor = db.query("SELECT name, festivalType, targetBudgetPaisa FROM festival_budgets WHERE id = 1")
        assertTrue(budgetCursor.moveToFirst())
        assertEquals("Dashain 2081", budgetCursor.getString(budgetCursor.getColumnIndexOrThrow("name")))
        assertEquals("DASHAIN", budgetCursor.getString(budgetCursor.getColumnIndexOrThrow("festivalType")))
        assertEquals(5000000L, budgetCursor.getLong(budgetCursor.getColumnIndexOrThrow("targetBudgetPaisa")))
        budgetCursor.close()

        db.close()
    }
}
