package com.sijan.kharcha.domain.parser

object Normalizer {

    private val CURRENCY_REGEX = Regex("(?i)rs\\.?|npr|रु\\.?|रू\\.?")
    private val VALID_MONEY_REGEX = Regex("^\\d+(\\.\\d{1,2})?$")

    fun toAsciiDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '\u0966'..'\u096F') {
                sb.append((ch.code - 0x0966 + '0'.code).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun cleanNumericString(text: String): String {
        val ascii = toAsciiDigits(text)
        return ascii
            .replace(CURRENCY_REGEX, "")
            .replace(",", "")
            .trim()
    }

    fun fixNumericLookalikes(text: String): String {
        return text
            .replace('O', '0')
            .replace('o', '0')
            .replace('I', '1')
            .replace('l', '1')
            .replace('S', '5')
            .replace('s', '5')
    }

    fun parsePaisa(amountText: String): Long? {
        if (amountText.isBlank()) return null
        val cleaned = cleanNumericString(amountText)
        if (!VALID_MONEY_REGEX.matches(cleaned)) return null

        val parts = cleaned.split('.')
        val rupees = parts[0].toLongOrNull() ?: return null

        val cents = if (parts.size == 2) {
            val decimals = parts[1]
            when (decimals.length) {
                1 -> (decimals.toIntOrNull() ?: return null) * 10
                2 -> decimals.toIntOrNull() ?: return null
                else -> return null
            }
        } else {
            0
        }

        val totalPaisa = rupees * 100L + cents
        return if (totalPaisa > 0) totalPaisa else null
    }
}
