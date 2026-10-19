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
class Migration1To2Test {

    @Test
    fun migrate1To2PreservesDataAndCreatesBillsTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `categories` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name_type` ON `categories` (`name`, `type`)
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `ledger_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `amountPaisa` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `categoryId` INTEGER NOT NULL,
                            `note` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `source` TEXT NOT NULL,
                            `receiptPath` TEXT,
                            `rawOcrText` TEXT,
                            `transactionId` TEXT,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL,
                            FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 1 && newVersion == 2) {
                        MIGRATION_1_2.migrate(db)
                    }
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Insert version 1 test data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Utilities', 'EXPENSE')")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, note, date, source, createdAt, updatedAt)
            VALUES (1, 150000, 'EXPENSE', 1, 'Internet Bill', 1700000000000, 'MANUAL', 1700000000000, 1700000000000)
        """)

        // Execute migration 1 -> 2
        MIGRATION_1_2.migrate(db)

        // Validate old categories data is preserved
        val catCursor = db.query("SELECT * FROM categories WHERE id = 1")
        assertNotNull(catCursor)
        assertTrue(catCursor.moveToFirst())
        assertEquals("Utilities", catCursor.getString(catCursor.getColumnIndexOrThrow("name")))
        catCursor.close()

        // Validate old ledger_entries data is preserved
        val entryCursor = db.query("SELECT * FROM ledger_entries WHERE id = 1")
        assertNotNull(entryCursor)
        assertTrue(entryCursor.moveToFirst())
        assertEquals(150000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        entryCursor.close()

        // Validate new bills table can accept inserts and enforce schema
        db.execSQL("""
            INSERT INTO bills (name, amountPaisa, categoryId, dueDayOfMonth, frequency, isAutoReminder, lastPaidDate, createdAt, updatedAt)
            VALUES ('Electricity', 250000, 1, 10, 'MONTHLY', 1, NULL, 1000, 1000)
        """)

        val billCursor = db.query("SELECT * FROM bills WHERE name = 'Electricity'")
        assertNotNull(billCursor)
        assertTrue(billCursor.moveToFirst())
        assertEquals(250000L, billCursor.getLong(billCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals(10, billCursor.getInt(billCursor.getColumnIndexOrThrow("dueDayOfMonth")))
        assertEquals("MONTHLY", billCursor.getString(billCursor.getColumnIndexOrThrow("frequency")))
        assertEquals(1, billCursor.getInt(billCursor.getColumnIndexOrThrow("isAutoReminder")))
        billCursor.close()

        // Validate index exists on bills (categoryId)
        val indexCursor = db.query("PRAGMA index_list('bills')")
        assertNotNull(indexCursor)
        var foundIndex = false
        while (indexCursor.moveToNext()) {
            val indexName = indexCursor.getString(indexCursor.getColumnIndexOrThrow("name"))
            if (indexName == "index_bills_categoryId") {
                foundIndex = true
                break
            }
        }
        indexCursor.close()
        assertTrue("Index index_bills_categoryId must exist on bills table", foundIndex)

        db.close()
        helper.close()
    }
}
