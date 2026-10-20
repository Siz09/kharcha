package com.sijan.kharcha.data.repository

import androidx.room.withTransaction
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.dao.AccountDao
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.dao.ReconciliationDao
import com.sijan.kharcha.data.db.entity.AccountReconciliationEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.AccountReconciliation
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconciliationRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val reconciliationDao: ReconciliationDao,
    private val ledgerDao: LedgerDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ReconciliationRepository {

    override fun getReconciliations(accountId: Long): Flow<List<AccountReconciliation>> {
        return reconciliationDao.getReconciliationsForAccount(accountId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUnclearedEntries(accountId: Long, beforeDate: Long): Flow<List<LedgerEntry>> {
        return reconciliationDao.getUnclearedEntries(accountId, beforeDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun reconcileAccount(
        accountId: Long,
        statementDate: Long,
        statementBalancePaisa: Long,
        clearedEntryIds: List<Long>,
        notes: String?,
        createAdjustmentEntry: Boolean
    ): Result<AccountReconciliation> = runCatching {
        appDatabase.withTransaction {
            // 1. Mark cleared entries
            if (clearedEntryIds.isNotEmpty()) {
                reconciliationDao.updateEntriesReconciledStatus(clearedEntryIds, true)
            }

            // 2. Compute current balance
            val computedBalance = ledgerDao.getAccountBalance(accountId).first() ?: 0L
            var difference = statementBalancePaisa - computedBalance

            // 3. Create adjustment entry if requested and non-zero
            if (difference != 0L && createAdjustmentEntry) {
                val categories = categoryDao.getAllCategories().first()
                val adjCatId = categories.firstOrNull { it.name.equals("General", ignoreCase = true) }?.id
                    ?: categoryDao.insertCategory(CategoryEntity(name = "General", type = if (difference > 0) EntryType.INCOME else EntryType.EXPENSE))

                val isSurplus = difference > 0L
                val adjEntry = LedgerEntryEntity(
                    amountPaisa = if (isSurplus) difference else -difference,
                    type = if (isSurplus) EntryType.INCOME else EntryType.EXPENSE,
                    categoryId = adjCatId,
                    accountId = accountId,
                    note = if (isSurplus) "Reconciliation Surplus Adjustment" else "Reconciliation Deficit Adjustment",
                    date = statementDate,
                    source = "reconciliation",
                    isReconciled = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                ledgerDao.insertEntry(adjEntry)
                difference = 0L
            }

            val reconciliationEntity = AccountReconciliationEntity(
                accountId = accountId,
                statementDate = statementDate,
                statementBalancePaisa = statementBalancePaisa,
                computedBalancePaisa = computedBalance,
                differencePaisa = difference,
                isReconciled = difference == 0L,
                reconciledAt = System.currentTimeMillis(),
                notes = notes
            )
            val recId = reconciliationDao.insertReconciliation(reconciliationEntity)
            reconciliationEntity.copy(id = recId).toDomain()
        }
    }
}
