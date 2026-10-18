package com.sijan.kharcha.domain.model

import java.util.Locale

object Money {

    private const val CURRENCY_PREFIX = "Rs. "

    /**
     * Formats an integer amount of paisa into a display string.
     * Example: 15050L -> "Rs. 150.50"
     */
    fun formatPaisa(paisa: Long): String {
        require(paisa >= 0) { "Money amount in paisa cannot be negative" }
        val rupees = paisa / 100
        val remainingPaisa = paisa % 100
        val formattedNumber = String.format(Locale.US, "%,d.%02d", rupees, remainingPaisa)
        return "$CURRENCY_PREFIX$formattedNumber"
    }

    /**
     * Parses user input or OCR text into paisa (Long).
     * Rejects negative numbers, invalid formats, or numbers with more than 2 decimal places.
     * Returns null if parsing fails.
     */
    fun parsePaisa(input: String): Long? {
        val cleaned = input.trim()
            .removePrefix("Rs.")
            .removePrefix("Rs")
            .removePrefix("RS.")
            .removePrefix("RS")
            .replace(",", "")
            .trim()

        if (cleaned.isEmpty() || cleaned.startsWith("-")) {
            return null
        }

        val parts = cleaned.split(".")
        return when (parts.size) {
            1 -> {
                val rupees = parts[0].toLongOrNull() ?: return null
                if (rupees < 0) null else rupees * 100
            }
            2 -> {
                val rupees = if (parts[0].isEmpty()) 0L else parts[0].toLongOrNull() ?: return null
                val decimalPart = parts[1]
                if (decimalPart.length > 2 || rupees < 0) {
                    return null
                }
                val paisa = when (decimalPart.length) {
                    1 -> (decimalPart.toLongOrNull() ?: return null) * 10
                    2 -> decimalPart.toLongOrNull() ?: return null
                    else -> return null
                }
                rupees * 100 + paisa
            }
            else -> null
        }
    }
}
