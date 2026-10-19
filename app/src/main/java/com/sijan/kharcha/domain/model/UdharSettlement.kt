package com.sijan.kharcha.domain.model

data class UdharSettlement(
    val id: Long = 0L,
    val udharId: Long,
    val amountPaisa: Long,
    val dateEpochMillis: Long,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
