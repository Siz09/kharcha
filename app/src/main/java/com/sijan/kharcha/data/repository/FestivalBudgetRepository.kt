package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.FestivalBudget
import com.sijan.kharcha.domain.model.FestivalBudgetProgress
import com.sijan.kharcha.domain.model.LedgerEntry
import kotlinx.coroutines.flow.Flow

interface FestivalBudgetRepository {
    fun getActiveFestivalBudgets(): Flow<List<FestivalBudgetProgress>>
    fun getAllFestivalBudgets(): Flow<List<FestivalBudget>>
    fun getFestivalBudgetById(id: Long): Flow<FestivalBudget?>
    fun getEntriesForFestival(budgetId: Long): Flow<List<LedgerEntry>>
    suspend fun saveFestivalBudget(budget: FestivalBudget): Result<Long>
    suspend fun deleteFestivalBudget(id: Long): Result<Unit>
    suspend fun tagEntryToFestival(entryId: Long, festivalBudgetId: Long?): Result<Unit>
}
