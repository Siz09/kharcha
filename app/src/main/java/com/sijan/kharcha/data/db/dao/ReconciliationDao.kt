package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sijan.kharcha.data.db.entity.AccountReconciliationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReconciliationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliation(reconciliation: AccountReconciliationEntity): Long

    @Query("SELECT * FROM account_reconciliations WHERE accountId = :accountId ORDER BY statementDate DESC, id DESC")
    fun getReconciliationsForAccount(accountId: Long): Flow<List<AccountReconciliationEntity>>

    @Query("SELECT * FROM account_reconciliations WHERE id = :id LIMIT 1")
    fun getReconciliationById(id: Long): Flow<AccountReconciliationEntity?>

    @Query("UPDATE ledger_entries SET isReconciled = :isReconciled WHERE id IN (:entryIds)")
    suspend fun updateEntriesReconciledStatus(entryIds: List<Long>, isReconciled: Boolean)

    @Query("SELECT * FROM ledger_entries WHERE accountId = :accountId AND isReconciled = 0 AND date <= :beforeDate ORDER BY date DESC")
    fun getUnclearedEntries(accountId: Long, beforeDate: Long): Flow<List<com.sijan.kharcha.data.db.entity.LedgerEntryEntity>>

    @Query("DELETE FROM account_reconciliations WHERE id = :id")
    suspend fun deleteReconciliation(id: Long)

    @Query("DELETE FROM account_reconciliations")
    suspend fun deleteAllReconciliations()
}
