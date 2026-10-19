package com.sijan.kharcha.ui.entries

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.data.repository.LedgerRepositoryImpl
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class AddEditEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(db.categoryDao(), db.ledgerDao())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialStateLoadsExpenseCategories() = runTest(testDispatcher) {
        val viewModel = AddEditEntryViewModel(repository, SavedStateHandle())
        val state = viewModel.uiState.first { it.categories.isNotEmpty() }

        assertFalse(state.isEditMode)
        assertEquals(EntryType.EXPENSE, state.type)
        assertNotNull(state.selectedCategoryId)
        assertFalse(state.isSaveEnabled) // blank amount
    }

    @Test
    fun amountValidationBlocksInvalidAndZeroAmounts() = runTest(testDispatcher) {
        val viewModel = AddEditEntryViewModel(repository, SavedStateHandle())
        val state = viewModel.uiState.first { it.categories.isNotEmpty() }
        val catId = state.selectedCategoryId!!

        viewModel.onAmountChanged("0")
        assertEquals(ValidationStatus.ERROR, viewModel.uiState.value.amountStatus)
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onAmountChanged("-50")
        assertEquals(ValidationStatus.ERROR, viewModel.uiState.value.amountStatus)
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onAmountChanged("abc")
        assertEquals(ValidationStatus.ERROR, viewModel.uiState.value.amountStatus)
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onAmountChanged("150.25")
        assertEquals(ValidationStatus.VALID, viewModel.uiState.value.amountStatus)
        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun dateValidationRejectsFutureDates() = runTest(testDispatcher) {
        val viewModel = AddEditEntryViewModel(repository, SavedStateHandle())
        val state = viewModel.uiState.first { it.categories.isNotEmpty() }
        viewModel.onAmountChanged("200")

        val future = System.currentTimeMillis() + 86400000L * 2
        viewModel.onDateChanged(future)
        assertEquals(ValidationStatus.ERROR, viewModel.uiState.value.dateStatus)
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        val past = System.currentTimeMillis() - 3600000L
        viewModel.onDateChanged(past)
        assertEquals(ValidationStatus.VALID, viewModel.uiState.value.dateStatus)
        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun saveNewEntryPersistsCorrectPaisaAndEmitsSuccess() = runTest(testDispatcher) {
        val viewModel = AddEditEntryViewModel(repository, SavedStateHandle())
        val state = viewModel.uiState.first { it.categories.isNotEmpty() }
        val categoryId = state.selectedCategoryId!!

        viewModel.onAmountChanged("450.50")
        viewModel.onCategorySelected(categoryId)
        viewModel.onNoteChanged("Test dinner")
        viewModel.onTransactionIdChanged("TXN123")

        val eventDeferred = async { viewModel.events.first() }
        viewModel.saveEntry()
        val event = eventDeferred.await()

        assertEquals(AddEditEntryEvent.SaveSuccess, event)
        val entries = repository.getAllEntries().first()
        assertEquals(1, entries.size)
        assertEquals(45050L, entries[0].amountPaisa)
        assertEquals("Test dinner", entries[0].note)
        assertEquals("TXN123", entries[0].transactionId)
        assertEquals("manual", entries[0].source)
    }

    @Test
    fun loadAndEditExistingEntry() = runTest(testDispatcher) {
        repository.seedDefaultCategories()
        val catId = repository.getCategoriesByType(EntryType.EXPENSE).first().first().id
        val initialEntry = LedgerEntry(
            amountPaisa = 10000L,
            type = EntryType.EXPENSE,
            categoryId = catId,
            note = "Old note",
            date = 1700000000000L,
            transactionId = "OLD_TXN"
        )
        val entryId = repository.saveEntry(initialEntry).getOrThrow()

        val handle = SavedStateHandle(mapOf("entryId" to entryId))
        val viewModel = AddEditEntryViewModel(repository, handle)
        val state = viewModel.uiState.first { it.amountText.isNotEmpty() }

        assertTrue(state.isEditMode)
        assertEquals("100", state.amountText)
        assertEquals("Old note", state.note)
        assertEquals("OLD_TXN", state.transactionId)

        // Modify amount and note
        viewModel.onAmountChanged("200.75")
        viewModel.onNoteChanged("Updated note")

        val eventDeferred = async { viewModel.events.first() }
        viewModel.saveEntry()
        val event = eventDeferred.await()

        assertEquals(AddEditEntryEvent.SaveSuccess, event)
        val updated = repository.getEntryById(entryId).first()
        assertNotNull(updated)
        assertEquals(20075L, updated!!.amountPaisa)
        assertEquals("Updated note", updated.note)
    }

    @Test
    fun deleteExistingEntryDeletesFromRepository() = runTest(testDispatcher) {
        repository.seedDefaultCategories()
        val catId = repository.getCategoriesByType(EntryType.EXPENSE).first().first().id
        val entry = LedgerEntry(
            amountPaisa = 8000L,
            type = EntryType.EXPENSE,
            categoryId = catId,
            date = System.currentTimeMillis()
        )
        val entryId = repository.saveEntry(entry).getOrThrow()

        val handle = SavedStateHandle(mapOf("entryId" to entryId))
        val viewModel = AddEditEntryViewModel(repository, handle)
        viewModel.uiState.first { it.amountText.isNotEmpty() }

        viewModel.onDeleteClicked()
        assertTrue(viewModel.uiState.value.showDeleteConfirmDialog)

        val eventDeferred = async { viewModel.events.first() }
        viewModel.onDeleteConfirmed()
        val event = eventDeferred.await()

        assertEquals(AddEditEntryEvent.DeleteSuccess, event)
        val deleted = repository.getEntryById(entryId).first()
        assertNull(deleted)
    }
}
