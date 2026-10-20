package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.entity.AccountEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.AccountType
import com.sijan.kharcha.domain.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
        repository = AnalyticsRepositoryImpl(db.ledgerDao(), db.categoryDao())
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
}
