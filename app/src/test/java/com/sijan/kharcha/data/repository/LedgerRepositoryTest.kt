package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LedgerRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LedgerRepositoryImpl(
            db.categoryDao(),
            db.ledgerDao(),
            db.billDao(),
            db.budgetDao(),
            db.auditLogDao(),
            db.recurringEntryDao(),
            db.accountDao()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun seedDefaultCategoriesPopulatesDefaultsIfEmpty() = runBlocking {
        repository.seedDefaultCategories()

        val allCategories = repository.getAllCategories().first()
        assertTrue(allCategories.isNotEmpty())
        assertTrue(allCategories.any { it.name == "Food & Dining" && it.type == EntryType.EXPENSE })
        assertTrue(allCategories.any { it.name == "Salary" && it.type == EntryType.INCOME })

        val initialCount = allCategories.size
        // Second call should not duplicate
        repository.seedDefaultCategories()
        val secondCount = repository.getAllCategories().first().size
        assertEquals(initialCount, secondCount)
    }

    @Test
    fun deleteCategoryFailsIfEntriesExist() = runBlocking {
        repository.seedDefaultCategories()
        val foodCategory = repository.getCategoriesByType(EntryType.EXPENSE).first().first { it.name == "Food & Dining" }

        // Save entry referencing this category
        val entry = LedgerEntry(
            amountPaisa = 50000L,
            type = EntryType.EXPENSE,
            categoryId = foodCategory.id,
            date = 1000L
        )
        val saveResult = repository.saveEntry(entry)
        assertTrue(saveResult.isSuccess)

        // Attempt deletion of category with entries
        val deleteResult = repository.deleteCategory(foodCategory.id)
        assertFalse(deleteResult.isSuccess)
        assertTrue(deleteResult.exceptionOrNull()?.message?.contains("in use") == true)
    }

    @Test
    fun checkDuplicateTransactionReturnsExisting() = runBlocking {
        repository.seedDefaultCategories()
        val cat = repository.getAllCategories().first().first()

        repository.saveEntry(
            LedgerEntry(
                amountPaisa = 20000L,
                type = EntryType.EXPENSE,
                categoryId = cat.id,
                date = 1000L,
                transactionId = "ESEWA_TXN_001"
            )
        )

        val duplicates = repository.checkDuplicateTransaction("ESEWA_TXN_001")
        assertEquals(1, duplicates.size)
        assertEquals("ESEWA_TXN_001", duplicates[0].transactionId)
    }

    @Test
    fun saveBillAndPayBillCreatesLedgerEntry() = runBlocking {
        repository.seedDefaultCategories()
        val cat = repository.getAllCategories().first().first { it.type == EntryType.EXPENSE }

        val billId = repository.saveBill(
            com.sijan.kharcha.domain.model.Bill(
                name = "WorldLink Internet",
                amountPaisa = 150000L,
                categoryId = cat.id,
                dueDayOfMonth = 15
            )
        ).getOrThrow()

        val bills = repository.getAllBills().first()
        assertEquals(1, bills.size)
        assertEquals("WorldLink Internet", bills[0].name)

        val paidEntry = repository.payBill(billId).getOrThrow()
        assertEquals(150000L, paidEntry.amountPaisa)
        assertEquals(EntryType.EXPENSE, paidEntry.type)
        assertEquals("bill_payment", paidEntry.source)

        val updatedBill = repository.getBillById(billId).first()!!
        assertTrue(updatedBill.lastPaidDate != null)
    }

    @Test
    fun seedDefaultAccountsPopulatesExpectedAccounts() = runBlocking {
        repository.seedDefaultAccounts()
        val accounts = repository.getAllAccounts().first()
        assertEquals(3, accounts.size)
        assertEquals("Cash in Hand", accounts[0].name)
        assertEquals(com.sijan.kharcha.domain.model.AccountType.CASH, accounts[0].type)
        assertEquals("eSewa Wallet", accounts[1].name)
        assertEquals(com.sijan.kharcha.domain.model.AccountType.WALLET, accounts[1].type)
        assertEquals("Bank Account", accounts[2].name)
        assertEquals(com.sijan.kharcha.domain.model.AccountType.BANK, accounts[2].type)
    }

    @Test
    fun accountBalanceTracksIncomeExpenseAndTransfersAccurately() = runBlocking {
        repository.seedDefaultAccounts()
        repository.seedDefaultCategories()

        val cashAccount = repository.getAllAccounts().first().first { it.type == com.sijan.kharcha.domain.model.AccountType.CASH }
        val walletAccount = repository.getAllAccounts().first().first { it.type == com.sijan.kharcha.domain.model.AccountType.WALLET }
        val expenseCat = repository.getAllCategories().first().first { it.type == EntryType.EXPENSE }
        val incomeCat = repository.getAllCategories().first().first { it.type == EntryType.INCOME }

        // Set cash opening balance by updating account
        repository.updateAccount(cashAccount.copy(openingBalancePaisa = 100000L)).getOrThrow() // Rs 1,000

        // Income of Rs 500 (50000 paisa) into Cash
        repository.saveEntry(
            LedgerEntry(
                amountPaisa = 50000L,
                type = EntryType.INCOME,
                categoryId = incomeCat.id,
                accountId = cashAccount.id,
                date = 1000L
            )
        ).getOrThrow()

        // Expense of Rs 200 (20000 paisa) from Cash
        repository.saveEntry(
            LedgerEntry(
                amountPaisa = 20000L,
                type = EntryType.EXPENSE,
                categoryId = expenseCat.id,
                accountId = cashAccount.id,
                date = 2000L
            )
        ).getOrThrow()

        // Balance of Cash: 100000 + 50000 - 20000 = 130000 paisa
        val cashBalanceBeforeTransfer = repository.getAccountWithBalance(cashAccount.id).first()!!
        assertEquals(130000L, cashBalanceBeforeTransfer.currentBalancePaisa)

        // Transfer Rs 300 (30000 paisa) from Cash to eSewa Wallet
        repository.transfer(
            fromAccountId = cashAccount.id,
            toAccountId = walletAccount.id,
            amountPaisa = 30000L,
            note = "Load wallet",
            date = 3000L
        ).getOrThrow()

        // Cash: 130000 - 30000 = 100000 paisa
        val cashBalanceAfter = repository.getAccountWithBalance(cashAccount.id).first()!!
        assertEquals(100000L, cashBalanceAfter.currentBalancePaisa)

        // Wallet: 0 + 30000 = 30000 paisa
        val walletBalanceAfter = repository.getAccountWithBalance(walletAccount.id).first()!!
        assertEquals(30000L, walletBalanceAfter.currentBalancePaisa)

        // Total Net Worth: 100000 + 30000 = 130000 paisa
        val netWorth = repository.getTotalNetWorth().first()
        assertEquals(130000L, netWorth)
    }

    @Test
    fun deleteAccountFailsWhenEntriesExist() = runBlocking {
        repository.seedDefaultAccounts()
        repository.seedDefaultCategories()

        val cashAccount = repository.getAllAccounts().first().first { it.type == com.sijan.kharcha.domain.model.AccountType.CASH }
        val cat = repository.getAllCategories().first().first { it.type == EntryType.EXPENSE }

        repository.saveEntry(
            LedgerEntry(
                amountPaisa = 10000L,
                type = EntryType.EXPENSE,
                categoryId = cat.id,
                accountId = cashAccount.id,
                date = 1000L
            )
        ).getOrThrow()

        val deleteResult = repository.deleteAccount(cashAccount.id)
        assertTrue(deleteResult.isFailure)
    }
}
