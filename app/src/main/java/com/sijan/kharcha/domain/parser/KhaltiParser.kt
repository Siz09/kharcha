package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.parser.layout.SpatialLayoutExtractor
import com.sijan.kharcha.domain.parser.model.OcrResult
import com.sijan.kharcha.domain.parser.model.ParseResult
import com.sijan.kharcha.domain.parser.model.ParsedField
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class KhaltiParser @Inject constructor() : ReceiptParser {

    override val sourceName: String = "khalti"

    override fun canParse(ocr: OcrResult): Boolean {
        val lower = ocr.fullText.lowercase(Locale.ROOT)
        return lower.contains("khalti") || (lower.contains("transaction id") && lower.contains("cashback"))
    }

    override fun parse(ocr: OcrResult): ParseResult {
        val spatial = SpatialLayoutExtractor(ocr)
        val rawLines = ocr.lines.map { it.trim() }.filter { it.isNotEmpty() }
        val normalizedLines = rawLines.map { Normalizer.toAsciiDigits(it) }

        val amountField = extractAmount(spatial, rawLines, normalizedLines)
        val txnIdField = extractTransactionId(spatial, rawLines, normalizedLines)
        val dateField = extractDate(spatial, rawLines, normalizedLines)
        val merchantField = extractMerchant(spatial, rawLines, normalizedLines)
        val feeField = extractFee(spatial, rawLines, normalizedLines)

        return ParseResult(
            source = sourceName,
            amountPaisa = amountField,
            dateEpochMillis = dateField,
            transactionId = txnIdField,
            merchant = merchantField,
            feePaisa = feeField,
            rawOcrText = ocr.fullText
        )
    }

    private fun extractAmount(
        spatial: SpatialLayoutExtractor,
        rawLines: List<String>,
        normalizedLines: List<String>
    ): ParsedField<Long> {
        val amountKeyRegex = Regex("(?i).*(total\\s*amount|amount|paid\\s*amount).*")
        if (spatial.hasBoundingBoxes) {
            val spatialMatch = spatial.extractField(amountKeyRegex) { candidate ->
                val clean = Normalizer.toAsciiDigits(candidate)
                Normalizer.parsePaisa(clean)
            }
            if (spatialMatch != null) {
                return Validator.validateAmount(spatialMatch.first, raw = spatialMatch.second)
            }
        }

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            if (amountKeyRegex.matches(line)) {
                val inlineValue = line.replace(Regex("(?i).*(total\\s*amount|amount|paid\\s*amount)\\s*[:\\-]?"), "").trim()
                val parsedInline = Normalizer.parsePaisa(inlineValue)
                if (parsedInline != null) {
                    return Validator.validateAmount(parsedInline, raw = rawLines[i])
                }
                if (i + 1 < normalizedLines.size) {
                    val nextLine = normalizedLines[i + 1]
                    val parsedNext = Normalizer.parsePaisa(nextLine)
                    if (parsedNext != null) {
                        return Validator.validateAmount(parsedNext, raw = rawLines[i + 1])
                    }
                }
            }
        }

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            if (line.contains("Rs", ignoreCase = true) || line.contains("NPR", ignoreCase = true)) {
                val parsed = Normalizer.parsePaisa(line)
                if (parsed != null) {
                    return Validator.validateAmount(parsed, raw = rawLines[i])
                }
            }
        }

        return Validator.validateAmount(null, raw = "")
    }

    private fun extractTransactionId(
        spatial: SpatialLayoutExtractor,
        rawLines: List<String>,
        normalizedLines: List<String>
    ): ParsedField<String> {
        val txnKeyRegex = Regex("(?i).*(transaction\\s*id|txn\\s*id|transaction\\s*no).*")
        if (spatial.hasBoundingBoxes) {
            val spatialMatch = spatial.extractField(txnKeyRegex) { candidate ->
                val clean = Normalizer.toAsciiDigits(candidate).trim()
                if (clean.length >= 6) clean else null
            }
            if (spatialMatch != null) {
                return Validator.validateTransactionId(spatialMatch.first, raw = spatialMatch.second)
            }
        }

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            if (txnKeyRegex.matches(line)) {
                val inlineValue = line.replace(Regex("(?i).*(transaction\\s*id|txn\\s*id|transaction\\s*no)\\s*[:\\-]?"), "").trim()
                if (inlineValue.length >= 6) {
                    return Validator.validateTransactionId(inlineValue, raw = rawLines[i])
                }
                if (i + 1 < normalizedLines.size) {
                    val nextLine = normalizedLines[i + 1].trim()
                    if (nextLine.length >= 6 && !nextLine.contains(" ")) {
                        return Validator.validateTransactionId(nextLine, raw = rawLines[i + 1])
                    }
                }
            }
        }

        return Validator.validateTransactionId(null, raw = "")
    }

    private fun extractDate(
        spatial: SpatialLayoutExtractor,
        rawLines: List<String>,
        normalizedLines: List<String>
    ): ParsedField<Long> {
        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("dd MMM, yyyy HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("dd MMM, yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        )

        val dateKeyRegex = Regex("(?i).*(date\\s*&?\\s*time|date|time).*")
        if (spatial.hasBoundingBoxes) {
            val spatialMatch = spatial.extractField(dateKeyRegex) { candidate ->
                val clean = Normalizer.toAsciiDigits(candidate).trim()
                parseDateWithFormats(clean, dateFormats)
            }
            if (spatialMatch != null) {
                return Validator.validateDate(spatialMatch.first, raw = spatialMatch.second)
            }
        }

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            if (dateKeyRegex.matches(line)) {
                val inlineValue = line.replace(Regex("(?i).*(date\\s*&?\\s*time|date|time)\\s*[:\\-]?"), "").trim()
                val parsedInline = parseDateWithFormats(inlineValue, dateFormats)
                if (parsedInline != null) {
                    return Validator.validateDate(parsedInline, raw = rawLines[i])
                }
                if (i + 1 < normalizedLines.size) {
                    val nextLine = normalizedLines[i + 1].trim()
                    val parsedNext = parseDateWithFormats(nextLine, dateFormats)
                    if (parsedNext != null) {
                        return Validator.validateDate(parsedNext, raw = rawLines[i + 1])
                    }
                }
            }
        }

        return Validator.validateDate(null, raw = "")
    }

    private fun parseDateWithFormats(text: String, formats: List<SimpleDateFormat>): Long? {
        if (text.isBlank()) return null
        for (format in formats) {
            try {
                val parsed = format.parse(text)
                if (parsed != null) return parsed.time
            } catch (ignored: Exception) {
            }
        }
        return null
    }

    private fun extractMerchant(
        spatial: SpatialLayoutExtractor,
        rawLines: List<String>,
        normalizedLines: List<String>
    ): ParsedField<String> {
        val merchantKeyRegex = Regex("(?i).*(paid\\s*to|recipient|merchant|to).*")
        if (spatial.hasBoundingBoxes) {
            val spatialMatch = spatial.extractField(merchantKeyRegex) { candidate ->
                val clean = candidate.trim()
                if (clean.isNotBlank()) clean else null
            }
            if (spatialMatch != null) {
                return ParsedField(value = spatialMatch.first, raw = spatialMatch.second, status = ValidationStatus.VALID)
            }
        }

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            if (merchantKeyRegex.matches(line)) {
                val inlineValue = line.replace(Regex("(?i).*(paid\\s*to|recipient|merchant|to)\\s*[:\\-]?", RegexOption.IGNORE_CASE), "").trim()
                if (inlineValue.isNotBlank()) {
                    return ParsedField(value = inlineValue, raw = rawLines[i], status = ValidationStatus.VALID)
                }
                if (i + 1 < rawLines.size) {
                    val nextLine = rawLines[i + 1].trim()
                    if (nextLine.isNotBlank()) {
                        return ParsedField(value = nextLine, raw = rawLines[i + 1], status = ValidationStatus.VALID)
                    }
                }
            }
        }

        return ParsedField(value = "", raw = "", status = ValidationStatus.WARNING)
    }

    private fun extractFee(
        spatial: SpatialLayoutExtractor,
        rawLines: List<String>,
        normalizedLines: List<String>
    ): ParsedField<Long> {
        val feeKeyRegex = Regex("(?i).*(service\\s*charge|fee|commission).*")
        if (spatial.hasBoundingBoxes) {
            val spatialMatch = spatial.extractField(feeKeyRegex) { candidate ->
                val clean = Normalizer.toAsciiDigits(candidate).trim()
                Normalizer.parsePaisa(clean)
            }
            if (spatialMatch != null) {
                return ParsedField(value = spatialMatch.first, raw = spatialMatch.second, status = ValidationStatus.VALID)
            }
        }

        return ParsedField(value = 0L, raw = "0", status = ValidationStatus.VALID)
    }
}
