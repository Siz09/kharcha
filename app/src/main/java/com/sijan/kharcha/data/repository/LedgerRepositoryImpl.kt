package com.sijan.kharcha.data.repository

import com.sijan.kharcha.data.db.dao.BillDao
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.entity.BillEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.Bill
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.MonthlySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val ledgerDao: LedgerDao,
    private val billDao: BillDao
) : LedgerRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getCategoriesByType(type: EntryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertCategory(name: String, type: EntryType): Result<Long> = runCatching {
        require(name.isNotBlank()) { "Category name cannot be blank" }
        categoryDao.insertCategory(CategoryEntity(name = name.trim(), type = type))
    }

    override suspend fun renameCategory(id: Long, newName: String): Result<Unit> = runCatching {
        require(newName.isNotBlank()) { "Category name cannot be blank" }
        val existing = categoryDao.getCategoryById(id)
            ?: throw NoSuchElementException("Category with ID $id not found")
        categoryDao.updateCategory(existing.copy(name = newName.trim()))
    }

    override suspend fun deleteCategory(id: Long): Result<Unit> = runCatching {
        val entryCount = categoryDao.countEntriesForCategory(id)
        if (entryCount > 0) {
            throw IllegalStateException("Cannot delete category in use by $entryCount existing entries")
        }
        val category = categoryDao.getCategoryById(id)
            ?: throw NoSuchElementException("Category with ID $id not found")
        categoryDao.deleteCategory(category)
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
        val now = System.currentTimeMillis()
        val toInsert = entry.copy(
            createdAt = if (entry.createdAt == 0L) now else entry.createdAt,
            updatedAt = now
        )
        ledgerDao.insertEntry(LedgerEntryEntity.fromDomain(toInsert))
    }

    override suspend fun updateEntry(entry: LedgerEntry): Result<Unit> = runCatching {
        require(entry.id > 0) { "Cannot update entry without ID" }
        require(entry.amountPaisa > 0) { "Amount must be greater than zero" }
        val toUpdate = entry.copy(updatedAt = System.currentTimeMillis())
        ledgerDao.updateEntry(LedgerEntryEntity.fromDomain(toUpdate))
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
        billDao.insertBill(BillEntity.fromDomain(bill))
    }

    override suspend fun updateBill(bill: Bill): Result<Unit> = runCatching {
        require(bill.name.isNotBlank()) { "Bill name cannot be blank" }
        require(bill.amountPaisa > 0L) { "Bill amount must be positive" }
        require(bill.dueDayOfMonth in 1..31) { "Due day must be between 1 and 31" }
        billDao.updateBill(BillEntity.fromDomain(bill))
    }

    override suspend fun deleteBill(id: Long): Result<Unit> = runCatching {
        val entity = billDao.getBillById(id).first() ?: return@runCatching
        billDao.deleteBill(entity)
    }

    override suspend fun payBill(billId: Long, note: String?): Result<LedgerEntry> = runCatching {
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

        entry.copy(id = entryId)
    }
}
