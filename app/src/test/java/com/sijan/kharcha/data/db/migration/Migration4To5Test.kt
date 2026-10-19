package com.sijan.kharcha.data.db.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration4To5Test {

    @Test
    fun migrate4To5PreservesDataAndCreatesUdharTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
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

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 4 && newVersion == 5) {
                        MIGRATION_4_5.migrate(db)
                    }
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Seed v4 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Food', 'EXPENSE')")
        db.execSQL("INSERT INTO accounts (id, name, type, openingBalancePaisa, isActive, createdAt, updatedAt) VALUES (1, 'Cash in Hand', 'CASH', 0, 1, 1000, 1000)")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, createdAt, updatedAt)
            VALUES (1, 15000, 'EXPENSE', 1, 1, 'Lunch', 1000, 'manual', 1000, 1000)
        """.trimIndent())

        // Run migration 4 -> 5
        MIGRATION_4_5.migrate(db)

        // Verify v4 data preserved
        val entryCursor = db.query("SELECT * FROM ledger_entries WHERE id = 1")
        assertNotNull(entryCursor)
        assertTrue(entryCursor.moveToFirst())
        assertEquals(15000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        entryCursor.close()

        // Insert udhar
        db.execSQL("""
            INSERT INTO udhars (id, personName, phoneNumber, type, amountPaisa, settledPaisa, dueDate, notes, status, createdAt, updatedAt)
            VALUES (1, 'Aayush Shrestha', '9841234567', 'LENT', 500000, 0, 1800000000000, 'Lent for phone repair', 'PENDING', 2000, 2000)
        """.trimIndent())

        val udharCursor = db.query("SELECT * FROM udhars WHERE id = 1")
        assertNotNull(udharCursor)
        assertTrue(udharCursor.moveToFirst())
        assertEquals("Aayush Shrestha", udharCursor.getString(udharCursor.getColumnIndexOrThrow("personName")))
        assertEquals("9841234567", udharCursor.getString(udharCursor.getColumnIndexOrThrow("phoneNumber")))
        assertEquals("LENT", udharCursor.getString(udharCursor.getColumnIndexOrThrow("type")))
        assertEquals(500000L, udharCursor.getLong(udharCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals(0L, udharCursor.getLong(udharCursor.getColumnIndexOrThrow("settledPaisa")))
        assertEquals("PENDING", udharCursor.getString(udharCursor.getColumnIndexOrThrow("status")))
        udharCursor.close()

        // Insert settlement
        db.execSQL("""
            INSERT INTO udhar_settlements (id, udharId, amountPaisa, dateEpochMillis, notes, createdAt)
            VALUES (1, 1, 200000, 3000, 'Partial cash return', 3000)
        """.trimIndent())

        val settlementCursor = db.query("SELECT * FROM udhar_settlements WHERE id = 1")
        assertNotNull(settlementCursor)
        assertTrue(settlementCursor.moveToFirst())
        assertEquals(1L, settlementCursor.getLong(settlementCursor.getColumnIndexOrThrow("udharId")))
        assertEquals(200000L, settlementCursor.getLong(settlementCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals("Partial cash return", settlementCursor.getString(settlementCursor.getColumnIndexOrThrow("notes")))
        settlementCursor.close()

        db.close()
        helper.close()
    }
}
