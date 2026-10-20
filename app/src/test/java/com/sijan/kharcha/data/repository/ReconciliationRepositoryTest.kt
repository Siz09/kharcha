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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReconciliationRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ReconciliationRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        db.accountDao().insertAccount(
            AccountEntity(id = 1L, name = "Bank Account", type = AccountType.BANK, openingBalancePaisa = 1000000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 1L, name = "General", type = EntryType.EXPENSE)
        )

        repository = ReconciliationRepositoryImpl(
            appDatabase = db,
            reconciliationDao = db.reconciliationDao(),
            ledgerDao = db.ledgerDao(),
            accountDao = db.accountDao(),
            categoryDao = db.categoryDao()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun reconcileAccount_matchingBalance_marksEntriesReconciled() = runTest {
        // Opening balance: 1,000,000. Entry: expense 200,000. Computed: 800,000.
        val entryId = db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 200000L,
                type = EntryType.EXPENSE,
                categoryId = 1L,
                accountId = 1L,
                date = 1761800000000L,
                isReconciled = false
            )
        )

        val unclearedBefore = repository.getUnclearedEntries(1L, 1761850000000L).first()
        assertEquals(1, unclearedBefore.size)

        // Statement balance matches computed: 800,000
        val result = repository.reconcileAccount(
            accountId = 1L,
            statementDate = 1761850000000L,
            statementBalancePaisa = 800000L,
            clearedEntryIds = listOf(entryId),
            notes = "Month-end bank statement",
            createAdjustmentEntry = false
        )

        assertTrue(result.isSuccess)
        val rec = result.getOrThrow()
        assertTrue(rec.isReconciled)
        assertEquals(0L, rec.differencePaisa)
        assertEquals(800000L, rec.computedBalancePaisa)

        // Verify entry marked isReconciled = true
        val updatedEntry = db.ledgerDao().getEntryById(entryId).first()
        assertTrue(updatedEntry!!.isReconciled)

        val unclearedAfter = repository.getUnclearedEntries(1L, 1761850000000L).first()
        assertEquals(0, unclearedAfter.size)
    }

    @Test
    fun reconcileAccount_discrepancyWithAdjustment_createsBalancingEntry() = runTest {
        // Opening: 1,000,000. Statement says 1,050,000 (Surplus of 50,000).
        val result = repository.reconcileAccount(
            accountId = 1L,
            statementDate = 1761850000000L,
            statementBalancePaisa = 1050000L,
            clearedEntryIds = emptyList(),
            notes = "Interest credit discrepancy",
            createAdjustmentEntry = true
        )

        assertTrue(result.isSuccess)
        val rec = result.getOrThrow()
        assertTrue(rec.isReconciled)
        assertEquals(0L, rec.differencePaisa) // Zeroed out after adjustment

        // Check new balance
        val newBalance = db.ledgerDao().getAccountBalance(1L).first()
        assertEquals(1050000L, newBalance)
    }
}
