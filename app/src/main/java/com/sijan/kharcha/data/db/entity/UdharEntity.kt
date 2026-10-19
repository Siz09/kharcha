package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.UdharRecord
import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharType

@Entity(
    tableName = "udhars",
    indices = [
        Index("personName"),
        Index("type"),
        Index("status"),
        Index("dueDate")
    ]
)
data class UdharEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val personName: String,
    val phoneNumber: String? = null,
    val type: String,
    val amountPaisa: Long,
    val settledPaisa: Long = 0L,
    val dueDate: Long? = null,
    val notes: String = "",
    val status: String = UdharStatus.PENDING.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): UdharRecord = UdharRecord(
        id = id,
        personName = personName,
        phoneNumber = phoneNumber,
        type = try {
            UdharType.valueOf(type)
        } catch (_: Exception) {
            UdharType.LENT
        },
        amountPaisa = amountPaisa,
        settledPaisa = settledPaisa,
        dueDate = dueDate,
        notes = notes,
        status = try {
            UdharStatus.valueOf(status)
        } catch (_: Exception) {
            UdharStatus.PENDING
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(record: UdharRecord): UdharEntity = UdharEntity(
            id = record.id,
            personName = record.personName,
            phoneNumber = record.phoneNumber,
            type = record.type.name,
            amountPaisa = record.amountPaisa,
            settledPaisa = record.settledPaisa,
            dueDate = record.dueDate,
            notes = record.notes,
            status = record.status.name,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }
}
