package com.sijan.kharcha.domain.model

enum class FestivalType {
    DASHAIN,
    TIHAR,
    CHHATH,
    TEEJ,
    LOSAR,
    EID,
    CHRISTMAS,
    OTHER
}

data class FestivalBudget(
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
    init {
        require(name.isNotBlank()) { "Festival name cannot be blank" }
        require(targetBudgetPaisa > 0L) { "Target budget in paisa must be positive" }
        require(startDateEpoch <= endDateEpoch) { "Start date must be before or equal to end date" }
    }
}

data class FestivalBudgetProgress(
    val budget: FestivalBudget,
    val spentPaisa: Long,
    val remainingPaisa: Long,
    val percentageUsed: Int,
    val isOverBudget: Boolean,
    val entryCount: Int
)
