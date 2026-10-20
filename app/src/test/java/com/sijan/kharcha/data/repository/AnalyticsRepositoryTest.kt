package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.entity.AccountEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.calendar.BikramSambatConverter
import com.sijan.kharcha.domain.calendar.NepaliDate
import com.sijan.kharcha.domain.model.AccountType
import com.sijan.kharcha.domain.model.CalendarSystem
import com.sijan.kharcha.domain.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class AnalyticsRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AnalyticsRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.accountDao().insertAccount(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, openingBalancePaisa = 0L)
        )
        repository = AnalyticsRepositoryImpl(db.ledgerDao(), db.categoryDao(), db.annualArchiveSummaryDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getMonthlyAnalytics_computesMonthOverMonthCorrectly() = runTest {
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Food", type = EntryType.EXPENSE))

        val currentMonthDate = LocalDate.of(2026, 10, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val prevMonthDate = LocalDate.of(2026, 9, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 300000L, categoryId = catId, type = EntryType.EXPENSE, date = currentMonthDate))
        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 200000L, categoryId = catId, type = EntryType.EXPENSE, date = prevMonthDate))

        val summary = repository.getMonthlyAnalytics(2026, 10).first()
        assertEquals(300000L, summary.momComparison.currentMonthTotalExpensePaisa)
        assertEquals(200000L, summary.momComparison.previousMonthTotalExpensePaisa)
        assertEquals(100000L, summary.momComparison.netDifferencePaisa)
        assertEquals(50, summary.momComparison.percentageChange)
    }

    @Test
    fun getAnnualAnalytics_adCalendar_computesIncomeExpenseAndMonthlyPoints() = runTest {
        val salaryCat = db.categoryDao().insertCategory(CategoryEntity(name = "Salary", type = EntryType.INCOME))
        val rentCat = db.categoryDao().insertCategory(CategoryEntity(name = "Rent", type = EntryType.EXPENSE))

        val febDate = LocalDate.of(2026, 2, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val marDate = LocalDate.of(2026, 3, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 10000000L, categoryId = salaryCat, type = EntryType.INCOME, date = febDate))
        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 3000000L, categoryId = rentCat, type = EntryType.EXPENSE, date = marDate))

        val annual = repository.getAnnualAnalytics(2026, CalendarSystem.AD).first()
        assertEquals(10000000L, annual.totalIncomePaisa)
        assertEquals(3000000L, annual.totalExpensePaisa)
        assertEquals(7000000L, annual.netSavingsPaisa)
        assertEquals(70, annual.savingsRatePercentage)
        assertEquals(2, annual.entryCount)
        assertEquals(12, annual.monthlyTotals.size)
        assertFalse(annual.isArchived)
    }

    @Test
    fun getAnnualAnalytics_bsCalendar_computesBikramSambatRanges() = runTest {
        val salaryCat = db.categoryDao().insertCategory(CategoryEntity(name = "Salary", type = EntryType.INCOME))
        val groceryCat = db.categoryDao().insertCategory(CategoryEntity(name = "Groceries", type = EntryType.EXPENSE))

        // Baisakh 15, 2081 BS
        val baisakhEpoch = BikramSambatConverter.toEpochMillis(NepaliDate(2081, 1, 15))
        // Shrawan 10, 2081 BS
        val shrawanEpoch = BikramSambatConverter.toEpochMillis(NepaliDate(2081, 4, 10))

        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 5000000L, categoryId = salaryCat, type = EntryType.INCOME, date = baisakhEpoch))
        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 1500000L, categoryId = groceryCat, type = EntryType.EXPENSE, date = shrawanEpoch))

        val annual = repository.getAnnualAnalytics(2081, CalendarSystem.BS).first()
        assertEquals(5000000L, annual.totalIncomePaisa)
        assertEquals(1500000L, annual.totalExpensePaisa)
        assertEquals(3500000L, annual.netSavingsPaisa)
        assertEquals(70, annual.savingsRatePercentage)
        assertEquals(2, annual.entryCount)
        assertEquals(12, annual.monthlyTotals.size)
        assertEquals(1, annual.categoryBreakdown.size)
        assertEquals("Groceries", annual.categoryBreakdown[0].categoryName)
        assertEquals(1500000L, annual.categoryBreakdown[0].totalExpensePaisa)
    }

    @Test
    fun archiveCompletedYear_persistsSnapshotAndLoadsFromArchive() = runTest {
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Fuel", type = EntryType.EXPENSE))
        val dateEpoch = LocalDate.of(2025, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 800000L, categoryId = catId, type = EntryType.EXPENSE, date = dateEpoch))

        // Archive 2025 AD
        val archiveResult = repository.archiveCompletedYear(2025, CalendarSystem.AD)
        assertTrue(archiveResult.isSuccess)

        // Query again, should return archived snapshot
        val cached = repository.getAnnualAnalytics(2025, CalendarSystem.AD).first()
        assertTrue(cached.isArchived)
        assertEquals(800000L, cached.totalExpensePaisa)
        assertEquals(1, cached.entryCount)
        assertEquals(1, cached.categoryBreakdown.size)
        assertEquals("Fuel", cached.categoryBreakdown[0].categoryName)
    }
}
