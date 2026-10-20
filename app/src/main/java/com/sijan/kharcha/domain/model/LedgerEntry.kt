package com.sijan.kharcha.domain.model

data class LedgerEntry(
    val id: Long = 0L,
    val amountPaisa: Long,
    val type: EntryType,
    val categoryId: Long,
    val accountId: Long = 1L,
    val toAccountId: Long? = null,
    val note: String = "",
    val date: Long,
    val source: String = "manual",
    val receiptPath: String? = null,
    val rawOcrText: String? = null,
    val transactionId: String? = null,
    val isTaxDeductible: Boolean = false,
    val taxCategory: TaxCategory? = null,
    val documentType: DocumentType? = null,
    val isReconciled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(amountPaisa > 0) { "Ledger entry amount in paisa must be strictly greater than 0" }
        if (type == EntryType.TRANSFER) {
            require(toAccountId != null) { "Transfer requires destination account" }
            require(toAccountId != accountId) { "Source and destination accounts must be different" }
        }
    }
}
