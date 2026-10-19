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
class Migration2To3Test {

    @Test
    fun migrate2To3PreservesDataAndCreatesBudgetsAuditAndRecurringTables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `categories` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `bills` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `amountPaisa` INTEGER NOT NULL,
                            `categoryId` INTEGER NOT NULL,
                            `dueDayOfMonth` INTEGER NOT NULL,
                            `frequency` TEXT NOT NULL,
                            `isAutoReminder` INTEGER NOT NULL,
                            `lastPaidDate` INTEGER,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 2 && newVersion == 3) {
                        MIGRATION_2_3.migrate(db)
                    }
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Seed v2 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Utilities', 'EXPENSE')")
        db.execSQL("""
            INSERT INTO bills (id, name, amountPaisa, categoryId, dueDayOfMonth, frequency, isAutoReminder, lastPaidDate, createdAt, updatedAt)
            VALUES (1, 'WiFi', 150000, 1, 15, 'MONTHLY', 1, NULL, 1000, 1000)
        """.trimIndent())

        // Execute migration 2 -> 3
        MIGRATION_2_3.migrate(db)

        // Validate old data is preserved
        val billCursor = db.query("SELECT * FROM bills WHERE id = 1")
        assertNotNull(billCursor)
        assertTrue(billCursor.moveToFirst())
        assertEquals("WiFi", billCursor.getString(billCursor.getColumnIndexOrThrow("name")))
        assertEquals(150000L, billCursor.getLong(billCursor.getColumnIndexOrThrow("amountPaisa")))
        billCursor.close()

        // Validate budgets table
        db.execSQL("""
            INSERT INTO budgets (categoryId, monthYear, limitPaisa, createdAt, updatedAt)
            VALUES (1, '2026-10', 500000, 1000, 1000)
        """.trimIndent())
        val budgetCursor = db.query("SELECT * FROM budgets WHERE categoryId = 1 AND monthYear = '2026-10'")
        assertNotNull(budgetCursor)
        assertTrue(budgetCursor.moveToFirst())
        assertEquals(500000L, budgetCursor.getLong(budgetCursor.getColumnIndexOrThrow("limitPaisa")))
        budgetCursor.close()

        // Validate audit_logs table
        db.execSQL("""
            INSERT INTO audit_logs (entityType, entityId, action, details, timestamp)
            VALUES ('BUDGET', 1, 'INSERT', 'Created budget', 1000)
        """.trimIndent())
        val auditCursor = db.query("SELECT * FROM audit_logs WHERE entityType = 'BUDGET'")
        assertNotNull(auditCursor)
        assertTrue(auditCursor.moveToFirst())
        assertEquals("Created budget", auditCursor.getString(auditCursor.getColumnIndexOrThrow("details")))
        auditCursor.close()

        // Validate recurring_entries table
        db.execSQL("""
            INSERT INTO recurring_entries (amountPaisa, type, categoryId, note, frequency, nextDueDate, isActive, createdAt, updatedAt)
            VALUES (200000, 'EXPENSE', 1, 'Rent', 'MONTHLY', 2000, 1, 1000, 1000)
        """.trimIndent())
        val recurringCursor = db.query("SELECT * FROM recurring_entries WHERE note = 'Rent'")
        assertNotNull(recurringCursor)
        assertTrue(recurringCursor.moveToFirst())
        assertEquals(200000L, recurringCursor.getLong(recurringCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals("MONTHLY", recurringCursor.getString(recurringCursor.getColumnIndexOrThrow("frequency")))
        recurringCursor.close()

        db.close()
        helper.close()
    }
}
