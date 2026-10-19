package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.SavingsContribution
import com.sijan.kharcha.domain.model.SavingsGoal
import com.sijan.kharcha.domain.model.SavingsGoalSummary
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getAllGoals(): Flow<List<SavingsGoal>>
    fun getActiveGoals(): Flow<List<SavingsGoal>>
    fun getCompletedGoals(): Flow<List<SavingsGoal>>
    fun getGoalById(id: Long): Flow<SavingsGoal?>
    fun getContributionsForGoal(goalId: Long): Flow<List<SavingsContribution>>
    fun getSummary(): Flow<SavingsGoalSummary>

    suspend fun createGoal(
        name: String,
        targetAmountPaisa: Long,
        targetDateMillis: Long? = null,
        notes: String? = null
    ): Long

    suspend fun updateGoal(
        id: Long,
        name: String,
        targetAmountPaisa: Long,
        targetDateMillis: Long? = null,
        notes: String? = null,
        isCompleted: Boolean = false
    )

    suspend fun deleteGoal(id: Long)

    suspend fun addContribution(
        goalId: Long,
        amountPaisa: Long,
        dateMillis: Long = System.currentTimeMillis(),
        notes: String? = null
    ): Long

    suspend fun deleteContribution(contributionId: Long)
}
