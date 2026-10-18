package com.sijan.kharcha.data.repository

import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
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
    private val ledgerDao: LedgerDao
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
        val existing = categoryDao.getAllCategories().first()
        if (existing.isNotEmpty()) return

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
        return ledgerDao.getEntriesFiltered(type, categoryId, startDate, endDate)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun searchEntries(query: String): Flow<List<LedgerEntry>> {
        val formattedQuery = query.trim().split("\\s+".toRegex()).joinToString(" ") { "$it*" }
        return ledgerDao.searchEntries(formattedQuery).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveEntry(entry: LedgerEntry): Result<Long> = runCatching {
        require(entry.amountPaisa > 0) { "Entry amount in paisa must be greater than zero" }
        ledgerDao.insertEntry(LedgerEntryEntity.fromDomain(entry))
    }

    override suspend fun updateEntry(entry: LedgerEntry): Result<Unit> = runCatching {
        require(entry.amountPaisa > 0) { "Entry amount in paisa must be greater than zero" }
        ledgerDao.updateEntry(LedgerEntryEntity.fromDomain(entry.copy(updatedAt = System.currentTimeMillis())))
    }

    override suspend fun deleteEntry(id: Long): Result<Unit> = runCatching {
        val entity = ledgerDao.getEntryById(id).first()
            ?: throw NoSuchElementException("Ledger entry with ID $id not found")

        entity.receiptPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (ignored: Exception) {
                // Best effort image deletion
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
}
