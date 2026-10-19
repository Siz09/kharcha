package com.sijan.kharcha.ui.entries

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.data.repository.LedgerRepositoryImpl
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EntriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var viewModel: EntriesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(
            db.categoryDao(),
            db.ledgerDao(),
            db.billDao(),
            db.budgetDao(),
            db.auditLogDao(),
            db.recurringEntryDao()
        )
        viewModel = EntriesViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialStateLoadsAllEntriesAndCategories() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.first { it.categories.isNotEmpty() }
        assertFalse(initialState.isLoading)
        assertTrue(initialState.entries.isEmpty())
        assertEquals("", initialState.searchQuery)
        assertEquals(null, initialState.selectedType)
    }

    @Test
    fun filterByTypeReturnsOnlyMatchingEntries() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.first { it.categories.isNotEmpty() }
        val expenseCatId = initialState.categories.first { it.type == EntryType.EXPENSE }.id
        val incomeCatId = initialState.categories.first { it.type == EntryType.INCOME }.id

        val now = System.currentTimeMillis()
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 10000L,
                type = EntryType.EXPENSE,
                categoryId = expenseCatId,
                note = "Lunch at cafe",
                date = now
            )
        )
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 50000L,
                type = EntryType.INCOME,
                categoryId = incomeCatId,
                note = "Freelance bonus",
                date = now
            )
        )

        val loadedState = viewModel.uiState.first { it.entries.size == 2 }
        assertEquals(2, loadedState.entries.size)

        // Filter by Expense
        viewModel.onTypeFilterChange(EntryType.EXPENSE)
        val expenseState = viewModel.uiState.first {
            it.selectedType == EntryType.EXPENSE && it.entries.size == 1 && it.entries.first().type == EntryType.EXPENSE
        }
        assertEquals(1, expenseState.entries.size)
        assertEquals(EntryType.EXPENSE, expenseState.entries.first().type)

        // Filter by Income
        viewModel.onTypeFilterChange(EntryType.INCOME)
        val incomeState = viewModel.uiState.first {
            it.selectedType == EntryType.INCOME && it.entries.size == 1 && it.entries.first().type == EntryType.INCOME
        }
        assertEquals(1, incomeState.entries.size)
        assertEquals(EntryType.INCOME, incomeState.entries.first().type)

        // Clear filter
        viewModel.onTypeFilterChange(null)
        val allState = viewModel.uiState.first { it.selectedType == null && it.entries.size == 2 }
        assertEquals(2, allState.entries.size)
    }

    @Test
    fun searchReturnsMatchingNotes() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.first { it.categories.isNotEmpty() }
        val catId = initialState.categories.first().id

        val now = System.currentTimeMillis()
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 12000L,
                type = EntryType.EXPENSE,
                categoryId = catId,
                note = "Bhatbhateni milk and eggs",
                date = now
            )
        )
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 34000L,
                type = EntryType.EXPENSE,
                categoryId = catId,
                note = "Pet shop cat food",
                date = now
            )
        )

        viewModel.uiState.first { it.entries.size == 2 }

        viewModel.onSearchQueryChange("Bhatbhateni")
        val searchState = viewModel.uiState.first { it.searchQuery == "Bhatbhateni" && it.entries.size == 1 }
        assertEquals(1, searchState.entries.size)
        assertTrue(searchState.entries.first().note.contains("Bhatbhateni"))
    }
}
