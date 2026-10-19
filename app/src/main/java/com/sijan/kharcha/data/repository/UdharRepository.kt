package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.UdharRecord
import com.sijan.kharcha.domain.model.UdharSettlement
import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharSummary
import com.sijan.kharcha.domain.model.UdharType
import kotlinx.coroutines.flow.Flow

interface UdharRepository {
    fun getAllUdhars(): Flow<List<UdharRecord>>
    fun getUdharsByType(type: UdharType): Flow<List<UdharRecord>>
    fun getUdharsByStatus(status: UdharStatus): Flow<List<UdharRecord>>
    fun getUdharById(id: Long): Flow<UdharRecord?>
    fun getSettlementsForUdhar(udharId: Long): Flow<List<UdharSettlement>>
    fun getUdharSummary(): Flow<UdharSummary>

    suspend fun createUdhar(
        personName: String,
        phoneNumber: String?,
        type: UdharType,
        amountPaisa: Long,
        dueDate: Long?,
        notes: String
    ): Result<Long>

    suspend fun updateUdhar(
        id: Long,
        personName: String,
        phoneNumber: String?,
        dueDate: Long?,
        notes: String
    ): Result<Unit>

    suspend fun recordSettlement(
        udharId: Long,
        amountPaisa: Long,
        dateEpochMillis: Long,
        notes: String?
    ): Result<Long>

    suspend fun deleteSettlement(settlementId: Long): Result<Unit>

    suspend fun deleteUdhar(id: Long): Result<Unit>
}
