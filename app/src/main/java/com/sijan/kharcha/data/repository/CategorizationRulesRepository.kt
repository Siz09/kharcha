package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.CategorizationRule
import kotlinx.coroutines.flow.Flow

interface CategorizationRulesRepository {
    fun getAllRules(): Flow<List<CategorizationRule>>
    fun getRuleById(id: Long): Flow<CategorizationRule?>
    suspend fun saveRule(rule: CategorizationRule): Result<Long>
    suspend fun deleteRule(id: Long): Result<Unit>
    suspend fun suggestCategory(note: String): Long?
    suspend fun applyRulesToEntries(): Result<Int>
}
