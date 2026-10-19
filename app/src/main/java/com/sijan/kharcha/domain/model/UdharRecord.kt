package com.sijan.kharcha.domain.model

data class UdharRecord(
    val id: Long = 0L,
    val personName: String,
    val phoneNumber: String? = null,
    val type: UdharType,
    val amountPaisa: Long,
    val settledPaisa: Long = 0L,
    val dueDate: Long? = null,
    val notes: String = "",
    val status: UdharStatus = UdharStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val remainingPaisa: Long
        get() = (amountPaisa - settledPaisa).coerceAtLeast(0L)

    val isFullySettled: Boolean
        get() = settledPaisa >= amountPaisa
}
