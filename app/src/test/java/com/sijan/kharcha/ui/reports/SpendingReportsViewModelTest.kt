package com.sijan.kharcha.ui.reports

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.repository.AnalyticsRepository
import com.sijan.kharcha.data.repository.AnalyticsRepositoryImpl
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.data.repository.LedgerRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SpendingReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var ledgerRepository: LedgerRepository
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var pdfGenerator: PdfStatementGenerator
    private lateinit var viewModel: SpendingReportsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        ledgerRepository = LedgerRepositoryImpl(
            categoryDao = db.categoryDao(),
            ledgerDao = db.ledgerDao(),
            billDao = db.billDao(),
            budgetDao = db.budgetDao(),
            auditLogDao = db.auditLogDao(),
            recurringEntryDao = db.recurringEntryDao(),
            accountDao = db.accountDao()
        )
        analyticsRepository = AnalyticsRepositoryImpl(
            ledgerDao = db.ledgerDao(),
            categoryDao = db.categoryDao(),
            annualArchiveSummaryDao = db.annualArchiveSummaryDao()
        )
        pdfGenerator = PdfStatementGenerator(context)

        viewModel = SpendingReportsViewModel(
            analyticsRepository = analyticsRepository,
            ledgerRepository = ledgerRepository,
            pdfStatementGenerator = pdfGenerator
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialState_defaultsToCurrentMonthAndYear() {
        val now = LocalDate.now()
        assertEquals(now.year, viewModel.selectedYear.value)
        assertEquals(now.monthValue, viewModel.selectedMonth.value)
        assertFalse(viewModel.isGeneratingPdf.value)
    }

    @Test
    fun monthNavigation_updatesSelectedYearAndMonth() = runTest(testDispatcher) {
        viewModel.selectedYear.value = 2026
        viewModel.selectedMonth.value = 10

        viewModel.onNextMonth()
        assertEquals(2026, viewModel.selectedYear.value)
        assertEquals(11, viewModel.selectedMonth.value)

        viewModel.onPreviousMonth()
        assertEquals(2026, viewModel.selectedYear.value)
        assertEquals(10, viewModel.selectedMonth.value)

        viewModel.onPreviousMonth()
        assertEquals(2026, viewModel.selectedYear.value)
        assertEquals(9, viewModel.selectedMonth.value)
    }

    @Test
    fun yearBoundaryNavigation_wrapsCorrectly() = runTest(testDispatcher) {
        viewModel.selectedYear.value = 2026
        viewModel.selectedMonth.value = 12

        viewModel.onNextMonth()
        assertEquals(2027, viewModel.selectedYear.value)
        assertEquals(1, viewModel.selectedMonth.value)

        viewModel.onPreviousMonth()
        assertEquals(2026, viewModel.selectedYear.value)
        assertEquals(12, viewModel.selectedMonth.value)

        viewModel.selectedMonth.value = 1
        viewModel.onPreviousMonth()
        assertEquals(2025, viewModel.selectedYear.value)
        assertEquals(12, viewModel.selectedMonth.value)
    }

    @Test
    fun annualNavigation_switchesTabsAndCalendarSystems() = runTest(testDispatcher) {
        assertEquals(0, viewModel.selectedReportTab.value)
        viewModel.onSelectTab(1)
        assertEquals(1, viewModel.selectedReportTab.value)

        viewModel.onSetCalendarSystem(com.sijan.kharcha.domain.model.CalendarSystem.AD)
        assertEquals(com.sijan.kharcha.domain.model.CalendarSystem.AD, viewModel.selectedCalendarSystem.value)
        assertEquals(LocalDate.now().year, viewModel.selectedAnnualYear.value)

        val initialYear = viewModel.selectedAnnualYear.value
        viewModel.onPreviousAnnualYear()
        assertEquals(initialYear - 1, viewModel.selectedAnnualYear.value)

        viewModel.onNextAnnualYear()
        assertEquals(initialYear, viewModel.selectedAnnualYear.value)
    }
}
