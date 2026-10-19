package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.SavingsGoal

@Entity(
    tableName = "savings_goals",
    indices = [
        Index("name"),
        Index("isCompleted"),
        Index("targetDateEpochMillis")
    ]
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val targetAmountPaisa: Long,
    val savedAmountPaisa: Long = 0L,
    val targetDateEpochMillis: Long? = null,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SavingsGoal = SavingsGoal(
        id = id,
        name = name,
        targetAmountPaisa = targetAmountPaisa,
        savedAmountPaisa = savedAmountPaisa,
        targetDateMillis = targetDateEpochMillis,
        notes = notes.ifEmpty { null },
        isCompleted = isCompleted,
        createdAtMillis = createdAt
    )

    companion object {
        fun fromDomain(goal: SavingsGoal): SavingsGoalEntity = SavingsGoalEntity(
            id = goal.id,
            name = goal.name,
            targetAmountPaisa = goal.targetAmountPaisa,
            savedAmountPaisa = goal.savedAmountPaisa,
            targetDateEpochMillis = goal.targetDateMillis,
            notes = goal.notes ?: "",
            isCompleted = goal.isCompleted,
            createdAt = goal.createdAtMillis,
            updatedAt = System.currentTimeMillis()
        )
    }
}
