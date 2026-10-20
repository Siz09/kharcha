package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sijan.kharcha.data.db.entity.FestivalBudgetEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FestivalBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFestivalBudget(budget: FestivalBudgetEntity): Long

    @Update
    suspend fun updateFestivalBudget(budget: FestivalBudgetEntity)

    @Delete
    suspend fun deleteFestivalBudget(budget: FestivalBudgetEntity)

    @Query("SELECT * FROM festival_budgets WHERE id = :id LIMIT 1")
    fun getFestivalBudgetById(id: Long): Flow<FestivalBudgetEntity?>

    @Query("SELECT * FROM festival_budgets WHERE isArchived = 0 ORDER BY startDateEpoch DESC")
    fun getActiveFestivalBudgets(): Flow<List<FestivalBudgetEntity>>

    @Query("SELECT * FROM festival_budgets ORDER BY startDateEpoch DESC")
    fun getAllFestivalBudgets(): Flow<List<FestivalBudgetEntity>>

    @Query("SELECT * FROM ledger_entries WHERE festivalBudgetId = :budgetId ORDER BY date DESC")
    fun getEntriesForFestivalBudget(budgetId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT SUM(amountPaisa) FROM ledger_entries WHERE festivalBudgetId = :budgetId AND type = 'EXPENSE'")
    fun getTotalSpentForFestivalBudget(budgetId: Long): Flow<Long?>

    @Query("DELETE FROM festival_budgets WHERE id = :id")
    suspend fun deleteFestivalBudgetById(id: Long)

    @Query("DELETE FROM festival_budgets")
    suspend fun deleteAllFestivalBudgets()
}
