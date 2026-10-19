package com.sijan.kharcha.domain.model

data class SmsPrefillData(
    val amountPaisa: Long,
    val note: String,
    val type: String,
    val date: Long,
    val transactionId: String? = null,
    val source: String
)
