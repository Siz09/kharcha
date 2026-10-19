package com.sijan.kharcha.ui.calendar

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
class CashFlowCalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var viewModel: CashFlowCalendarViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(db.categoryDao(), db.ledgerDao(), db.billDao())
        viewModel = CashFlowCalendarViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialStateLoadsCurrentMonthCorrectly() = runTest(testDispatcher) {
        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse(state.isLoading)
        assertTrue(state.daysInMonth in 28..31)
        assertTrue(state.selectedDay in 1..state.daysInMonth)
        assertTrue(state.selectedMonthLabel.isNotEmpty())
    }

    @Test
    fun entriesGroupedByDayWithDailyIncomeAndExpense() = runTest(testDispatcher) {
        val state = viewModel.uiState.first { !it.isLoading }
        val currentYear = state.selectedYear
        val currentMonth = state.selectedMonth

        // Create an expense entry on day 15 and an income entry on day 15
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(currentYear, currentMonth - 1, 15, 10, 0, 0)
        }
        val day15Millis = cal.timeInMillis

        // First need a category
        val catId = repository.insertCategory("General", EntryType.EXPENSE).getOrThrow()
        val incomeCatId = repository.insertCategory("IncomeCat", EntryType.INCOME).getOrThrow()

        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 150000L, // Rs. 1500
                type = EntryType.EXPENSE,
                categoryId = catId,
                date = day15Millis,
                note = "Electric bill"
            )
        )
        repository.saveEntry(
            LedgerEntry(
                id = 0,
                amountPaisa = 400000L, // Rs. 4000
                type = EntryType.INCOME,
                categoryId = incomeCatId,
                date = day15Millis,
                note = "Client payment"
            )
        )

        // Select day 15
        viewModel.selectDay(15)

        val updatedState = viewModel.uiState.first {
            it.dailyCashFlowMap[15]?.entries?.size == 2
        }

        val day15Flow = updatedState.dailyCashFlowMap[15]!!
        assertEquals(400000L, day15Flow.totalIncomePaisa)
        assertEquals(150000L, day15Flow.totalExpensePaisa)
        assertEquals(250000L, day15Flow.netPaisa)
        assertTrue(day15Flow.hasEntries)

        // Selected day details
        assertEquals(15, updatedState.selectedDay)
        assertEquals(2, updatedState.selectedDayCashFlow.entries.size)
    }

    @Test
    fun monthNavigationUpdatesMonthAndDays() = runTest(testDispatcher) {
        val state = viewModel.uiState.first { !it.isLoading }
        val initialMonth = state.selectedMonth

        viewModel.previousMonth()
        val prevMonthState = viewModel.uiState.first { it.selectedMonth != initialMonth }
        assertEquals(1, prevMonthState.selectedDay)

        viewModel.nextMonth()
        val nextMonthState = viewModel.uiState.first { it.selectedMonth == initialMonth }
        assertEquals(1, nextMonthState.selectedDay)
    }
}
