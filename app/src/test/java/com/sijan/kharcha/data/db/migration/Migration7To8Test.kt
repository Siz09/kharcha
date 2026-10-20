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
class Migration7To8Test {

    @Test
    fun migrate7To8PreservesDataAndAddsDocumentTypeColumn() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(7) {
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

        // Insert baseline v7 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Utilities', 'EXPENSE')")
        db.execSQL("INSERT INTO accounts (id, name, type, openingBalancePaisa, isActive, createdAt, updatedAt) VALUES (1, 'Cash', 'CASH', 0, 1, 1000, 1000)")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, isTaxDeductible, taxCategory, createdAt, updatedAt)
            VALUES (1, 150000, 'EXPENSE', 1, 1, 'Electricity Bill', 1761800000000, 'manual', 0, NULL, 1000, 1000)
        """.trimIndent())

        // Run migration 7 to 8
        MIGRATION_7_8.migrate(db)

        // Verify pre-existing entry preserved and documentType is NULL
        val entryCursor = db.query("SELECT id, amountPaisa, documentType FROM ledger_entries WHERE id = 1")
        assertTrue(entryCursor.moveToFirst())
        assertEquals(1L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("id")))
        assertEquals(150000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        assertTrue(entryCursor.isNull(entryCursor.getColumnIndexOrThrow("documentType")))
        entryCursor.close()

        // Verify inserting new entries with documentType works
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, receiptPath, documentType, createdAt, updatedAt)
            VALUES (2, 250000, 'EXPENSE', 1, 1, 'Bank Statement PDF', 1761800000000, 'share', '/path/to/statement.pdf', 'PDF', 2000, 2000)
        """.trimIndent())

        val pdfCursor = db.query("SELECT id, documentType, receiptPath FROM ledger_entries WHERE id = 2")
        assertTrue(pdfCursor.moveToFirst())
        assertEquals("PDF", pdfCursor.getString(pdfCursor.getColumnIndexOrThrow("documentType")))
        assertEquals("/path/to/statement.pdf", pdfCursor.getString(pdfCursor.getColumnIndexOrThrow("receiptPath")))
        pdfCursor.close()
    }
}
