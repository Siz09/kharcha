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
class Migration5To6Test {

    @Test
    fun migrate5To6PreservesDataAndCreatesSavingsGoalsTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(5) {
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
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `udhars` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `personName` TEXT NOT NULL,
                            `phoneNumber` TEXT,
                            `type` TEXT NOT NULL,
                            `amountPaisa` INTEGER NOT NULL,
                            `settledPaisa` INTEGER NOT NULL,
                            `dueDate` INTEGER,
                            `notes` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 5 && newVersion == 6) {
                        MIGRATION_5_6.migrate(db)
                    }
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Seed v5 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Food', 'EXPENSE')")
        db.execSQL("INSERT INTO accounts (id, name, type, openingBalancePaisa, isActive, createdAt, updatedAt) VALUES (1, 'Cash in Hand', 'CASH', 0, 1, 1000, 1000)")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, note, date, source, createdAt, updatedAt)
            VALUES (1, 15000, 'EXPENSE', 1, 1, 'Lunch', 1000, 'manual', 1000, 1000)
        """.trimIndent())
        db.execSQL("""
            INSERT INTO udhars (id, personName, phoneNumber, type, amountPaisa, settledPaisa, dueDate, notes, status, createdAt, updatedAt)
            VALUES (1, 'Bikram Shah', '9812345678', 'LENT', 300000, 0, 1800000000000, 'Lent for groceries', 'PENDING', 2000, 2000)
        """.trimIndent())

        // Run migration 5 -> 6
        MIGRATION_5_6.migrate(db)

        // Verify v5 data preserved
        val udharCursor = db.query("SELECT * FROM udhars WHERE id = 1")
        assertNotNull(udharCursor)
        assertTrue(udharCursor.moveToFirst())
        assertEquals("Bikram Shah", udharCursor.getString(udharCursor.getColumnIndexOrThrow("personName")))
        assertEquals(300000L, udharCursor.getLong(udharCursor.getColumnIndexOrThrow("amountPaisa")))
        udharCursor.close()

        // Insert savings goal
        db.execSQL("""
            INSERT INTO savings_goals (id, name, targetAmountPaisa, savedAmountPaisa, targetDateEpochMillis, notes, isCompleted, createdAt, updatedAt)
            VALUES (1, 'Dashain Trip', 5000000, 1000000, 1800000000000, 'Family trip to Pokhara', 0, 2500, 2500)
        """.trimIndent())

        val goalCursor = db.query("SELECT * FROM savings_goals WHERE id = 1")
        assertNotNull(goalCursor)
        assertTrue(goalCursor.moveToFirst())
        assertEquals("Dashain Trip", goalCursor.getString(goalCursor.getColumnIndexOrThrow("name")))
        assertEquals(5000000L, goalCursor.getLong(goalCursor.getColumnIndexOrThrow("targetAmountPaisa")))
        assertEquals(1000000L, goalCursor.getLong(goalCursor.getColumnIndexOrThrow("savedAmountPaisa")))
        assertEquals("Family trip to Pokhara", goalCursor.getString(goalCursor.getColumnIndexOrThrow("notes")))
        assertEquals(0, goalCursor.getInt(goalCursor.getColumnIndexOrThrow("isCompleted")))
        goalCursor.close()

        // Insert contribution
        db.execSQL("""
            INSERT INTO savings_contributions (id, goalId, amountPaisa, dateEpochMillis, notes, createdAt)
            VALUES (1, 1, 1000000, 3000, 'Initial savings deposit', 3000)
        """.trimIndent())

        val contributionCursor = db.query("SELECT * FROM savings_contributions WHERE id = 1")
        assertNotNull(contributionCursor)
        assertTrue(contributionCursor.moveToFirst())
        assertEquals(1L, contributionCursor.getLong(contributionCursor.getColumnIndexOrThrow("goalId")))
        assertEquals(1000000L, contributionCursor.getLong(contributionCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals("Initial savings deposit", contributionCursor.getString(contributionCursor.getColumnIndexOrThrow("notes")))
        contributionCursor.close()

        db.close()
        helper.close()
    }
}
