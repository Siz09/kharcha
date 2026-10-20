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
class Migration10To11Test {

    @Test
    fun migrate10To11PreservesDataAndCreatesCategorizationRulesTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
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

        // Insert baseline v10 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Food', 'EXPENSE')")
        db.execSQL("INSERT INTO accounts (id, name, type, openingBalancePaisa, isActive, createdAt, updatedAt) VALUES (1, 'Cash', 'CASH', 100000, 1, 1000, 1000)")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, isReconciled, createdAt, updatedAt)
            VALUES (1, 50000, 'EXPENSE', 1, 1, 'Cafe Lunch', 1761800000000, 'manual', 1, 1000, 1000)
        """.trimIndent())

        // Run migration 10 to 11
        MIGRATION_10_11.migrate(db)

        // Verify pre-existing data preserved
        val entryCursor = db.query("SELECT id, amountPaisa, isReconciled FROM ledger_entries WHERE id = 1")
        assertTrue(entryCursor.moveToFirst())
        assertEquals(1L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("id")))
        assertEquals(50000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals(1, entryCursor.getInt(entryCursor.getColumnIndexOrThrow("isReconciled")))
        entryCursor.close()

        // Verify inserting categorization rule
        db.execSQL("""
            INSERT INTO categorization_rules (name, pattern, isRegex, categoryId, priority, isActive, createdAt, updatedAt)
            VALUES ('Bhatbhateni Rule', 'bhatbhateni', 0, 1, 10, 1, 1000, 1000)
        """.trimIndent())

        val ruleCursor = db.query("SELECT name, pattern, isRegex, categoryId, priority FROM categorization_rules WHERE id = 1")
        assertTrue(ruleCursor.moveToFirst())
        assertEquals("Bhatbhateni Rule", ruleCursor.getString(ruleCursor.getColumnIndexOrThrow("name")))
        assertEquals("bhatbhateni", ruleCursor.getString(ruleCursor.getColumnIndexOrThrow("pattern")))
        assertEquals(0, ruleCursor.getInt(ruleCursor.getColumnIndexOrThrow("isRegex")))
        assertEquals(1L, ruleCursor.getLong(ruleCursor.getColumnIndexOrThrow("categoryId")))
        assertEquals(10, ruleCursor.getInt(ruleCursor.getColumnIndexOrThrow("priority")))
        ruleCursor.close()

        db.close()
    }
}
