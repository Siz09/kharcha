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

class EsewaParser @Inject constructor() : ReceiptParser {

    override val sourceName: String = "esewa"

    override fun canParse(ocr: OcrResult): Boolean {
        val lower = ocr.fullText.lowercase(Locale.ROOT)
        if (lower.contains("khalti") || lower.contains("fonepay")) return false
        return lower.contains("esewa") || (lower.contains("transaction id") && lower.contains("amount"))
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

    // TODO(parser-spec): Adjust amount keyword heuristics once expanded sample set is available
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
            if (line.contains("Rs", ignoreCase = true) || line.contains("NPR", ignoreCase = true) || line.contains("रु")) {
                val parsed = Normalizer.parsePaisa(line)
                if (parsed != null) {
                    return Validator.validateAmount(parsed, raw = rawLines[i])
                }
            }
        }

        return Validator.validateAmount(null, raw = "")
    }

    // TODO(parser-spec): Refine transaction ID regex patterns for merchant vs P2P transfers
    private fun extractTransactionId(
        spatial: SpatialLayoutExtractor,
        rawLines: List<String>,
        normalizedLines: List<String>
    ): ParsedField<String> {
        val txnKeyRegex = Regex("(?i).*(transaction\\s*id|txn\\s*id|ref\\s*id|transaction\\s*code).*")
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
                val inlineValue = line.replace(Regex("(?i).*(transaction\\s*id|txn\\s*id|ref\\s*id|transaction\\s*code)\\s*[:\\-]?"), "").trim()
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

        val standaloneTxnRegex = Regex("^[A-Z0-9]{8,20}$")
        for (i in normalizedLines.indices) {
            val candidate = normalizedLines[i].trim()
            if (standaloneTxnRegex.matches(candidate) && !candidate.startsWith("RS", ignoreCase = true)) {
                return Validator.validateTransactionId(candidate, raw = rawLines[i])
            }
        }

        return Validator.validateTransactionId(null, raw = "")
    }

    // TODO(parser-spec): Add BS date converters in v1.1 if eSewa emits Nepali Sambat
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
            SimpleDateFormat("dd MMM, yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
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

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i].trim()
            val parsed = parseDateWithFormats(line, dateFormats)
            if (parsed != null) {
                return Validator.validateDate(parsed, raw = rawLines[i])
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

    // TODO(parser-spec): Expand merchant labels (e.g., utility bills, ISP payments)
    private fun extractMerchant(
        spatial: SpatialLayoutExtractor,
        rawLines: List<String>,
        normalizedLines: List<String>
    ): ParsedField<String> {
        val merchantKeyRegex = Regex("(?i).*(paid\\s*to|recipient|merchant|bill\\s*details|\\bto\\b).*")
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
                val inlineValue = line.replace(Regex("(?i).*(paid\\s*to|recipient|merchant|bill\\s*details|\\bto\\b)\\s*[:\\-]?", RegexOption.IGNORE_CASE), "").trim()
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
        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            if (feeKeyRegex.matches(line)) {
                val inlineValue = line.replace(Regex("(?i).*(service\\s*charge|fee|commission)\\s*[:\\-]?"), "").trim()
                val parsed = Normalizer.parsePaisa(inlineValue)
                if (parsed != null) {
                    return ParsedField(value = parsed, raw = rawLines[i], status = ValidationStatus.VALID)
                }
                if (i + 1 < normalizedLines.size) {
                    val next = Normalizer.parsePaisa(normalizedLines[i + 1])
                    if (next != null) {
                        return ParsedField(value = next, raw = rawLines[i + 1], status = ValidationStatus.VALID)
                    }
                }
            }
        }

        return ParsedField(value = 0L, raw = "0", status = ValidationStatus.VALID)
    }
}
