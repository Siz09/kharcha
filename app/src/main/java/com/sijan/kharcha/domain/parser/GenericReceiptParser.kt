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

class GenericReceiptParser @Inject constructor() : ReceiptParser {

    override val sourceName: String = "receipt"

    override fun canParse(ocr: OcrResult): Boolean {
        val lower = ocr.fullText.lowercase(Locale.ROOT)
        if (lower.contains("esewa") || lower.contains("khalti") || lower.contains("fonepay")) return false
        return lower.contains("total") ||
            lower.contains("bill") ||
            lower.contains("invoice") ||
            lower.contains("pan no") ||
            lower.contains("vat no")
    }

    override fun parse(ocr: OcrResult): ParseResult {
        val spatial = SpatialLayoutExtractor(ocr)
        val rawLines = ocr.lines.map { it.trim() }.filter { it.isNotEmpty() }
        val normalizedLines = rawLines.map { Normalizer.toAsciiDigits(it) }

        val amountField = extractAmount(spatial, rawLines, normalizedLines)
        val txnIdField = extractTransactionId(spatial, rawLines, normalizedLines)
        val dateField = extractDate(spatial, rawLines, normalizedLines)
        val merchantField = extractMerchant(rawLines)

        return ParseResult(
            source = sourceName,
            amountPaisa = amountField,
            dateEpochMillis = dateField,
            transactionId = txnIdField,
            merchant = merchantField,
            feePaisa = ParsedField(0L, raw = "0", status = ValidationStatus.VALID),
            rawOcrText = ocr.fullText
        )
    }

    private fun extractAmount(
        spatial: SpatialLayoutExtractor,
        rawLines: List<String>,
        normalizedLines: List<String>
    ): ParsedField<Long> {
        val grandTotalRegex = Regex("(?i).*(grand\\s*total|net\\s*amount|total\\s*amount|total).*")
        if (spatial.hasBoundingBoxes) {
            val spatialMatch = spatial.extractField(grandTotalRegex) { candidate ->
                val clean = Normalizer.toAsciiDigits(candidate)
                Normalizer.parsePaisa(clean)
            }
            if (spatialMatch != null) {
                return Validator.validateAmount(spatialMatch.first, raw = spatialMatch.second)
            }
        }

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            if (grandTotalRegex.matches(line)) {
                val inlineValue = line.replace(Regex("(?i).*(grand\\s*total|net\\s*amount|total\\s*amount|total)\\s*[:\\-]?"), "").trim()
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
        val invoiceKeyRegex = Regex("(?i).*(bill\\s*no|invoice\\s*no|receipt\\s*no|tax\\s*invoice).*")
        if (spatial.hasBoundingBoxes) {
            val spatialMatch = spatial.extractField(invoiceKeyRegex) { candidate ->
                val clean = Normalizer.toAsciiDigits(candidate).trim()
                if (clean.length >= 3) clean else null
            }
            if (spatialMatch != null) {
                return Validator.validateTransactionId(spatialMatch.first, raw = spatialMatch.second)
            }
        }

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            if (invoiceKeyRegex.matches(line)) {
                val inlineValue = line.replace(Regex("(?i).*(bill\\s*no|invoice\\s*no|receipt\\s*no|tax\\s*invoice)\\s*[:\\-]?"), "").trim()
                if (inlineValue.length >= 3) {
                    return Validator.validateTransactionId(inlineValue, raw = rawLines[i])
                }
                if (i + 1 < normalizedLines.size) {
                    val nextLine = normalizedLines[i + 1].trim()
                    if (nextLine.length >= 3 && !nextLine.contains(" ")) {
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
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("dd MMM, yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        )

        val dateKeyRegex = Regex("(?i).*(date\\s*&?\\s*time|date|dated).*")
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
                val inlineValue = line.replace(Regex("(?i).*(date\\s*&?\\s*time|date|dated)\\s*[:\\-]?"), "").trim()
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

    private fun extractMerchant(rawLines: List<String>): ParsedField<String> {
        // Typically the header/first non-empty line of a paper receipt is the store/restaurant name
        val firstLine = rawLines.firstOrNull { it.isNotBlank() && it.length > 2 }
        return if (firstLine != null) {
            ParsedField(value = firstLine, raw = firstLine, status = ValidationStatus.VALID)
        } else {
            ParsedField(value = "", raw = "", status = ValidationStatus.WARNING)
        }
    }
}
