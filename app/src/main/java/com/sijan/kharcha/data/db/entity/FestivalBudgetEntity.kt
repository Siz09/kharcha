package com.sijan.kharcha.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sijan.kharcha.domain.model.FestivalBudget
import com.sijan.kharcha.domain.model.FestivalType

@Entity(
    tableName = "festival_budgets",
    indices = [
        Index("festivalType"),
        Index("startDateEpoch")
    ]
)
data class FestivalBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val festivalType: FestivalType = FestivalType.DASHAIN,
    val targetBudgetPaisa: Long,
    val startDateEpoch: Long,
    val endDateEpoch: Long,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): FestivalBudget = FestivalBudget(
        id = id,
        name = name,
        festivalType = festivalType,
        targetBudgetPaisa = targetBudgetPaisa,
        startDateEpoch = startDateEpoch,
        endDateEpoch = endDateEpoch,
        notes = notes,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(budget: FestivalBudget): FestivalBudgetEntity = FestivalBudgetEntity(
            id = budget.id,
            name = budget.name,
            festivalType = budget.festivalType,
            targetBudgetPaisa = budget.targetBudgetPaisa,
            startDateEpoch = budget.startDateEpoch,
            endDateEpoch = budget.endDateEpoch,
            notes = budget.notes,
            isArchived = budget.isArchived,
            createdAt = budget.createdAt,
            updatedAt = budget.updatedAt
        )
    }
}
