package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sijan.kharcha.data.db.entity.UtilityAccountEntity
import com.sijan.kharcha.domain.model.UtilityType
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilityAccountDao {

    @Query("SELECT * FROM utility_accounts ORDER BY utilityType ASC, providerName ASC")
    fun getAllUtilityAccounts(): Flow<List<UtilityAccountEntity>>

    @Query("SELECT * FROM utility_accounts WHERE utilityType = :type ORDER BY providerName ASC")
    fun getUtilityAccountsByType(type: UtilityType): Flow<List<UtilityAccountEntity>>

    @Query("SELECT * FROM utility_accounts WHERE id = :id")
    fun getUtilityAccountById(id: Long): Flow<UtilityAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUtilityAccount(account: UtilityAccountEntity): Long

    @Update
    suspend fun updateUtilityAccount(account: UtilityAccountEntity)

    @Delete
    suspend fun deleteUtilityAccount(account: UtilityAccountEntity)

    @Query("DELETE FROM utility_accounts WHERE id = :id")
    suspend fun deleteUtilityAccountById(id: Long)

    @Query("DELETE FROM utility_accounts")
    suspend fun deleteAllUtilityAccounts()
}
