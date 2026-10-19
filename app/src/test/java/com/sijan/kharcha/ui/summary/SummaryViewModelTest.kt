package com.sijan.kharcha.ui.summary

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
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var viewModel: SummaryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(db.categoryDao(), db.ledgerDao())
        viewModel = SummaryViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialStateCalculatesMonthlyTotals() = runTest(testDispatcher) {
        repository.seedDefaultCategories()
        val categories = repository.getAllCategories().first()
        val expenseCat = categories.first { it.type == EntryType.EXPENSE }
        val incomeCat = categories.first { it.type == EntryType.INCOME }

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.DAY_OF_MONTH, 10)
        val now = cal.timeInMillis

        repository.saveEntry(
            LedgerEntry(
                amountPaisa = 100000L,
                type = EntryType.EXPENSE,
                categoryId = expenseCat.id,
                date = now
            )
        )
        repository.saveEntry(
            LedgerEntry(
                amountPaisa = 250000L,
                type = EntryType.INCOME,
                categoryId = incomeCat.id,
                date = now
            )
        )

        val state = viewModel.uiState.first { !it.isLoading && it.entryCount == 2 }

        assertEquals(250000L, state.totalIncomePaisa)
        assertEquals(100000L, state.totalExpensePaisa)
        assertEquals(150000L, state.balancePaisa)
        assertEquals(1, state.categorySpending.size)
        assertEquals(expenseCat.id, state.categorySpending[0].category.id)
        assertEquals(100000L, state.categorySpending[0].amountPaisa)
        assertEquals(1.0f, state.categorySpending[0].percentage, 0.001f)
    }

    @Test
    fun navigationChangesSelectedMonth() = runTest(testDispatcher) {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)

        val initialState = viewModel.uiState.first()
        assertEquals(currentMonth, initialState.month)
        assertEquals(currentYear, initialState.year)

        val expectedNextMonth = if (currentMonth == 12) 1 else currentMonth + 1
        val expectedNextYear = if (currentMonth == 12) currentYear + 1 else currentYear

        viewModel.nextMonth()
        testScheduler.advanceUntilIdle()
        val nextMonthState = viewModel.uiState.first { it.month == expectedNextMonth }
        assertEquals(expectedNextMonth, nextMonthState.month)
        assertEquals(expectedNextYear, nextMonthState.year)

        viewModel.previousMonth()
        testScheduler.advanceUntilIdle()
        val backState = viewModel.uiState.first { it.month == currentMonth }
        assertEquals(currentMonth, backState.month)
        assertEquals(currentYear, backState.year)
    }

    @Test
    fun emptyMonthShowsZeroTotals() = runTest(testDispatcher) {
        repository.seedDefaultCategories()

        // Jump to next year (no entries)
        repeat(12) { viewModel.nextMonth() }

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(0L, state.totalIncomePaisa)
        assertEquals(0L, state.totalExpensePaisa)
        assertEquals(0L, state.balancePaisa)
        assertEquals(0, state.entryCount)
        assertTrue(state.categorySpending.isEmpty())
    }
}
