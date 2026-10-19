package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.SavingsContribution

@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("goalId"),
        Index("dateEpochMillis")
    ]
)
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val goalId: Long,
    val amountPaisa: Long,
    val dateEpochMillis: Long,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SavingsContribution = SavingsContribution(
        id = id,
        goalId = goalId,
        amountPaisa = amountPaisa,
        dateMillis = dateEpochMillis,
        notes = notes,
        createdAtMillis = createdAt
    )

    companion object {
        fun fromDomain(contribution: SavingsContribution): SavingsContributionEntity = SavingsContributionEntity(
            id = contribution.id,
            goalId = contribution.goalId,
            amountPaisa = contribution.amountPaisa,
            dateEpochMillis = contribution.dateMillis,
            notes = contribution.notes,
            createdAt = contribution.createdAtMillis
        )
    }
}
