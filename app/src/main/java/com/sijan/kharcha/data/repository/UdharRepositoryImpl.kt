package com.sijan.kharcha.data.repository

import androidx.room.withTransaction
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.dao.AuditLogDao
import com.sijan.kharcha.data.db.dao.UdharDao
import com.sijan.kharcha.data.db.entity.AuditLogEntity
import com.sijan.kharcha.data.db.entity.UdharEntity
import com.sijan.kharcha.data.db.entity.UdharSettlementEntity
import com.sijan.kharcha.domain.model.UdharRecord
import com.sijan.kharcha.domain.model.UdharSettlement
import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharSummary
import com.sijan.kharcha.domain.model.UdharType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UdharRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val udharDao: UdharDao,
    private val auditLogDao: AuditLogDao
) : UdharRepository {

    override fun getAllUdhars(): Flow<List<UdharRecord>> {
        return udharDao.getAllUdhars().map { list -> list.map { it.toDomain() } }
    }

    override fun getUdharsByType(type: UdharType): Flow<List<UdharRecord>> {
        return udharDao.getUdharsByType(type.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getUdharsByStatus(status: UdharStatus): Flow<List<UdharRecord>> {
        return udharDao.getUdharsByStatus(status.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getUdharById(id: Long): Flow<UdharRecord?> {
        return udharDao.getUdharById(id).map { it?.toDomain() }
    }

    override fun getSettlementsForUdhar(udharId: Long): Flow<List<UdharSettlement>> {
        return udharDao.getSettlementsForUdhar(udharId).map { list -> list.map { it.toDomain() } }
    }

    override fun getUdharSummary(): Flow<UdharSummary> {
        return getAllUdhars().map { udhars ->
            var lent = 0L
            var borrowed = 0L
            var settled = 0L

            for (u in udhars) {
                settled += u.settledPaisa
                if (!u.isFullySettled) {
                    if (u.type == UdharType.LENT) {
                        lent += u.remainingPaisa
                    } else {
                        borrowed += u.remainingPaisa
                    }
                }
            }

            UdharSummary(
                totalLentPaisa = lent,
                totalBorrowedPaisa = borrowed,
                totalSettledPaisa = settled,
                netReceivablePaisa = lent - borrowed
            )
        }
    }

    override suspend fun createUdhar(
        personName: String,
        phoneNumber: String?,
        type: UdharType,
        amountPaisa: Long,
        dueDate: Long?,
        notes: String
    ): Result<Long> = runCatching {
        require(personName.isNotBlank()) { "Person name cannot be blank" }
        require(amountPaisa > 0L) { "Udhar amount must be greater than zero" }

        val now = System.currentTimeMillis()
        val entity = UdharEntity(
            personName = personName.trim(),
            phoneNumber = phoneNumber?.trim()?.takeIf { it.isNotBlank() },
            type = type.name,
            amountPaisa = amountPaisa,
            settledPaisa = 0L,
            dueDate = dueDate,
            notes = notes.trim(),
            status = UdharStatus.PENDING.name,
            createdAt = now,
            updatedAt = now
        )

        val id = udharDao.insertUdhar(entity)
        auditLogDao.insertLog(
            AuditLogEntity(
                entityType = "UDHAR",
                entityId = id,
                action = "CREATE",
                details = "Created ${type.name} udhar for $personName: paisa=$amountPaisa"
            )
        )
        id
    }

    override suspend fun updateUdhar(
        id: Long,
        personName: String,
        phoneNumber: String?,
        dueDate: Long?,
        notes: String
    ): Result<Unit> = runCatching {
        require(personName.isNotBlank()) { "Person name cannot be blank" }

        val existing = udharDao.getUdharByIdDirect(id)
            ?: throw NoSuchElementException("Udhar with ID $id not found")

        val updated = existing.copy(
            personName = personName.trim(),
            phoneNumber = phoneNumber?.trim()?.takeIf { it.isNotBlank() },
            dueDate = dueDate,
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        )

        udharDao.updateUdhar(updated)
        auditLogDao.insertLog(
            AuditLogEntity(
                entityType = "UDHAR",
                entityId = id,
                action = "UPDATE",
                details = "Updated udhar for $personName"
            )
        )
    }

    override suspend fun recordSettlement(
        udharId: Long,
        amountPaisa: Long,
        dateEpochMillis: Long,
        notes: String?
    ): Result<Long> = runCatching {
        require(amountPaisa > 0L) { "Settlement amount must be greater than zero" }

        database.withTransaction {
            val udhar = udharDao.getUdharByIdDirect(udharId)
                ?: throw NoSuchElementException("Udhar with ID $udharId not found")

            val settlementEntity = UdharSettlementEntity(
                udharId = udharId,
                amountPaisa = amountPaisa,
                dateEpochMillis = dateEpochMillis,
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis()
            )
            val settlementId = udharDao.insertSettlement(settlementEntity)

            val allSettlements = udharDao.getSettlementsForUdharDirect(udharId)
            val totalSettled = allSettlements.sumOf { it.amountPaisa }

            val newStatus = if (totalSettled >= udhar.amountPaisa) {
                UdharStatus.SETTLED
            } else {
                UdharStatus.PARTIAL
            }

            val updatedUdhar = udhar.copy(
                settledPaisa = totalSettled,
                status = newStatus.name,
                updatedAt = System.currentTimeMillis()
            )
            udharDao.updateUdhar(updatedUdhar)

            auditLogDao.insertLog(
                AuditLogEntity(
                    entityType = "UDHAR_SETTLEMENT",
                    entityId = settlementId,
                    action = "SETTLE",
                    details = "Settled paisa=$amountPaisa on udhar $udharId (${udhar.personName})"
                )
            )

            settlementId
        }
    }

    override suspend fun deleteSettlement(settlementId: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val settlement = udharDao.getSettlementById(settlementId)
                ?: return@withTransaction

            val udharId = settlement.udharId
            udharDao.deleteSettlement(settlementId)

            val udhar = udharDao.getUdharByIdDirect(udharId) ?: return@withTransaction
            val remainingSettlements = udharDao.getSettlementsForUdharDirect(udharId)
            val totalSettled = remainingSettlements.sumOf { it.amountPaisa }

            val newStatus = when {
                totalSettled >= udhar.amountPaisa -> UdharStatus.SETTLED
                totalSettled > 0L -> UdharStatus.PARTIAL
                else -> UdharStatus.PENDING
            }

            val updatedUdhar = udhar.copy(
                settledPaisa = totalSettled,
                status = newStatus.name,
                updatedAt = System.currentTimeMillis()
            )
            udharDao.updateUdhar(updatedUdhar)

            auditLogDao.insertLog(
                AuditLogEntity(
                    entityType = "UDHAR_SETTLEMENT",
                    entityId = settlementId,
                    action = "DELETE",
                    details = "Deleted settlement $settlementId on udhar $udharId"
                )
            )
        }
    }

    override suspend fun deleteUdhar(id: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val udhar = udharDao.getUdharByIdDirect(id)
                ?: throw NoSuchElementException("Udhar with ID $id not found")

            udharDao.deleteUdhar(id)
            auditLogDao.insertLog(
                AuditLogEntity(
                    entityType = "UDHAR",
                    entityId = id,
                    action = "DELETE",
                    details = "Deleted udhar $id for ${udhar.personName}"
                )
            )
        }
    }
}
