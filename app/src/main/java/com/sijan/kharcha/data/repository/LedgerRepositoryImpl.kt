package com.sijan.kharcha.data.repository

import com.sijan.kharcha.data.db.dao.AccountDao
import com.sijan.kharcha.data.db.dao.AuditLogDao
import com.sijan.kharcha.data.db.dao.BillDao
import com.sijan.kharcha.data.db.dao.BudgetDao
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.dao.RecurringEntryDao
import com.sijan.kharcha.data.db.entity.AccountEntity
import com.sijan.kharcha.data.db.entity.AuditLogEntity
import com.sijan.kharcha.data.db.entity.BillEntity
import com.sijan.kharcha.data.db.entity.BudgetEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.data.db.entity.RecurringEntryEntity
import com.sijan.kharcha.domain.model.Account
import com.sijan.kharcha.domain.model.AccountType
import com.sijan.kharcha.domain.model.AccountWithBalance
import com.sijan.kharcha.domain.model.Bill
import com.sijan.kharcha.domain.model.Budget
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.MonthlySummary
import com.sijan.kharcha.domain.model.RecurringEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LedgerRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val ledgerDao: LedgerDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
    private val auditLogDao: AuditLogDao,
    private val recurringEntryDao: RecurringEntryDao,
    private val accountDao: AccountDao
) : LedgerRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getCategoriesByType(type: EntryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertCategory(name: String, type: EntryType): Result<Long> = runCatching {
        require(name.isNotBlank()) { "Category name cannot be blank" }
        val id = categoryDao.insertCategory(CategoryEntity(name = name.trim(), type = type))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "CATEGORY",
            entityId = id,
            action = "INSERT",
            details = "Added $type category: $name"
        ))
        id
    }

    override suspend fun renameCategory(id: Long, newName: String): Result<Unit> = runCatching {
        require(newName.isNotBlank()) { "Category name cannot be blank" }
        val existing = categoryDao.getCategoryById(id)
            ?: throw NoSuchElementException("Category with ID $id not found")
        categoryDao.updateCategory(existing.copy(name = newName.trim()))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "CATEGORY",
            entityId = id,
            action = "UPDATE",
            details = "Renamed category ${existing.name} to $newName"
        ))
    }

    override suspend fun deleteCategory(id: Long): Result<Unit> = runCatching {
        val entryCount = categoryDao.countEntriesForCategory(id)
        if (entryCount > 0) {
            throw IllegalStateException("Cannot delete category in use by $entryCount existing entries")
        }
        val category = categoryDao.getCategoryById(id)
            ?: throw NoSuchElementException("Category with ID $id not found")
        categoryDao.deleteCategory(category)
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "CATEGORY",
            entityId = id,
            action = "DELETE",
            details = "Deleted category ${category.name}"
        ))
    }

    override suspend fun seedDefaultCategories() {
        if (categoryDao.countCategories() > 0) return

        val defaultExpense = listOf(
            "Food & Dining",
            "Groceries",
            "Utilities & Bills",
            "Transportation",
            "Shopping",
            "Health",
            "Entertainment",
            "Education",
            "Others"
        ).map { CategoryEntity(name = it, type = EntryType.EXPENSE) }

        val defaultIncome = listOf(
            "Salary",
            "Freelance / Business",
            "Investment",
            "Gifts",
            "Others"
        ).map { CategoryEntity(name = it, type = EntryType.INCOME) }

        categoryDao.insertAll(defaultExpense + defaultIncome)
        seedDefaultAccounts()
    }

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveAccounts(): Flow<List<Account>> {
        return accountDao.getActiveAccounts().map { list -> list.map { it.toDomain() } }
    }

    override fun getAccountById(id: Long): Flow<Account?> {
        return accountDao.getAccountById(id).map { it?.toDomain() }
    }

    override fun getAccountWithBalance(id: Long): Flow<AccountWithBalance?> {
        return combine(accountDao.getAccountById(id), ledgerDao.getAccountBalance(id)) { accountEntity, balance ->
            accountEntity?.let {
                AccountWithBalance(
                    account = it.toDomain(),
                    currentBalancePaisa = balance ?: it.openingBalancePaisa
                )
            }
        }
    }

    override fun getAccountsWithBalances(): Flow<List<AccountWithBalance>> {
        return accountDao.getAllAccounts().flatMapLatest { accounts ->
            if (accounts.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(accounts.map { account ->
                    ledgerDao.getAccountBalance(account.id).map { balance ->
                        AccountWithBalance(
                            account = account.toDomain(),
                            currentBalancePaisa = balance ?: account.openingBalancePaisa
                        )
                    }
                }) { array -> array.toList() }
            }
        }
    }

    override fun getTotalNetWorth(): Flow<Long> {
        return getAccountsWithBalances().map { list ->
            list.filter { it.account.isActive }.sumOf { it.currentBalancePaisa }
        }
    }

    override suspend fun saveAccount(account: Account): Result<Long> = runCatching {
        require(account.name.isNotBlank()) { "Account name cannot be blank" }
        require(account.openingBalancePaisa >= 0L) { "Opening balance cannot be negative" }
        val now = System.currentTimeMillis()
        val toInsert = account.copy(
            createdAt = if (account.createdAt == 0L) now else account.createdAt,
            updatedAt = now
        )
        val id = accountDao.insertAccount(AccountEntity.fromDomain(toInsert))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "ACCOUNT",
            entityId = id,
            action = "INSERT",
            details = "Added account ${account.name} (${account.type}) with opening balance ${account.openingBalancePaisa} paisa"
        ))
        id
    }

    override suspend fun updateAccount(account: Account): Result<Unit> = runCatching {
        require(account.id > 0) { "Cannot update account without ID" }
        require(account.name.isNotBlank()) { "Account name cannot be blank" }
        require(account.openingBalancePaisa >= 0L) { "Opening balance cannot be negative" }
        val toUpdate = account.copy(updatedAt = System.currentTimeMillis())
        accountDao.updateAccount(AccountEntity.fromDomain(toUpdate))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "ACCOUNT",
            entityId = account.id,
            action = "UPDATE",
            details = "Updated account ${account.id}: ${account.name}"
        ))
    }

    override suspend fun deleteAccount(id: Long): Result<Unit> = runCatching {
        val entryCount = ledgerDao.countEntriesForAccount(id)
        if (entryCount > 0) {
            throw IllegalStateException("Cannot delete account in use by $entryCount existing entries")
        }
        val account = accountDao.getAccountByIdDirect(id)
            ?: throw NoSuchElementException("Account with ID $id not found")
        accountDao.deleteAccount(account)
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "ACCOUNT",
            entityId = id,
            action = "DELETE",
            details = "Deleted account ${account.name}"
        ))
    }

    override suspend fun seedDefaultAccounts() {
        if (accountDao.countAccounts() > 0) return
        val now = System.currentTimeMillis()
        val defaultAccounts = listOf(
            AccountEntity(name = "Cash in Hand", type = AccountType.CASH, openingBalancePaisa = 0L, isActive = true, createdAt = now, updatedAt = now),
            AccountEntity(name = "eSewa Wallet", type = AccountType.WALLET, openingBalancePaisa = 0L, isActive = true, createdAt = now, updatedAt = now),
            AccountEntity(name = "Bank Account", type = AccountType.BANK, openingBalancePaisa = 0L, isActive = true, createdAt = now, updatedAt = now)
        )
        accountDao.insertAll(defaultAccounts)
    }

    override suspend fun transfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountPaisa: Long,
        note: String,
        date: Long
    ): Result<Long> = runCatching {
        require(fromAccountId != toAccountId) { "Source and destination accounts must be different" }
        require(amountPaisa > 0L) { "Transfer amount must be strictly positive" }
        val from = accountDao.getAccountByIdDirect(fromAccountId)
            ?: throw IllegalArgumentException("Source account $fromAccountId not found")
        val to = accountDao.getAccountByIdDirect(toAccountId)
            ?: throw IllegalArgumentException("Destination account $toAccountId not found")

        val categories = categoryDao.getAllCategories().first()
        val transferCategory = categories.find { it.name.equals("Transfer", ignoreCase = true) }
            ?: run {
                val newCatId = categoryDao.insertCategory(CategoryEntity(name = "Transfer", type = EntryType.EXPENSE))
                categoryDao.getCategoryById(newCatId) ?: throw IllegalStateException("Failed to create Transfer category")
            }

        val now = System.currentTimeMillis()
        val entry = LedgerEntry(
            id = 0L,
            amountPaisa = amountPaisa,
            type = EntryType.TRANSFER,
            categoryId = transferCategory.id,
            accountId = fromAccountId,
            toAccountId = toAccountId,
            note = if (note.isBlank()) "Transfer from ${from.name} to ${to.name}" else note.trim(),
            date = date,
            source = "transfer",
            createdAt = now,
            updatedAt = now
        )
        val entryId = ledgerDao.insertEntry(LedgerEntryEntity.fromDomain(entry))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "ENTRY",
            entityId = entryId,
            action = "TRANSFER",
            details = "Transferred $amountPaisa paisa from ${from.name} to ${to.name}"
        ))
        entryId
    }


    override fun getEntryById(id: Long): Flow<LedgerEntry?> {
        return ledgerDao.getEntryById(id).map { it?.toDomain() }
    }

    override fun getAllEntries(): Flow<List<LedgerEntry>> {
        return ledgerDao.getAllEntries().map { list -> list.map { it.toDomain() } }
    }

    override fun getFilteredEntries(
        type: EntryType?,
        categoryId: Long?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<LedgerEntry>> {
        return ledgerDao.getEntriesFiltered(type, categoryId, startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun searchEntries(query: String): Flow<List<LedgerEntry>> {
        val sanitized = query.trim().replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
        if (sanitized.isEmpty()) {
            return getAllEntries()
        }
        val ftsQuery = "$sanitized*"
        return ledgerDao.searchEntries(ftsQuery).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveEntry(entry: LedgerEntry): Result<Long> = runCatching {
        require(entry.amountPaisa > 0) { "Amount must be greater than zero" }
        require(entry.categoryId > 0) { "Category must be specified" }
        seedDefaultAccounts()
        val now = System.currentTimeMillis()
        val toInsert = entry.copy(
            createdAt = if (entry.createdAt == 0L) now else entry.createdAt,
            updatedAt = now
        )
        val id = ledgerDao.insertEntry(LedgerEntryEntity.fromDomain(toInsert))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "ENTRY",
            entityId = id,
            action = "INSERT",
            details = "Added ${entry.type} entry: ${entry.amountPaisa} paisa"
        ))
        id
    }

    override suspend fun updateEntry(entry: LedgerEntry): Result<Unit> = runCatching {
        require(entry.id > 0) { "Cannot update entry without ID" }
        require(entry.amountPaisa > 0) { "Amount must be greater than zero" }
        val toUpdate = entry.copy(updatedAt = System.currentTimeMillis())
        ledgerDao.updateEntry(LedgerEntryEntity.fromDomain(toUpdate))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "ENTRY",
            entityId = entry.id,
            action = "UPDATE",
            details = "Updated entry ${entry.id}: ${entry.amountPaisa} paisa"
        ))
    }

    override suspend fun deleteEntry(id: Long): Result<Unit> = runCatching {
        val entity = ledgerDao.getEntryById(id).first() ?: return@runCatching
        entity.receiptPath?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }

        ledgerDao.deleteEntry(entity)
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "ENTRY",
            entityId = id,
            action = "DELETE",
            details = "Deleted entry $id"
        ))
    }

    override suspend fun checkDuplicateTransaction(transactionId: String): List<LedgerEntry> {
        if (transactionId.isBlank()) return emptyList()
        return ledgerDao.findByTransactionId(transactionId.trim()).map { it.toDomain() }
    }

    override fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMillis = cal.timeInMillis

        val incomeFlow = ledgerDao.getMonthlyTotalPaisa(EntryType.INCOME, startMillis, endMillis)
        val expenseFlow = ledgerDao.getMonthlyTotalPaisa(EntryType.EXPENSE, startMillis, endMillis)

        return combine(incomeFlow, expenseFlow) { income, expense ->
            val inc = income ?: 0L
            val exp = expense ?: 0L
            MonthlySummary(
                totalIncomePaisa = inc,
                totalExpensePaisa = exp,
                balancePaisa = inc - exp
            )
        }
    }

    override fun getOpeningBalance(beforeDate: Long): Flow<Long> {
        return ledgerDao.getNetBalanceBefore(beforeDate).map { it ?: 0L }
    }

    override fun getAllBills(): Flow<List<Bill>> {
        return billDao.getAllBills().map { list -> list.map { it.toDomain() } }
    }

    override fun getBillById(id: Long): Flow<Bill?> {
        return billDao.getBillById(id).map { it?.toDomain() }
    }

    override suspend fun saveBill(bill: Bill): Result<Long> = runCatching {
        require(bill.name.isNotBlank()) { "Bill name cannot be blank" }
        require(bill.amountPaisa > 0L) { "Bill amount must be positive" }
        require(bill.dueDayOfMonth in 1..31) { "Due day must be between 1 and 31" }
        val id = billDao.insertBill(BillEntity.fromDomain(bill))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "BILL",
            entityId = id,
            action = "INSERT",
            details = "Added bill ${bill.name} for ${bill.amountPaisa} paisa"
        ))
        id
    }

    override suspend fun updateBill(bill: Bill): Result<Unit> = runCatching {
        require(bill.name.isNotBlank()) { "Bill name cannot be blank" }
        require(bill.amountPaisa > 0L) { "Bill amount must be positive" }
        require(bill.dueDayOfMonth in 1..31) { "Due day must be between 1 and 31" }
        billDao.updateBill(BillEntity.fromDomain(bill))
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "BILL",
            entityId = bill.id,
            action = "UPDATE",
            details = "Updated bill ${bill.name}"
        ))
    }

    override suspend fun deleteBill(id: Long): Result<Unit> = runCatching {
        val entity = billDao.getBillById(id).first() ?: return@runCatching
        billDao.deleteBill(entity)
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "BILL",
            entityId = id,
            action = "DELETE",
            details = "Deleted bill ${entity.name}"
        ))
    }

    override suspend fun payBill(billId: Long, note: String?): Result<LedgerEntry> = runCatching {
        seedDefaultAccounts()
        val billEntity = billDao.getBillById(billId).first()
            ?: throw IllegalArgumentException("Bill with id $billId not found")

        val now = System.currentTimeMillis()
        val entry = LedgerEntry(
            id = 0L,
            amountPaisa = billEntity.amountPaisa,
            type = EntryType.EXPENSE,
            categoryId = billEntity.categoryId,
            note = note ?: "Bill: ${billEntity.name}",
            date = now,
            source = "bill_payment",
            createdAt = now,
            updatedAt = now
        )

        val entryId = ledgerDao.insertEntry(LedgerEntryEntity.fromDomain(entry))
        val updatedBill = billEntity.copy(lastPaidDate = now, updatedAt = now)
        billDao.updateBill(updatedBill)

        auditLogDao.insertLog(AuditLogEntity(
            entityType = "BILL",
            entityId = billId,
            action = "PAY",
            details = "Paid bill ${billEntity.name}: recorded entry $entryId"
        ))

        entry.copy(id = entryId)
    }

    override fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(monthYear).map { list -> list.map { it.toDomain() } }
    }

    override fun getBudget(categoryId: Long, monthYear: String): Flow<Budget?> {
        return budgetDao.observeBudget(categoryId, monthYear).map { it?.toDomain() }
    }

    override suspend fun saveBudget(categoryId: Long, monthYear: String, limitPaisa: Long): Result<Long> = runCatching {
        require(limitPaisa > 0) { "Budget limit must be greater than zero" }
        val existing = budgetDao.getBudget(categoryId, monthYear)
        val entity = BudgetEntity(
            id = existing?.id ?: 0L,
            categoryId = categoryId,
            monthYear = monthYear,
            limitPaisa = limitPaisa,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = budgetDao.upsertBudget(entity)
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "BUDGET",
            entityId = id,
            action = if (existing == null) "INSERT" else "UPDATE",
            details = "Budget for cat $categoryId in $monthYear: $limitPaisa paisa"
        ))
        id
    }

    override suspend fun deleteBudget(categoryId: Long, monthYear: String): Result<Unit> = runCatching {
        budgetDao.deleteBudget(categoryId, monthYear)
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "BUDGET",
            entityId = categoryId,
            action = "DELETE",
            details = "Deleted budget for cat $categoryId in $monthYear"
        ))
    }

    override fun getAllRecurring(): Flow<List<RecurringEntry>> {
        return recurringEntryDao.getAllRecurring().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveRecurring(entry: RecurringEntry): Result<Long> = runCatching {
        require(entry.amountPaisa > 0) { "Amount must be greater than zero" }
        require(entry.categoryId > 0) { "Category must be specified" }
        val now = System.currentTimeMillis()
        val entity = RecurringEntryEntity(
            id = entry.id,
            amountPaisa = entry.amountPaisa,
            type = entry.type,
            categoryId = entry.categoryId,
            note = entry.note,
            frequency = entry.frequency,
            nextDueDate = entry.nextDueDate,
            isActive = entry.isActive,
            createdAt = if (entry.createdAt == 0L) now else entry.createdAt,
            updatedAt = now
        )
        val id = recurringEntryDao.insertRecurring(entity)
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "RECURRING",
            entityId = id,
            action = if (entry.id == 0L) "INSERT" else "UPDATE",
            details = "Recurring entry template ${entry.frequency} ${entry.amountPaisa} paisa"
        ))
        id
    }

    override suspend fun deleteRecurring(id: Long): Result<Unit> = runCatching {
        recurringEntryDao.deleteRecurring(id)
        auditLogDao.insertLog(AuditLogEntity(
            entityType = "RECURRING",
            entityId = id,
            action = "DELETE",
            details = "Deleted recurring template $id"
        ))
    }

    private fun BudgetEntity.toDomain(): Budget {
        return Budget(
            id = id,
            categoryId = categoryId,
            monthYear = monthYear,
            limitPaisa = limitPaisa,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun RecurringEntryEntity.toDomain(): RecurringEntry {
        return RecurringEntry(
            id = id,
            amountPaisa = amountPaisa,
            type = type,
            categoryId = categoryId,
            note = note,
            frequency = frequency,
            nextDueDate = nextDueDate,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
