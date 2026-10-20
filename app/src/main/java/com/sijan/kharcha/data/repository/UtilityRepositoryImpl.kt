package com.sijan.kharcha.data.repository

import com.sijan.kharcha.data.db.dao.UtilityAccountDao
import com.sijan.kharcha.data.db.entity.UtilityAccountEntity
import com.sijan.kharcha.domain.model.UtilityAccount
import com.sijan.kharcha.domain.model.UtilityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UtilityRepositoryImpl @Inject constructor(
    private val utilityAccountDao: UtilityAccountDao
) : UtilityRepository {

    override fun getAllUtilityAccounts(): Flow<List<UtilityAccount>> {
        return utilityAccountDao.getAllUtilityAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUtilityAccountsByType(type: UtilityType): Flow<List<UtilityAccount>> {
        return utilityAccountDao.getUtilityAccountsByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUtilityAccountById(id: Long): Flow<UtilityAccount?> {
        return utilityAccountDao.getUtilityAccountById(id).map { it?.toDomain() }
    }

    override suspend fun saveUtilityAccount(account: UtilityAccount): Result<Long> {
        return runCatching {
            utilityAccountDao.insertUtilityAccount(UtilityAccountEntity.fromDomain(account))
        }
    }

    override suspend fun updateUtilityAccount(account: UtilityAccount): Result<Unit> {
        return runCatching {
            utilityAccountDao.updateUtilityAccount(UtilityAccountEntity.fromDomain(account.copy(updatedAt = System.currentTimeMillis())))
        }
    }

    override suspend fun deleteUtilityAccount(id: Long): Result<Unit> {
        return runCatching {
            utilityAccountDao.deleteUtilityAccountById(id)
        }
    }
}
