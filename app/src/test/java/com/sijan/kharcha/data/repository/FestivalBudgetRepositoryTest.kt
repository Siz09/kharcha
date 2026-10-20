package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.entity.AccountEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.AccountType
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.FestivalBudget
import com.sijan.kharcha.domain.model.FestivalType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FestivalBudgetRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FestivalBudgetRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        db.accountDao().insertAccount(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, openingBalancePaisa = 1000000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 1L, name = "Shopping", type = EntryType.EXPENSE)
        )

        repository = FestivalBudgetRepositoryImpl(
            festivalBudgetDao = db.festivalBudgetDao(),
            ledgerDao = db.ledgerDao()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getActiveFestivalBudgets_computesSpendingAndRemainingCorrectly() = runTest {
        // Target budget: Rs. 50,000 (5,000,000 paisa)
        val budgetId = repository.saveFestivalBudget(
            FestivalBudget(
                name = "Dashain 2081",
                festivalType = FestivalType.DASHAIN,
                targetBudgetPaisa = 5000000L,
                startDateEpoch = 1761800000000L,
                endDateEpoch = 1762200000000L
            )
        ).getOrThrow()

        // Insert entry linked to Dashain budget: Rs. 20,000 (2,000,000 paisa)
        db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 2000000L,
                type = EntryType.EXPENSE,
                categoryId = 1L,
                accountId = 1L,
                date = 1761850000000L,
                festivalBudgetId = budgetId,
                note = "Clothes for Puja"
            )
        )

        val activeBudgets = repository.getActiveFestivalBudgets().first()
        assertEquals(1, activeBudgets.size)

        val progress = activeBudgets[0]
        assertEquals("Dashain 2081", progress.budget.name)
        assertEquals(2000000L, progress.spentPaisa)
        assertEquals(3000000L, progress.remainingPaisa)
        assertEquals(40, progress.percentageUsed)
        assertFalse(progress.isOverBudget)
        assertEquals(1, progress.entryCount)
    }

    @Test
    fun tagEntryToFestival_linksExistingEntryToBudget() = runTest {
        val budgetId = repository.saveFestivalBudget(
            FestivalBudget(
                name = "Tihar 2081",
                festivalType = FestivalType.TIHAR,
                targetBudgetPaisa = 2000000L,
                startDateEpoch = 1761800000000L,
                endDateEpoch = 1762200000000L
            )
        ).getOrThrow()

        val entryId = db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 500000L,
                type = EntryType.EXPENSE,
                categoryId = 1L,
                accountId = 1L,
                date = 1761850000000L,
                note = "Sweets & Lights"
            )
        )

        val tagResult = repository.tagEntryToFestival(entryId, budgetId)
        assertTrue(tagResult.isSuccess)

        val updatedEntry = db.ledgerDao().getEntryById(entryId).first()
        assertEquals(budgetId, updatedEntry!!.festivalBudgetId)
    }
}
