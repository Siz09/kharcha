package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.UdharSettlement

@Entity(
    tableName = "udhar_settlements",
    foreignKeys = [
        ForeignKey(
            entity = UdharEntity::class,
            parentColumns = ["id"],
            childColumns = ["udharId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("udharId"),
        Index("dateEpochMillis")
    ]
)
data class UdharSettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val udharId: Long,
    val amountPaisa: Long,
    val dateEpochMillis: Long,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): UdharSettlement = UdharSettlement(
        id = id,
        udharId = udharId,
        amountPaisa = amountPaisa,
        dateEpochMillis = dateEpochMillis,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(settlement: UdharSettlement): UdharSettlementEntity = UdharSettlementEntity(
            id = settlement.id,
            udharId = settlement.udharId,
            amountPaisa = settlement.amountPaisa,
            dateEpochMillis = settlement.dateEpochMillis,
            notes = settlement.notes,
            createdAt = settlement.createdAt
        )
    }
}
