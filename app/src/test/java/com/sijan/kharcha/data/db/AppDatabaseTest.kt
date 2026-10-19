package com.sijan.kharcha.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var ledgerDao: LedgerDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = db.categoryDao()
        ledgerDao = db.ledgerDao()
        runBlocking {
            db.accountDao().insertAccount(
                com.sijan.kharcha.data.db.entity.AccountEntity(
                    id = 1L,
                    name = "Cash in Hand",
                    type = com.sijan.kharcha.domain.model.AccountType.CASH,
                    openingBalancePaisa = 0L
                )
            )
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveCategory() = runBlocking {
        val category = CategoryEntity(name = "Food", type = EntryType.EXPENSE)
        val id = categoryDao.insertCategory(category)
        assertTrue(id > 0)

        val categories = categoryDao.getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Food", categories[0].name)
        assertEquals(EntryType.EXPENSE, categories[0].type)
    }

    @Test
    fun insertAndRetrieveLedgerEntry() = runBlocking {
        val categoryId = categoryDao.insertCategory(CategoryEntity(name = "Groceries", type = EntryType.EXPENSE))

        val entry = LedgerEntryEntity(
            amountPaisa = 150000L, // Rs. 1500.00
            type = EntryType.EXPENSE,
            categoryId = categoryId,
            note = "Bhatbhateni supermarket",
            date = 1718000000000L,
            source = "receipt",
            transactionId = "TXN_998877"
        )
        val entryId = ledgerDao.insertEntry(entry)
        assertTrue(entryId > 0)

        val retrieved = ledgerDao.getEntryById(entryId).first()
        assertNotNull(retrieved)
        assertEquals(150000L, retrieved?.amountPaisa)
        assertEquals("Bhatbhateni supermarket", retrieved?.note)
        assertEquals("TXN_998877", retrieved?.transactionId)

        // Verify duplicate check by transaction ID
        val duplicates = ledgerDao.findByTransactionId("TXN_998877")
        assertEquals(1, duplicates.size)
    }

    @Test
    fun fullTextSearchReturnsMatchingNotes() = runBlocking {
        val catId = categoryDao.insertCategory(CategoryEntity(name = "General", type = EntryType.EXPENSE))

        ledgerDao.insertEntry(
            LedgerEntryEntity(amountPaisa = 10000L, type = EntryType.EXPENSE, categoryId = catId, note = "Dinner with friends at cafe", date = 1000L)
        )
        ledgerDao.insertEntry(
            LedgerEntryEntity(amountPaisa = 20000L, type = EntryType.EXPENSE, categoryId = catId, note = "Taxi ride to office", date = 2000L)
        )

        val searchResults = ledgerDao.searchEntries("cafe").first()
        assertEquals(1, searchResults.size)
        assertTrue(searchResults[0].note.contains("cafe"))
    }
}
