package com.sijan.kharcha.ui.settings.categories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.R
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CategoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var viewModel: CategoriesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(db.categoryDao(), db.ledgerDao())
        viewModel = CategoriesViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialCategoriesLoadAndTabSwitching() = runTest(testDispatcher) {
        repository.seedDefaultCategories()

        val state = viewModel.uiState.first { !it.isLoading && it.expenseCategories.isNotEmpty() }
        assertEquals(EntryType.EXPENSE, state.selectedTab)
        assertTrue(state.currentCategories.all { it.type == EntryType.EXPENSE })

        viewModel.selectTab(EntryType.INCOME)
        val incomeState = viewModel.uiState.first { it.selectedTab == EntryType.INCOME }
        assertTrue(incomeState.currentCategories.all { it.type == EntryType.INCOME })
    }

    @Test
    fun addAndRenameCategory() = runTest(testDispatcher) {
        repository.seedDefaultCategories()

        viewModel.selectTab(EntryType.EXPENSE)
        viewModel.addCategory("Subscriptions")

        val stateWithNew = viewModel.uiState.first { it.expenseCategories.any { c -> c.name == "Subscriptions" } }
        val newCat = stateWithNew.expenseCategories.first { it.name == "Subscriptions" }
        assertNotNull(newCat)

        viewModel.renameCategory(newCat.id, "Digital Subscriptions")
        val stateWithRenamed = viewModel.uiState.first { it.expenseCategories.any { c -> c.name == "Digital Subscriptions" } }
        assertTrue(stateWithRenamed.expenseCategories.any { it.name == "Digital Subscriptions" })
        assertFalse(stateWithRenamed.expenseCategories.any { it.name == "Subscriptions" })
    }

    @Test
    fun deleteCategoryInUseFailsWithErrorMessage() = runTest(testDispatcher) {
        repository.seedDefaultCategories()

        val foodCat = repository.getAllCategories().first().first { it.name == "Food & Dining" }

        repository.saveEntry(
            LedgerEntry(
                amountPaisa = 15000L,
                type = EntryType.EXPENSE,
                categoryId = foodCat.id,
                date = System.currentTimeMillis()
            )
        )

        var capturedError: Int? = null
        val job = viewModel.deleteCategory(foodCat.id) { errorResId ->
            capturedError = errorResId
        }
        job.join()
        testScheduler.advanceUntilIdle()

        assertEquals(R.string.categories_error_in_use, capturedError)
        // Ensure category still exists
        val currentCats = repository.getAllCategories().first()
        assertTrue(currentCats.any { it.id == foodCat.id })
    }

    @Test
    fun deleteUnusedCategorySucceeds() = runTest(testDispatcher) {
        repository.seedDefaultCategories()

        val insertResult = repository.insertCategory("Temporary", EntryType.EXPENSE)
        val tempId = insertResult.getOrThrow()

        var capturedError: Int? = null
        val job = viewModel.deleteCategory(tempId) { errorResId ->
            capturedError = errorResId
        }
        job.join()
        testScheduler.advanceUntilIdle()

        assertEquals(null, capturedError)
        val currentCats = repository.getAllCategories().first()
        assertFalse(currentCats.any { it.id == tempId })
    }
}
