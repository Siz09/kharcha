package com.sijan.kharcha.ui.udhar

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.repository.UdharRepository
import com.sijan.kharcha.data.repository.UdharRepositoryImpl
import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharType
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UdharViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repository: UdharRepository
    private lateinit var viewModel: UdharViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UdharRepositoryImpl(db, db.udharDao(), db.auditLogDao())
        viewModel = UdharViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun creatingUdharReflectsInUiState() = runTest(testDispatcher) {
        viewModel.onAddClicked()
        val stateWithDialog = viewModel.uiState.first { it.showAddDialog }
        assertTrue(stateWithDialog.showAddDialog)

        viewModel.onCreateUdhar(
            name = "Suresh",
            phone = "9841000000",
            type = UdharType.LENT,
            amountPaisa = 250000L,
            dueDate = null,
            notes = "Bike petrol"
        )

        val updatedState = viewModel.uiState.first { it.items.isNotEmpty() }
        assertFalse(updatedState.showAddDialog)
        val items = updatedState.items
        assertEquals(1, items.size)
        assertEquals("Suresh", items[0].personName)
        assertEquals(UdharType.LENT, items[0].type)
        assertEquals(250000L, items[0].amountPaisa)
    }

    @Test
    fun tabFilterCorrectlyFiltersItems() = runTest(testDispatcher) {
        repository.createUdhar("Prakash", null, UdharType.LENT, 100000L, null, "")
        repository.createUdhar("Deepa", null, UdharType.BORROWED, 50000L, null, "")

        viewModel.onTabFilterChanged(UdharTabFilter.ALL)
        val allState = viewModel.uiState.first { it.items.size == 2 }
        assertEquals(2, allState.items.size)

        viewModel.onTabFilterChanged(UdharTabFilter.LENT)
        val lentState = viewModel.uiState.first { it.tabFilter == UdharTabFilter.LENT && it.items.size == 1 }
        assertEquals(1, lentState.items.size)
        assertEquals("Prakash", lentState.items[0].personName)

        viewModel.onTabFilterChanged(UdharTabFilter.BORROWED)
        val borrowedState = viewModel.uiState.first { it.tabFilter == UdharTabFilter.BORROWED && it.items.size == 1 }
        assertEquals(1, borrowedState.items.size)
        assertEquals("Deepa", borrowedState.items[0].personName)
    }

    @Test
    fun recordingSettlementUpdatesBalanceAndClosesDialog() = runTest(testDispatcher) {
        val id = repository.createUdhar("Anil", null, UdharType.LENT, 100000L, null, "").getOrThrow()
        val initial = viewModel.uiState.first { it.items.any { item -> item.id == id } }

        val udhar = initial.items.first { it.id == id }
        viewModel.onSettleClicked(udhar)
        val dialogState = viewModel.uiState.first { it.selectedUdharForSettlement?.id == id }
        assertNotNull(dialogState.selectedUdharForSettlement)

        viewModel.onConfirmSettlement(
            amountPaisa = 100000L,
            date = 1000L,
            notes = "Full return"
        )

        val settledState = viewModel.uiState.first {
            it.items.any { item -> item.id == id && item.status == UdharStatus.SETTLED }
        }
        assertNull(settledState.selectedUdharForSettlement)
        val updated = settledState.items.first { it.id == id }
        assertEquals(UdharStatus.SETTLED, updated.status)
        assertEquals(0L, updated.remainingPaisa)
    }

    @Test
    fun deletingUdharRemovesItFromState() = runTest(testDispatcher) {
        val id = repository.createUdhar("Madan", null, UdharType.BORROWED, 80000L, null, "").getOrThrow()
        val initial = viewModel.uiState.first { it.items.any { item -> item.id == id } }

        val udhar = initial.items.first { it.id == id }
        viewModel.onDeleteUdharClicked(udhar)
        val dialogState = viewModel.uiState.first { it.udharToDelete?.id == id }
        assertNotNull(dialogState.udharToDelete)

        viewModel.onConfirmDeleteUdhar()

        val deletedState = viewModel.uiState.first { it.items.none { item -> item.id == id } }
        assertNull(deletedState.udharToDelete)
        assertTrue(deletedState.items.none { it.id == id })
    }
}
