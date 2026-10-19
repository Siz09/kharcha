package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.EntryType
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: LedgerEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: LedgerEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: LedgerEntryEntity)

    @Query("SELECT * FROM ledger_entries WHERE id = :id LIMIT 1")
    fun getEntryById(id: Long): Flow<LedgerEntryEntity?>

    @Query("SELECT * FROM ledger_entries ORDER BY date DESC, id DESC")
    fun getAllEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE transactionId = :transactionId")
    suspend fun findByTransactionId(transactionId: String): List<LedgerEntryEntity>

    @Query("""
        SELECT e.* FROM ledger_entries e
        JOIN ledger_entries_fts fts ON e.id = fts.rowid
        WHERE ledger_entries_fts MATCH :query
        ORDER BY e.date DESC
    """)
    fun searchEntries(query: String): Flow<List<LedgerEntryEntity>>

    @Query("""
        SELECT SUM(amountPaisa) FROM ledger_entries
        WHERE type = :type AND date >= :startDate AND date <= :endDate
    """)
    fun getMonthlyTotalPaisa(type: EntryType, startDate: Long, endDate: Long): Flow<Long?>

    @Query("""
        SELECT SUM(CASE WHEN type = 'INCOME' THEN amountPaisa ELSE -amountPaisa END)
        FROM ledger_entries
        WHERE date < :beforeDate
    """)
    fun getNetBalanceBefore(beforeDate: Long): Flow<Long?>

    @Query("""
        SELECT * FROM ledger_entries
        WHERE (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC, id DESC
    """)
    fun getEntriesFiltered(
        type: EntryType?,
        categoryId: Long?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<LedgerEntryEntity>>

    @Query("""
        SELECT (
            SELECT openingBalancePaisa FROM accounts WHERE id = :accountId
        ) + COALESCE((
            SELECT SUM(amountPaisa) FROM ledger_entries WHERE accountId = :accountId AND type = 'INCOME'
        ), 0) - COALESCE((
            SELECT SUM(amountPaisa) FROM ledger_entries WHERE accountId = :accountId AND type = 'EXPENSE'
        ), 0) - COALESCE((
            SELECT SUM(amountPaisa) FROM ledger_entries WHERE accountId = :accountId AND type = 'TRANSFER'
        ), 0) + COALESCE((
            SELECT SUM(amountPaisa) FROM ledger_entries WHERE toAccountId = :accountId AND type = 'TRANSFER'
        ), 0)
    """)
    fun getAccountBalance(accountId: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM ledger_entries WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun countEntriesForAccount(accountId: Long): Int

    @Query("SELECT * FROM ledger_entries WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY date DESC, id DESC")
    fun getEntriesByAccount(accountId: Long): Flow<List<LedgerEntryEntity>>

    @Query("DELETE FROM ledger_entries")
    suspend fun deleteAllEntries()
}
