package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.Account
import com.sijan.kharcha.domain.model.AccountWithBalance
import com.sijan.kharcha.domain.model.Bill
import com.sijan.kharcha.domain.model.Budget
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.MonthlySummary
import com.sijan.kharcha.domain.model.RecurringEntry
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: EntryType): Flow<List<Category>>
    suspend fun insertCategory(name: String, type: EntryType): Result<Long>
    suspend fun renameCategory(id: Long, newName: String): Result<Unit>
    suspend fun deleteCategory(id: Long): Result<Unit>
    suspend fun seedDefaultCategories()

    fun getAllAccounts(): Flow<List<Account>>
    fun getActiveAccounts(): Flow<List<Account>>
    fun getAccountById(id: Long): Flow<Account?>
    fun getAccountWithBalance(id: Long): Flow<AccountWithBalance?>
    fun getAccountsWithBalances(): Flow<List<AccountWithBalance>>
    fun getTotalNetWorth(): Flow<Long>
    suspend fun saveAccount(account: Account): Result<Long>
    suspend fun updateAccount(account: Account): Result<Unit>
    suspend fun deleteAccount(id: Long): Result<Unit>
    suspend fun seedDefaultAccounts()
    suspend fun transfer(fromAccountId: Long, toAccountId: Long, amountPaisa: Long, note: String, date: Long): Result<Long>

    fun getEntryById(id: Long): Flow<LedgerEntry?>
    fun getAllEntries(): Flow<List<LedgerEntry>>
    fun getFilteredEntries(
        type: EntryType? = null,
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<LedgerEntry>>
    fun searchEntries(query: String): Flow<List<LedgerEntry>>
    suspend fun saveEntry(entry: LedgerEntry): Result<Long>
    suspend fun updateEntry(entry: LedgerEntry): Result<Unit>
    suspend fun deleteEntry(id: Long): Result<Unit>

    suspend fun checkDuplicateTransaction(transactionId: String): List<LedgerEntry>
    fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary>
    fun getOpeningBalance(beforeDate: Long): Flow<Long>

    fun getAllBills(): Flow<List<Bill>>
    fun getBillById(id: Long): Flow<Bill?>
    suspend fun saveBill(bill: Bill): Result<Long>
    suspend fun updateBill(bill: Bill): Result<Unit>
    suspend fun deleteBill(id: Long): Result<Unit>
    suspend fun payBill(billId: Long, note: String? = null): Result<LedgerEntry>

    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>>
    fun getBudget(categoryId: Long, monthYear: String): Flow<Budget?>
    suspend fun saveBudget(categoryId: Long, monthYear: String, limitPaisa: Long): Result<Long>
    suspend fun deleteBudget(categoryId: Long, monthYear: String): Result<Unit>

    fun getAllRecurring(): Flow<List<RecurringEntry>>
    suspend fun saveRecurring(entry: RecurringEntry): Result<Long>
    suspend fun deleteRecurring(id: Long): Result<Unit>
}
