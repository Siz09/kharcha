package com.sijan.kharcha.ui.bills

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.bills.BillReminderScheduler
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.data.repository.LedgerRepositoryImpl
import com.sijan.kharcha.domain.model.Bill
import com.sijan.kharcha.domain.model.EntryType
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
class BillsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var scheduler: BillReminderScheduler
    private lateinit var viewModel: BillsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(db.categoryDao(), db.ledgerDao(), db.billDao())
        scheduler = BillReminderScheduler(context)
        viewModel = BillsViewModel(repository, scheduler)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialStateLoadsBillsAndRemainingPaisa() = runTest(testDispatcher) {
        val catId = repository.insertCategory("Utilities", EntryType.EXPENSE).getOrThrow()

        repository.saveBill(
            Bill(
                name = "Electricity",
                amountPaisa = 120000L,
                categoryId = catId,
                dueDayOfMonth = 15
            )
        )
        repository.saveBill(
            Bill(
                name = "Water",
                amountPaisa = 80000L,
                categoryId = catId,
                dueDayOfMonth = 20
            )
        )

        val state = viewModel.uiState.first { it.bills.size == 2 }
        assertFalse(state.isLoading)
        assertEquals(2, state.bills.size)
        assertEquals(200000L, state.totalMonthlyBillsPaisa)
        assertEquals(200000L, state.remainingUnpaidPaisa)
    }

    @Test
    fun payBillCreatesLedgerEntryAndDecreasesRemainingPaisa() = runTest(testDispatcher) {
        val catId = repository.insertCategory("Subscriptions", EntryType.EXPENSE).getOrThrow()

        val billId = repository.saveBill(
            Bill(
                name = "Spotify",
                amountPaisa = 50000L,
                categoryId = catId,
                dueDayOfMonth = 10
            )
        ).getOrThrow()

        val initialState = viewModel.uiState.first { it.bills.isNotEmpty() }
        assertEquals(50000L, initialState.remainingUnpaidPaisa)
        assertFalse(initialState.bills.first().isPaidThisMonth)

        viewModel.payBill(billId) { result ->
            assertTrue(result.isSuccess)
        }

        val updatedState = viewModel.uiState.first { it.bills.first().isPaidThisMonth }
        assertTrue(updatedState.bills.first().isPaidThisMonth)
        assertEquals(0L, updatedState.remainingUnpaidPaisa)

        // Verify ledger entry created
        val entries = repository.getAllEntries().first()
        assertEquals(1, entries.size)
        assertEquals(50000L, entries.first().amountPaisa)
        assertEquals("bill_payment", entries.first().source)
    }
}
