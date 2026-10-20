package com.sijan.kharcha.data.repository

import androidx.room.withTransaction
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.dao.CategorizationRuleDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.entity.CategorizationRuleEntity
import com.sijan.kharcha.domain.model.CategorizationRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorizationRulesRepositoryImpl @Inject constructor(
    private val categorizationRuleDao: CategorizationRuleDao,
    private val ledgerDao: LedgerDao,
    private val appDatabase: AppDatabase
) : CategorizationRulesRepository {

    override fun getAllRules(): Flow<List<CategorizationRule>> {
        return categorizationRuleDao.getAllRules().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRuleById(id: Long): Flow<CategorizationRule?> {
        return categorizationRuleDao.getRuleById(id).map { it?.toDomain() }
    }

    override suspend fun saveRule(rule: CategorizationRule): Result<Long> = runCatching {
        val entity = CategorizationRuleEntity.fromDomain(rule)
        categorizationRuleDao.insertRule(entity)
    }

    override suspend fun deleteRule(id: Long): Result<Unit> = runCatching {
        categorizationRuleDao.deleteRuleById(id)
    }

    override suspend fun suggestCategory(note: String): Long? {
        if (note.isBlank()) return null
        val activeRules = categorizationRuleDao.getActiveRulesSync().map { it.toDomain() }
        val matchingRule = activeRules.firstOrNull { it.matches(note) }
        return matchingRule?.categoryId
    }

    override suspend fun applyRulesToEntries(): Result<Int> = runCatching {
        val activeRules = categorizationRuleDao.getActiveRulesSync().map { it.toDomain() }
        if (activeRules.isEmpty()) return@runCatching 0

        val entries = ledgerDao.getAllEntries().first()
        var updatedCount = 0

        appDatabase.withTransaction {
            for (entry in entries) {
                if (entry.note.isBlank()) continue
                val matchedRule = activeRules.firstOrNull { it.matches(entry.note) }
                if (matchedRule != null && matchedRule.categoryId != entry.categoryId) {
                    val updated = entry.copy(
                        categoryId = matchedRule.categoryId,
                        updatedAt = System.currentTimeMillis()
                    )
                    ledgerDao.updateEntry(updated)
                    updatedCount++
                }
            }
        }
        updatedCount
    }
}
