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
class Migration3To4Test {

    @Test
    fun migrate3To4PreservesDataCreatesAccountsAndAddsAccountColumnsToLedger() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `categories` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL
                        )
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
                    if (oldVersion == 3 && newVersion == 4) {
                        MIGRATION_3_4.migrate(db)
                    }
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Seed v3 data
        db.execSQL("INSERT INTO categories (id, name, type) VALUES (1, 'Food', 'EXPENSE')")
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, note, date, source, createdAt, updatedAt)
            VALUES (1, 25000, 'EXPENSE', 1, 'Momo lunch', 1000, 'manual', 1000, 1000)
        """.trimIndent())

        // Execute migration 3 -> 4
        MIGRATION_3_4.migrate(db)

        // Validate accounts table and default Cash account
        val accountCursor = db.query("SELECT * FROM accounts WHERE id = 1")
        assertNotNull(accountCursor)
        assertTrue(accountCursor.moveToFirst())
        assertEquals("Cash in Hand", accountCursor.getString(accountCursor.getColumnIndexOrThrow("name")))
        assertEquals("CASH", accountCursor.getString(accountCursor.getColumnIndexOrThrow("type")))
        assertEquals(0L, accountCursor.getLong(accountCursor.getColumnIndexOrThrow("openingBalancePaisa")))
        assertEquals(1, accountCursor.getInt(accountCursor.getColumnIndexOrThrow("isActive")))
        accountCursor.close()

        // Validate existing ledger entry was preserved and defaulted to accountId = 1
        val entryCursor = db.query("SELECT * FROM ledger_entries WHERE id = 1")
        assertNotNull(entryCursor)
        assertTrue(entryCursor.moveToFirst())
        assertEquals(25000L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals("Momo lunch", entryCursor.getString(entryCursor.getColumnIndexOrThrow("note")))
        assertEquals(1L, entryCursor.getLong(entryCursor.getColumnIndexOrThrow("accountId")))
        assertTrue(entryCursor.isNull(entryCursor.getColumnIndexOrThrow("toAccountId")))
        entryCursor.close()

        // Validate new account insertion
        db.execSQL("""
            INSERT INTO accounts (id, name, type, institutionName, accountNumberLast4, openingBalancePaisa, isActive, createdAt, updatedAt)
            VALUES (2, 'eSewa Wallet', 'WALLET', 'eSewa', '9841', 500000, 1, 2000, 2000)
        """.trimIndent())

        // Validate new ledger entry with accountId and toAccountId
        db.execSQL("""
            INSERT INTO ledger_entries (id, amountPaisa, type, categoryId, accountId, toAccountId, note, date, source, createdAt, updatedAt)
            VALUES (2, 50000, 'TRANSFER', 1, 1, 2, 'Load eSewa', 2000, 'transfer', 2000, 2000)
        """.trimIndent())

        val transferCursor = db.query("SELECT * FROM ledger_entries WHERE id = 2")
        assertNotNull(transferCursor)
        assertTrue(transferCursor.moveToFirst())
        assertEquals(50000L, transferCursor.getLong(transferCursor.getColumnIndexOrThrow("amountPaisa")))
        assertEquals(1L, transferCursor.getLong(transferCursor.getColumnIndexOrThrow("accountId")))
        assertEquals(2L, transferCursor.getLong(transferCursor.getColumnIndexOrThrow("toAccountId")))
        transferCursor.close()

        db.close()
        helper.close()
    }
}
