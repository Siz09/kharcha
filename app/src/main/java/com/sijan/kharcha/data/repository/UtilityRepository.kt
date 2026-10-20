package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.UtilityAccount
import com.sijan.kharcha.domain.model.UtilityType
import kotlinx.coroutines.flow.Flow

interface UtilityRepository {
    fun getAllUtilityAccounts(): Flow<List<UtilityAccount>>
    fun getUtilityAccountsByType(type: UtilityType): Flow<List<UtilityAccount>>
    fun getUtilityAccountById(id: Long): Flow<UtilityAccount?>
    suspend fun saveUtilityAccount(account: UtilityAccount): Result<Long>
    suspend fun updateUtilityAccount(account: UtilityAccount): Result<Unit>
    suspend fun deleteUtilityAccount(id: Long): Result<Unit>
}
