package com.sijan.kharcha.data.repository

import androidx.room.withTransaction
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.dao.AuditLogDao
import com.sijan.kharcha.data.db.dao.SavingsGoalDao
import com.sijan.kharcha.data.db.entity.AuditLogEntity
import com.sijan.kharcha.data.db.entity.SavingsContributionEntity
import com.sijan.kharcha.data.db.entity.SavingsGoalEntity
import com.sijan.kharcha.domain.model.SavingsContribution
import com.sijan.kharcha.domain.model.SavingsGoal
import com.sijan.kharcha.domain.model.SavingsGoalSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val savingsGoalDao: SavingsGoalDao,
    private val auditLogDao: AuditLogDao
) : SavingsGoalRepository {

    override fun getAllGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getAllGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getActiveGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getCompletedGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getCompletedGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getGoalById(id: Long): Flow<SavingsGoal?> {
        return savingsGoalDao.getGoalById(id).map { it?.toDomain() }
    }

    override fun getContributionsForGoal(goalId: Long): Flow<List<SavingsContribution>> {
        return savingsGoalDao.getContributionsForGoal(goalId).map { list -> list.map { it.toDomain() } }
    }

    override fun getSummary(): Flow<SavingsGoalSummary> {
        return getAllGoals().map { goals ->
            var totalTarget = 0L
            var totalSaved = 0L
            var active = 0
            var completed = 0

            for (g in goals) {
                totalTarget += g.targetAmountPaisa
                totalSaved += g.savedAmountPaisa
                if (g.isCompleted) {
                    completed++
                } else {
                    active++
                }
            }

            SavingsGoalSummary(
                totalTargetPaisa = totalTarget,
                totalSavedPaisa = totalSaved,
                activeGoalsCount = active,
                completedGoalsCount = completed
            )
        }
    }

    override suspend fun createGoal(
        name: String,
        targetAmountPaisa: Long,
        targetDateMillis: Long?,
        notes: String?
    ): Long {
        require(name.isNotBlank()) { "Goal name cannot be blank" }
        require(targetAmountPaisa > 0L) { "Target amount must be positive" }

        val entity = SavingsGoalEntity(
            name = name.trim(),
            targetAmountPaisa = targetAmountPaisa,
            savedAmountPaisa = 0L,
            targetDateEpochMillis = targetDateMillis,
            notes = notes?.trim() ?: "",
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = savingsGoalDao.insertGoal(entity)
        auditLogDao.insertLog(
            AuditLogEntity(
                action = "CREATE_SAVINGS_GOAL",
                entityType = "SAVINGS_GOAL",
                entityId = id,
                details = "Created goal '${name.trim()}' for target $targetAmountPaisa paisa"
            )
        )
        return id
    }

    override suspend fun updateGoal(
        id: Long,
        name: String,
        targetAmountPaisa: Long,
        targetDateMillis: Long?,
        notes: String?,
        isCompleted: Boolean
    ) {
        require(name.isNotBlank()) { "Goal name cannot be blank" }
        require(targetAmountPaisa > 0L) { "Target amount must be positive" }

        database.withTransaction {
            val existing = savingsGoalDao.getGoalByIdDirect(id) ?: return@withTransaction
            val autoCompleted = isCompleted || (existing.savedAmountPaisa >= targetAmountPaisa)
            val updated = existing.copy(
                name = name.trim(),
                targetAmountPaisa = targetAmountPaisa,
                targetDateEpochMillis = targetDateMillis,
                notes = notes?.trim() ?: "",
                isCompleted = autoCompleted,
                updatedAt = System.currentTimeMillis()
            )
            savingsGoalDao.updateGoal(updated)
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "UPDATE_SAVINGS_GOAL",
                    entityType = "SAVINGS_GOAL",
                    entityId = id,
                    details = "Updated goal '${name.trim()}'"
                )
            )
        }
    }

    override suspend fun deleteGoal(id: Long) {
        database.withTransaction {
            val existing = savingsGoalDao.getGoalByIdDirect(id) ?: return@withTransaction
            savingsGoalDao.deleteGoal(id)
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "DELETE_SAVINGS_GOAL",
                    entityType = "SAVINGS_GOAL",
                    entityId = id,
                    details = "Deleted goal '${existing.name}'"
                )
            )
        }
    }

    override suspend fun addContribution(
        goalId: Long,
        amountPaisa: Long,
        dateMillis: Long,
        notes: String?
    ): Long {
        require(amountPaisa > 0L) { "Contribution amount must be positive" }

        return database.withTransaction {
            val goal = savingsGoalDao.getGoalByIdDirect(goalId)
                ?: error("Savings goal not found: $goalId")

            val contributionEntity = SavingsContributionEntity(
                goalId = goalId,
                amountPaisa = amountPaisa,
                dateEpochMillis = dateMillis,
                notes = notes?.trim(),
                createdAt = System.currentTimeMillis()
            )
            val contributionId = savingsGoalDao.insertContribution(contributionEntity)

            val allContributions = savingsGoalDao.getContributionsForGoalDirect(goalId)
            val newTotalSaved = allContributions.sumOf { it.amountPaisa }
            val isCompleted = newTotalSaved >= goal.targetAmountPaisa

            savingsGoalDao.updateGoal(
                goal.copy(
                    savedAmountPaisa = newTotalSaved,
                    isCompleted = isCompleted,
                    updatedAt = System.currentTimeMillis()
                )
            )

            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "ADD_SAVINGS_CONTRIBUTION",
                    entityType = "SAVINGS_GOAL",
                    entityId = goalId,
                    details = "Added deposit of $amountPaisa paisa to '${goal.name}'"
                )
            )

            contributionId
        }
    }

    override suspend fun deleteContribution(contributionId: Long) {
        database.withTransaction {
            val contrib = savingsGoalDao.getContributionById(contributionId) ?: return@withTransaction
            val goalId = contrib.goalId
            val goal = savingsGoalDao.getGoalByIdDirect(goalId) ?: return@withTransaction

            savingsGoalDao.deleteContribution(contributionId)

            val remainingContributions = savingsGoalDao.getContributionsForGoalDirect(goalId)
            val newTotalSaved = remainingContributions.sumOf { it.amountPaisa }
            val isCompleted = newTotalSaved >= goal.targetAmountPaisa

            savingsGoalDao.updateGoal(
                goal.copy(
                    savedAmountPaisa = newTotalSaved,
                    isCompleted = isCompleted,
                    updatedAt = System.currentTimeMillis()
                )
            )

            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "DELETE_SAVINGS_CONTRIBUTION",
                    entityType = "SAVINGS_GOAL",
                    entityId = goalId,
                    details = "Removed contribution of ${contrib.amountPaisa} paisa from '${goal.name}'"
                )
            )
        }
    }
}
