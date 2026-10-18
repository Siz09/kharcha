package com.sijan.kharcha.ui.home

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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(db.categoryDao(), db.ledgerDao())
        viewModel = HomeViewModel(repository)
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
}
