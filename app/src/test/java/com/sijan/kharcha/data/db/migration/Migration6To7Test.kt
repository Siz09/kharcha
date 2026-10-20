package com.sijan.kharcha.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration6To7Test {

    @Test
    fun migrate6To7PreservesDataAndCreatesUtilityAccountsAndTaxColumns() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
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

        // Insert baseline pre-migration data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Insurance', 'EXPENSE')")
        db.execSQL("INSERT INTO accounts (id, name, type, openingBalancePaisa, isActive, createdAt, updatedAt) VALUES (1, 'Cash', 'CASH', 0, 1, 1000, 1000)")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, createdAt, updatedAt)
            VALUES (1, 2500000, 'EXPENSE', 1, 1, 'Life Insurance Premium', 1761800000000, 'manual', 1000, 1000)
        """.trimIndent())

        // Run migration 6 to 7
        MIGRATION_6_7.migrate(db)

        // Verify pre-existing entry preserved with default tax columns
        val entryCursor = db.query("SELECT id, amountPaisa, isTaxDeductible, taxCategory FROM ledger_entries WHERE id = 1")
        assertTrue(entryCursor.moveToFirst())
        assertEquals(1L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("id")))
        assertEquals(2500000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals(0, entryCursor.getInt(entryCursor.getColumnIndexOrThrow("isTaxDeductible")))
        assertTrue(entryCursor.isNull(entryCursor.getColumnIndexOrThrow("taxCategory")))
        entryCursor.close()

        // Verify inserting new tax deductible entry works
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, isTaxDeductible, taxCategory, createdAt, updatedAt)
            VALUES (2, 4000000, 'EXPENSE', 1, 1, 'Nepal Life Premium', 1761800000000, 'manual', 1, 'LIFE_INSURANCE', 2000, 2000)
        """.trimIndent())

        val newEntryCursor = db.query("SELECT isTaxDeductible, taxCategory FROM ledger_entries WHERE id = 2")
        assertTrue(newEntryCursor.moveToFirst())
        assertEquals(1, newEntryCursor.getInt(newEntryCursor.getColumnIndexOrThrow("isTaxDeductible")))
        assertEquals("LIFE_INSURANCE", newEntryCursor.getString(newEntryCursor.getColumnIndexOrThrow("taxCategory")))
        newEntryCursor.close()

        // Verify utility_accounts table exists and supports CRUD
        db.execSQL("""
            INSERT INTO utility_accounts (id, utilityType, providerName, consumerId, counterOrBranch, accountHolderName, notes, createdAt, updatedAt)
            VALUES (1, 'NEA', 'Nepal Electricity Authority', '012.34.567', 'Ratnapark', 'Ram Sharma', 'Home Meter', 3000, 3000)
        """.trimIndent())

        val utilityCursor = db.query("SELECT * FROM utility_accounts WHERE id = 1")
        assertTrue(utilityCursor.moveToFirst())
        assertEquals("NEA", utilityCursor.getString(utilityCursor.getColumnIndexOrThrow("utilityType")))
        assertEquals("012.34.567", utilityCursor.getString(utilityCursor.getColumnIndexOrThrow("consumerId")))
        assertEquals("Ratnapark", utilityCursor.getString(utilityCursor.getColumnIndexOrThrow("counterOrBranch")))
        utilityCursor.close()

        db.close()
    }
}
