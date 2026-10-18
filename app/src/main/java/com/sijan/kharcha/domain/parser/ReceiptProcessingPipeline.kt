package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.parser.model.OcrResult
import com.sijan.kharcha.domain.parser.model.ParseResult
import com.sijan.kharcha.domain.parser.model.ParsedField
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptProcessingPipeline @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards ReceiptParser>
) {
    fun process(ocr: OcrResult, imagePath: String? = null): ParseResult {
        for (parser in parsers) {
            if (parser.canParse(ocr)) {
                return parser.parse(ocr).copy(
                    receiptPath = imagePath,
                    rawOcrText = ocr.fullText
                )
            }
        }

        return ParseResult(
            source = "unknown",
            amountPaisa = ParsedField(null, raw = "", status = ValidationStatus.ERROR, errorMessage = "Could not detect amount"),
            dateEpochMillis = ParsedField(System.currentTimeMillis(), raw = "", status = ValidationStatus.VALID),
            transactionId = ParsedField(null, raw = "", status = ValidationStatus.WARNING, errorMessage = "No transaction ID detected"),
            merchant = ParsedField("", raw = "", status = ValidationStatus.WARNING),
            feePaisa = ParsedField(0L, raw = "0", status = ValidationStatus.VALID),
            rawOcrText = ocr.fullText,
            receiptPath = imagePath
        )
    }
}
