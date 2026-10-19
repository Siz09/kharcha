package com.sijan.kharcha.domain.model

data class BankSmsTransaction(
    val bankName: String,
    val amountPaisa: Long,
    val type: EntryType,
    val dateEpochMillis: Long,
    val transactionId: String? = null,
    val merchant: String? = null,
    val accountNumberTail: String? = null,
    val rawSms: String
)
