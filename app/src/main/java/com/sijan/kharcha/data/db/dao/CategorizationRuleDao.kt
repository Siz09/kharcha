package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sijan.kharcha.data.db.entity.CategorizationRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorizationRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategorizationRuleEntity): Long

    @Update
    suspend fun updateRule(rule: CategorizationRuleEntity)

    @Delete
    suspend fun deleteRule(rule: CategorizationRuleEntity)

    @Query("SELECT * FROM categorization_rules WHERE id = :id LIMIT 1")
    fun getRuleById(id: Long): Flow<CategorizationRuleEntity?>

    @Query("SELECT * FROM categorization_rules ORDER BY priority DESC, id ASC")
    fun getAllRules(): Flow<List<CategorizationRuleEntity>>

    @Query("SELECT * FROM categorization_rules WHERE isActive = 1 ORDER BY priority DESC, id ASC")
    suspend fun getActiveRulesSync(): List<CategorizationRuleEntity>

    @Query("DELETE FROM categorization_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM categorization_rules")
    suspend fun deleteAllRules()
}
