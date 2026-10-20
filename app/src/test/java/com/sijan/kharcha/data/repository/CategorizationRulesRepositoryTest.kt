package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.entity.AccountEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.AccountType
import com.sijan.kharcha.domain.model.CategorizationRule
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
class CategorizationRulesRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: CategorizationRulesRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        db.accountDao().insertAccount(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, openingBalancePaisa = 100000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 1L, name = "General", type = EntryType.EXPENSE)
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 2L, name = "Groceries", type = EntryType.EXPENSE)
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = 3L, name = "Fuel", type = EntryType.EXPENSE)
        )

        repository = CategorizationRulesRepositoryImpl(
            categorizationRuleDao = db.categorizationRuleDao(),
            ledgerDao = db.ledgerDao(),
            appDatabase = db
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun suggestCategory_matchesPriorityRule() = runTest {
        // High priority rule for Bhatbhateni -> Groceries (cat 2)
        repository.saveRule(
            CategorizationRule(
                name = "Bhatbhateni Rule",
                pattern = "bhatbhateni",
                categoryId = 2L,
                priority = 10
            )
        )

        // Lower priority rule for Petrol -> Fuel (cat 3)
        repository.saveRule(
            CategorizationRule(
                name = "Petrol Rule",
                pattern = "petrol",
                categoryId = 3L,
                priority = 5
            )
        )

        val suggested1 = repository.suggestCategory("Paid at Bhatbhateni Supermarket")
        assertEquals(2L, suggested1)

        val suggested2 = repository.suggestCategory("Bike petrol refill")
        assertEquals(3L, suggested2)

        val suggestedNone = repository.suggestCategory("Unrelated purchase")
        assertEquals(null, suggestedNone)
    }

    @Test
    fun applyRulesToEntries_batchUpdatesMatchingEntries() = runTest {
        repository.saveRule(
            CategorizationRule(
                name = "Bhatbhateni Rule",
                pattern = "bhatbhateni",
                categoryId = 2L,
                priority = 10
            )
        )

        // Entry currently has category 1 (General), but note mentions Bhatbhateni
        val entryId = db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 150000L,
                type = EntryType.EXPENSE,
                categoryId = 1L,
                accountId = 1L,
                date = 1000L,
                note = "Bhatbhateni Veggies"
            )
        )

        val updatedCount = repository.applyRulesToEntries().getOrThrow()
        assertEquals(1, updatedCount)

        val updatedEntry = db.ledgerDao().getEntryById(entryId).first()
        assertEquals(2L, updatedEntry!!.categoryId) // Successfully recategorized to Groceries!
    }
}
