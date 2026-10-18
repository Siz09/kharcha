package com.sijan.kharcha.domain.model

data class LedgerEntry(
    val id: Long = 0L,
    val amountPaisa: Long,
    val type: EntryType,
    val categoryId: Long,
    val note: String = "",
    val date: Long,
    val source: String = "manual",
    val receiptPath: String? = null,
    val rawOcrText: String? = null,
    val transactionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(amountPaisa > 0) { "Ledger entry amount in paisa must be strictly greater than 0" }
    }
}
