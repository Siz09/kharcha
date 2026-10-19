package com.sijan.kharcha.ui.home

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import com.sijan.kharcha.data.preferences.UserPreferencesRepositoryImpl
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.data.repository.LedgerRepositoryImpl
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.FinancialHealthStatus
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
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var preferencesRepo: UserPreferencesRepository
    private lateinit var viewModel: HomeViewModel

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
        preferencesRepo = UserPreferencesRepositoryImpl(context)
        viewModel = HomeViewModel(repository, preferencesRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialStateLoadsDefaultCategoriesAndSummary() = runTest(testDispatcher) {
        val state = viewModel.uiState.first { it.categoriesMap.isNotEmpty() }
        assertFalse(state.isLoading)
        assertTrue("Categories should be seeded", state.categoriesMap.isNotEmpty())
        assertEquals(0L, state.monthlySummary.totalIncomePaisa)
        assertEquals(0L, state.monthlySummary.totalExpensePaisa)
    }

    @Test
    fun entryAddedUpdatesSummaryAndRecentEntries() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.first { it.categoriesMap.isNotEmpty() }
        val categoryId = initialState.categoriesMap.values.first { it.type == EntryType.EXPENSE }.id

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = cal.timeInMillis

        val entry = LedgerEntry(
            id = 0,
            amountPaisa = 50000L, // Rs. 500.00
            type = EntryType.EXPENSE,
            categoryId = categoryId,
            date = now,
            note = "Bhatbhateni groceries",
            createdAt = now,
            updatedAt = now
        )

        repository.saveEntry(entry)

        val updatedState = viewModel.uiState.first { it.recentEntries.isNotEmpty() }
        assertEquals(1, updatedState.recentEntries.size)
        assertEquals(50000L, updatedState.monthlySummary.totalExpensePaisa)
        assertEquals("Bhatbhateni groceries", updatedState.recentEntries.first().note)
    }

    @Test
    fun savingsRateAndBurnRateCalculatedCorrectly() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.first { it.categoriesMap.isNotEmpty() }
        val expenseCatId = initialState.categoriesMap.values.first { it.type == EntryType.EXPENSE }.id
        val incomeCatId = initialState.categoriesMap.values.first { it.type == EntryType.INCOME }.id

        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis

        // Add 100,000 paisa income (Rs 1,000) and 40,000 paisa expense (Rs 400) -> 60% savings rate -> Healthy (>30%)
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 100000L,
                type = EntryType.INCOME,
                categoryId = incomeCatId,
                date = now,
                note = "Freelance"
            )
        )
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 40000L,
                type = EntryType.EXPENSE,
                categoryId = expenseCatId,
                date = now,
                note = "Groceries"
            )
        )

        val updatedState = viewModel.uiState.first { it.recentEntries.size == 2 }
        assertEquals(60, updatedState.savingsRatePercent)
        assertEquals(FinancialHealthStatus.HEALTHY, updatedState.financialHealth)
        assertTrue(updatedState.dailyBurnRatePaisa > 0L)
    }

    @Test
    fun selectMonthUpdatesActiveMonthAndFiltersEntries() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.first { it.categoriesMap.isNotEmpty() }
        assertTrue(initialState.isCurrentMonth)

        // Select a past month (e.g., year 2024, month 5)
        viewModel.selectMonth(2024, 5)

        val pastState = viewModel.uiState.first { it.selectedYear == 2024 && it.selectedMonth == 5 }
        assertFalse(pastState.isCurrentMonth)
        assertEquals("May 2024", pastState.selectedMonthLabel)
        assertEquals(0, pastState.recentEntries.size)
        assertEquals(0L, pastState.monthlySummary.totalExpensePaisa)

        // Reset back to current month
        viewModel.resetToCurrentMonth()
        val resetState = viewModel.uiState.first { it.isCurrentMonth }
        assertTrue(resetState.isCurrentMonth)
    }

    @Test
    fun rolloverBalanceCarriesOverWhenEnabled() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.first { it.categoriesMap.isNotEmpty() }
        val incomeCatId = initialState.categoriesMap.values.first { it.type == EntryType.INCOME }.id

        // Create an entry in January 2024 (income 500,000 paisa)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2024, Calendar.JANUARY, 15, 12, 0, 0)
        }
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 500000L,
                type = EntryType.INCOME,
                categoryId = incomeCatId,
                date = cal.timeInMillis,
                note = "January salary"
            )
        )

        // Enable rollover
        preferencesRepo.setRolloverEnabled(true)

        // View February 2024
        viewModel.selectMonth(2024, 2)

        val febState = viewModel.uiState.first { it.selectedYear == 2024 && it.selectedMonth == 2 && it.rolloverEnabled }
        assertTrue(febState.rolloverEnabled)
        assertEquals(500000L, febState.openingBalancePaisa)
    }

    @Test
    fun categoryExpensesBreakdownCalculatedForExpenseEntries() = runTest(testDispatcher) {
        val initialState = viewModel.uiState.first { it.categoriesMap.isNotEmpty() }
        val foodCat = initialState.categoriesMap.values.first { it.type == EntryType.EXPENSE }

        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 150000L,
                type = EntryType.EXPENSE,
                categoryId = foodCat.id,
                date = now,
                note = "Groceries"
            )
        )

        val updatedState = viewModel.uiState.first { it.categoryExpenses.isNotEmpty() }
        assertEquals(1, updatedState.categoryExpenses.size)
        assertEquals(foodCat.name, updatedState.categoryExpenses.first().categoryName)
        assertEquals(150000L, updatedState.categoryExpenses.first().amountPaisa)
        assertEquals(100f, updatedState.categoryExpenses.first().percentage)
    }
}
