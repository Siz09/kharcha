package com.sijan.kharcha.data.repository

import com.sijan.kharcha.data.db.dao.FestivalBudgetDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.entity.FestivalBudgetEntity
import com.sijan.kharcha.domain.model.FestivalBudget
import com.sijan.kharcha.domain.model.FestivalBudgetProgress
import com.sijan.kharcha.domain.model.LedgerEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FestivalBudgetRepositoryImpl @Inject constructor(
    private val festivalBudgetDao: FestivalBudgetDao,
    private val ledgerDao: LedgerDao
) : FestivalBudgetRepository {

    override fun getActiveFestivalBudgets(): Flow<List<FestivalBudgetProgress>> {
        return festivalBudgetDao.getActiveFestivalBudgets().flatMapLatest { entities ->
            if (entities.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val progressFlows = entities.map { entity ->
                val budget = entity.toDomain()
                combine(
                    festivalBudgetDao.getTotalSpentForFestivalBudget(budget.id),
                    festivalBudgetDao.getEntriesForFestivalBudget(budget.id)
                ) { totalSpent, entries ->
                    val spent = totalSpent ?: 0L
                    val remaining = budget.targetBudgetPaisa - spent
                    val pct = if (budget.targetBudgetPaisa > 0L) {
                        ((spent * 100L) / budget.targetBudgetPaisa).toInt()
                    } else 0
                    FestivalBudgetProgress(
                        budget = budget,
                        spentPaisa = spent,
                        remainingPaisa = remaining,
                        percentageUsed = pct,
                        isOverBudget = spent > budget.targetBudgetPaisa,
                        entryCount = entries.size
                    )
                }
            }
            combine(progressFlows) { it.toList() }
        }
    }

    override fun getAllFestivalBudgets(): Flow<List<FestivalBudget>> {
        return festivalBudgetDao.getAllFestivalBudgets().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getFestivalBudgetById(id: Long): Flow<FestivalBudget?> {
        return festivalBudgetDao.getFestivalBudgetById(id).map { it?.toDomain() }
    }

    override fun getEntriesForFestival(budgetId: Long): Flow<List<LedgerEntry>> {
        return festivalBudgetDao.getEntriesForFestivalBudget(budgetId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveFestivalBudget(budget: FestivalBudget): Result<Long> = runCatching {
        val entity = FestivalBudgetEntity.fromDomain(budget)
        festivalBudgetDao.insertFestivalBudget(entity)
    }

    override suspend fun deleteFestivalBudget(id: Long): Result<Unit> = runCatching {
        festivalBudgetDao.deleteFestivalBudgetById(id)
    }

    override suspend fun tagEntryToFestival(entryId: Long, festivalBudgetId: Long?): Result<Unit> = runCatching {
        val entry = ledgerDao.getEntryById(entryId).first() ?: throw IllegalArgumentException("Entry $entryId not found")
        val updated = entry.copy(festivalBudgetId = festivalBudgetId, updatedAt = System.currentTimeMillis())
        ledgerDao.updateEntry(updated)
    }
}
