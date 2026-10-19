package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sijan.kharcha.data.db.entity.SavingsContributionEntity
import com.sijan.kharcha.data.db.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE isCompleted = 1 ORDER BY createdAt DESC")
    fun getCompletedGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getGoalById(id: Long): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalByIdDirect(id: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(entity: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(entities: List<SavingsGoalEntity>)

    @Update
    suspend fun updateGoal(entity: SavingsGoalEntity): Int

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long): Int

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllGoals(): Int

    // Contributions
    @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY dateEpochMillis DESC")
    fun getContributionsForGoal(goalId: Long): Flow<List<SavingsContributionEntity>>

    @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY dateEpochMillis DESC")
    suspend fun getContributionsForGoalDirect(goalId: Long): List<SavingsContributionEntity>

    @Query("SELECT * FROM savings_contributions ORDER BY dateEpochMillis DESC")
    fun getAllContributions(): Flow<List<SavingsContributionEntity>>

    @Query("SELECT * FROM savings_contributions WHERE id = :id")
    suspend fun getContributionById(id: Long): SavingsContributionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(entity: SavingsContributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(entities: List<SavingsContributionEntity>)

    @Query("DELETE FROM savings_contributions WHERE id = :id")
    suspend fun deleteContribution(id: Long): Int

    @Query("DELETE FROM savings_contributions WHERE goalId = :goalId")
    suspend fun deleteContributionsForGoal(goalId: Long): Int

    @Query("DELETE FROM savings_contributions")
    suspend fun deleteAllContributions(): Int
}
