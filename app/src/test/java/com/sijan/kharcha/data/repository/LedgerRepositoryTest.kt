package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LedgerRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(db.categoryDao(), db.ledgerDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun seedDefaultCategoriesPopulatesDefaultsIfEmpty() = runBlocking {
        repository.seedDefaultCategories()

        val allCategories = repository.getAllCategories().first()
        assertTrue(allCategories.isNotEmpty())
        assertTrue(allCategories.any { it.name == "Food & Dining" && it.type == EntryType.EXPENSE })
        assertTrue(allCategories.any { it.name == "Salary" && it.type == EntryType.INCOME })

        val initialCount = allCategories.size
        // Second call should not duplicate
        repository.seedDefaultCategories()
        val secondCount = repository.getAllCategories().first().size
        assertEquals(initialCount, secondCount)
    }

    @Test
    fun deleteCategoryFailsIfEntriesExist() = runBlocking {
        repository.seedDefaultCategories()
        val foodCategory = repository.getCategoriesByType(EntryType.EXPENSE).first().first { it.name == "Food & Dining" }

        // Save entry referencing this category
        val entry = LedgerEntry(
            amountPaisa = 50000L,
            type = EntryType.EXPENSE,
            categoryId = foodCategory.id,
            date = 1000L
        )
        val saveResult = repository.saveEntry(entry)
        assertTrue(saveResult.isSuccess)

        // Attempt deletion of category with entries
        val deleteResult = repository.deleteCategory(foodCategory.id)
        assertFalse(deleteResult.isSuccess)
        assertTrue(deleteResult.exceptionOrNull()?.message?.contains("in use") == true)
    }

    @Test
    fun checkDuplicateTransactionReturnsExisting() = runBlocking {
        repository.seedDefaultCategories()
        val cat = repository.getAllCategories().first().first()

        repository.saveEntry(
            LedgerEntry(
                amountPaisa = 20000L,
                type = EntryType.EXPENSE,
                categoryId = cat.id,
                date = 1000L,
                transactionId = "ESEWA_TXN_001"
            )
        )

        val duplicates = repository.checkDuplicateTransaction("ESEWA_TXN_001")
        assertEquals(1, duplicates.size)
        assertEquals("ESEWA_TXN_001", duplicates[0].transactionId)
    }
}
