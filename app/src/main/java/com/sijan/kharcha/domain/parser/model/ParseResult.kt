package com.sijan.kharcha.domain.parser.model

data class ParseResult(
    val source: String,
    val amountPaisa: ParsedField<Long>,
    val dateEpochMillis: ParsedField<Long>,
    val transactionId: ParsedField<String>,
    val merchant: ParsedField<String>,
    val feePaisa: ParsedField<Long> = ParsedField(0L),
    val rawOcrText: String = "",
    val receiptPath: String? = null
) {
    val isValid: Boolean
        get() = amountPaisa.status != ValidationStatus.ERROR &&
                dateEpochMillis.status != ValidationStatus.ERROR &&
                transactionId.status != ValidationStatus.ERROR
}
