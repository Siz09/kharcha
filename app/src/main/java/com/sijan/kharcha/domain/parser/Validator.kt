package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.parser.model.ParsedField
import com.sijan.kharcha.domain.parser.model.ValidationStatus

object Validator {

    private const val ONE_DAY_MILLIS = 86_400_000L
    private const val TEN_YEARS_MILLIS = 10L * 365 * ONE_DAY_MILLIS

    fun validateAmount(paisa: Long?, raw: String = ""): ParsedField<Long> {
        return if (paisa == null || paisa <= 0L) {
            ParsedField(
                value = null,
                raw = raw,
                status = ValidationStatus.ERROR,
                errorMessage = "Amount must be strictly greater than zero"
            )
        } else {
            ParsedField(
                value = paisa,
                raw = raw,
                status = ValidationStatus.VALID
            )
        }
    }

    fun validateDate(epochMillis: Long?, raw: String = ""): ParsedField<Long> {
        if (epochMillis == null || epochMillis <= 0L) {
            return ParsedField(
                value = null,
                raw = raw,
                status = ValidationStatus.ERROR,
                errorMessage = "Date is missing or invalid"
            )
        }

        val now = System.currentTimeMillis()
        return when {
            epochMillis > now + ONE_DAY_MILLIS -> {
                ParsedField(
                    value = epochMillis,
                    raw = raw,
                    status = ValidationStatus.ERROR,
                    errorMessage = "Date cannot be in the future"
                )
            }
            epochMillis < now - TEN_YEARS_MILLIS -> {
                ParsedField(
                    value = epochMillis,
                    raw = raw,
                    status = ValidationStatus.WARNING,
                    errorMessage = "Date is more than 10 years ago"
                )
            }
            else -> {
                ParsedField(
                    value = epochMillis,
                    raw = raw,
                    status = ValidationStatus.VALID
                )
            }
        }
    }

    fun validateTransactionId(id: String?, raw: String = ""): ParsedField<String> {
        return if (id.isNullOrBlank()) {
            ParsedField(
                value = null,
                raw = raw,
                status = ValidationStatus.ERROR,
                errorMessage = "Transaction ID is required"
            )
        } else {
            ParsedField(
                value = id.trim(),
                raw = raw,
                status = ValidationStatus.VALID
            )
        }
    }

    fun isFeeConsistent(totalPaisa: Long, amountPaisa: Long, feePaisa: Long): Boolean {
        return totalPaisa == amountPaisa + feePaisa
    }
}
