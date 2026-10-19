package com.sijan.kharcha.domain.model

data class SavingsGoal(
    val id: Long = 0L,
    val name: String,
    val targetAmountPaisa: Long,
    val savedAmountPaisa: Long = 0L,
    val targetDateMillis: Long? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val remainingPaisa: Long
        get() = (targetAmountPaisa - savedAmountPaisa).coerceAtLeast(0L)

    val progressPercent: Int
        get() = if (targetAmountPaisa > 0L) {
            ((savedAmountPaisa * 100L) / targetAmountPaisa).toInt().coerceIn(0, 100)
        } else {
            100
        }

    val isTargetReached: Boolean
        get() = savedAmountPaisa >= targetAmountPaisa
}
