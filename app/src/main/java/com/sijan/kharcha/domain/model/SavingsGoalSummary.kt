package com.sijan.kharcha.domain.model

data class SavingsGoalSummary(
    val totalTargetPaisa: Long = 0L,
    val totalSavedPaisa: Long = 0L,
    val activeGoalsCount: Int = 0,
    val completedGoalsCount: Int = 0
) {
    val remainingPaisa: Long
        get() = (totalTargetPaisa - totalSavedPaisa).coerceAtLeast(0L)

    val overallProgressPercent: Int
        get() = if (totalTargetPaisa > 0L) {
            ((totalSavedPaisa * 100L) / totalTargetPaisa).toInt().coerceIn(0, 100)
        } else {
            0
        }
}
