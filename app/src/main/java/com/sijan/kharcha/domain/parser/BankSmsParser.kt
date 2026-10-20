package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.model.BankSmsTransaction
import com.sijan.kharcha.domain.model.EntryType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankSmsParser @Inject constructor() {

    private val otpPattern = Regex("(?i).*(otp|verification code|one time password|do not share).*")

    private val debitKeywords = Regex("(?i).*(debited|debited by|debited for|withdrawn|spent|transferred to|dr\\.?\\b|purchase at|paid|payment of|transfer to).*")
    private val creditKeywords = Regex("(?i).*(credited|credited by|credited with|deposited|received from|received|cr\\.?\\b).*")

    private val amountRegex = Regex("(?i)(?:NPR|Rs\\.?|रु\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:NPR|Rs\\.?|रु\\.?)")

    private val txnIdRegex = Regex("(?i)(?:Ref(?:\\s*No|\\s*ID)?|Txn(?:\\s*ID|\\s*No)?|Trace(?:\\s*ID)?|Code)\\s*[:\\-]?\\s*([A-Za-z0-9]+)")
    private val accountTailRegex = Regex("(?i)(?:A/C|Acct|Account)(?:\\s*No\\.?)?\\s*[:\\-]?\\s*(?:\\.{2,}|x+|\\*+)?([0-9]{3,6})")
    private val remarksRegex = Regex("(?i)(?:Remarks|Info)\\s*[:\\-]?\\s*([A-Za-z0-9\\s&'\\-]+?)(?=\\s*(?:[.,;\\n]|Txn|Ref|Avail|Bal|$))")
    private val atRegex = Regex("(?i)\\bat\\s+([A-Za-z0-9\\s&'\\-]+?)(?=\\s*(?:[.,;\\n]|Ref|Txn|Avail|Bal|$))")
    private val toForRegex = Regex("(?i)\\b(?:to|for|by)\\s+([A-Za-z0-9\\s&'\\-]+?)(?=\\s*(?:[.,;\\n]|is\\s+successful|has\\s+been|Ref|Txn|Avail|Bal|$))")

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("dd-MMM-yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    )

    fun parse(sender: String, messageBody: String): BankSmsTransaction? {
        val cleanBody = messageBody.trim()
        if (cleanBody.isBlank()) return null

        // 1. Filter out OTPs and authentication codes immediately
        if (otpPattern.matches(cleanBody)) return null

        // 2. Identify Bank Sender / Content
        val bankName = resolveBankName(sender, cleanBody) ?: return null

        // 3. Determine EntryType (debit vs credit)
        val entryType = when {
            debitKeywords.matches(cleanBody) -> EntryType.EXPENSE
            creditKeywords.matches(cleanBody) -> EntryType.INCOME
            else -> return null
        }

        // 4. Extract Amount
        val normalizedDigits = Normalizer.toAsciiDigits(cleanBody)
        val amountMatch = amountRegex.find(normalizedDigits) ?: return null
        val amountString = amountMatch.groupValues[1].ifBlank { amountMatch.groupValues[2] }
        val amountPaisa = Normalizer.parsePaisa(amountString) ?: return null
        if (amountPaisa <= 0L) return null

        // 5. Extract Transaction Reference ID
        val txnIdMatch = txnIdRegex.find(cleanBody)
        val transactionId = txnIdMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.length >= 4 }

        // 6. Extract Account Number Tail
        val acctMatch = accountTailRegex.find(cleanBody)
        val acctTail = acctMatch?.groupValues?.getOrNull(1)?.let { "...$it" }

        // 7. Extract Merchant / Remarks
        val merchant = extractMerchant(cleanBody)

        // 8. Extract Date
        val dateEpochMillis = extractDate(normalizedDigits) ?: System.currentTimeMillis()

        return BankSmsTransaction(
            bankName = bankName,
            amountPaisa = amountPaisa,
            type = entryType,
            dateEpochMillis = dateEpochMillis,
            transactionId = transactionId,
            merchant = merchant,
            accountNumberTail = acctTail,
            rawSms = cleanBody
        )
    }

    private fun resolveBankName(sender: String, body: String): String? {
        val upperSender = sender.uppercase(Locale.US)
        val upperBody = body.uppercase(Locale.US)

        return when {
            upperSender.contains("ESEWA") || upperBody.contains("ESEWA") -> "eSewa"
            upperSender.contains("KHALTI") || upperBody.contains("KHALTI") -> "Khalti"
            upperSender.contains("FONEPAY") || upperBody.contains("FONEPAY") -> "Fonepay"
            upperSender.contains("CZBIL") || upperSender.contains("CITIZEN") || upperBody.contains("CITIZENS BANK") || upperBody.contains("CITIZEN BANK") -> "Citizens Bank"
            upperSender.contains("NMB") || upperBody.contains("NMB BANK") -> "NMB Bank"
            upperSender.contains("NABIL") || upperBody.contains("NABIL") -> "Nabil Bank"
            upperSender.contains("NICASIA") || upperBody.contains("NIC ASIA") -> "NIC Asia Bank"
            upperSender.contains("GLOBAL") || upperSender.contains("GIBL") || upperBody.contains("GLOBAL IME") -> "Global IME Bank"
            upperSender.contains("NIMB") || upperBody.contains("INVESTMENT MEGA") -> "Nepal Investment Mega Bank"
            upperSender.contains("SANIMA") || upperBody.contains("SANIMA") -> "Sanima Bank"
            upperSender.contains("PRABHU") || upperBody.contains("PRABHU") -> "Prabhu Bank"
            upperSender.contains("RBB") || upperBody.contains("RASTRIYA BANIJYA") -> "Rastriya Banijya Bank"
            upperSender.contains("HBL") || upperBody.contains("HIMALAYAN BANK") -> "Himalayan Bank"
            upperSender.contains("SCB") || upperBody.contains("STANDARD CHARTERED") -> "Standard Chartered Bank"
            upperSender.contains("EBL") || upperBody.contains("EVEREST BANK") -> "Everest Bank"
            upperSender.contains("KBL") || upperBody.contains("KUMARI BANK") -> "Kumari Bank"
            upperSender.contains("SBL") || upperBody.contains("SIDDHARTHA BANK") -> "Siddhartha Bank"
            upperSender.contains("MBL") || upperBody.contains("MACHHAPUCHHRE") -> "Machhapuchhre Bank"
            upperSender.contains("PCBL") || upperBody.contains("PRIME COMMERCIAL") -> "Prime Commercial Bank"
            upperSender.contains("ADBL") || upperBody.contains("AGRICULTURE DEVELOPMENT") -> "Agricultural Development Bank"
            upperSender.contains("BANK") || upperBody.contains("A/C") || upperBody.contains("ACCOUNT") -> "Bank Alert"
            else -> null
        }
    }

    private fun extractDate(text: String): Long? {
        for (format in dateFormats) {
            val dateRegex = when (format.toPattern()) {
                "yyyy-MM-dd HH:mm:ss" -> Regex("\\b(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})\\b")
                "yyyy-MM-dd HH:mm" -> Regex("\\b(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2})\\b")
                "dd-MMM-yyyy HH:mm:ss" -> Regex("\\b(\\d{1,2}-[A-Za-z]{3}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})\\b")
                "dd-MMM-yyyy HH:mm" -> Regex("\\b(\\d{1,2}-[A-Za-z]{3}-\\d{4}\\s+\\d{2}:\\d{2})\\b")
                "dd/MM/yyyy HH:mm:ss" -> Regex("\\b(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})\\b")
                "dd/MM/yyyy HH:mm" -> Regex("\\b(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{2}:\\d{2})\\b")
                "dd-MM-yyyy HH:mm" -> Regex("\\b(\\d{1,2}-\\d{1,2}-\\d{4}\\s+\\d{2}:\\d{2})\\b")
                "dd-MMM-yyyy" -> Regex("\\b(\\d{1,2}-[A-Za-z]{3}-\\d{4})\\b")
                "dd/MM/yyyy" -> Regex("\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\b")
                "yyyy-MM-dd" -> Regex("\\b(\\d{4}-\\d{2}-\\d{2})\\b")
                else -> null
            } ?: continue

            val match = dateRegex.find(text)
            if (match != null) {
                try {
                    val parsed = format.parse(match.groupValues[1])
                    if (parsed != null) return parsed.time
                } catch (ignored: Exception) {
                }
            }
        }
        return null
    }

    private fun extractMerchant(body: String): String? {
        // Priority 1: Explicit Remarks / Info field
        val remarksMatch = remarksRegex.find(body)
        if (remarksMatch != null) {
            val merchant = remarksMatch.groupValues[1].trim()
            if (isValidMerchant(merchant)) return merchant
        }

        // Priority 2: "at <Merchant>"
        val atMatch = atRegex.find(body)
        if (atMatch != null) {
            val merchant = atMatch.groupValues[1].trim()
            if (isValidMerchant(merchant)) return merchant
        }

        // Priority 3: "to <Merchant>" or "for <Merchant>" or "by <Source>"
        val matches = toForRegex.findAll(body)
        for (match in matches) {
            val candidate = match.groupValues[1].trim()
            if (isValidMerchant(candidate)) return candidate
        }

        return null
    }

    private fun isValidMerchant(candidate: String): Boolean {
        if (candidate.length <= 2) return false
        val lower = candidate.lowercase(Locale.US).trim()
        val genericExclusions = setOf(
            "pos", "npr", "rs", "qr", "pos purchase", "qr payment",
            "purchase", "payment", "fund transfer", "withdrawal"
        )
        if (lower in genericExclusions) return false
        if (lower.startsWith("npr ") || lower.startsWith("rs ") || lower.startsWith("rs.")) return false
        if (candidate.all { it.isDigit() || it == ',' || it == '.' || it.isWhitespace() }) return false
        return true
    }
}

