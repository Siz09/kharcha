package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.AccountReconciliation
import com.sijan.kharcha.domain.model.LedgerEntry
import kotlinx.coroutines.flow.Flow

interface ReconciliationRepository {
    fun getReconciliations(accountId: Long): Flow<List<AccountReconciliation>>
    fun getUnclearedEntries(accountId: Long, beforeDate: Long): Flow<List<LedgerEntry>>
    suspend fun reconcileAccount(
        accountId: Long,
        statementDate: Long,
        statementBalancePaisa: Long,
        clearedEntryIds: List<Long>,
        notes: String? = null,
        createAdjustmentEntry: Boolean = false
    ): Result<AccountReconciliation>
}
