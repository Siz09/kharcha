package com.sijan.kharcha.domain.model

data class SavingsContribution(
    val id: Long = 0L,
    val goalId: Long,
    val amountPaisa: Long,
    val dateMillis: Long,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
