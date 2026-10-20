package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.model.EntryType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ParsedCsvRow(
    val rowNumber: Int,
    val amountPaisa: Long,
    val type: EntryType,
    val dateEpochMillis: Long,
    val categoryName: String,
    val note: String,
    val transactionId: String?,
    val isDuplicate: Boolean = false,
    val errorMessage: String? = null
)

data class CsvImportPreview(
    val totalRows: Int,
    val validRows: List<ParsedCsvRow>,
    val duplicateRows: List<ParsedCsvRow>,
    val errorRows: List<ParsedCsvRow>
)

object CsvStatementParser {

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
    )

    fun parse(
        inputStream: InputStream,
        existingTransactionIds: Set<String> = emptySet()
    ): CsvImportPreview {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return CsvImportPreview(0, emptyList(), emptyList(), emptyList())
        }

        val headerRow = parseCsvLine(lines.first())
        val columnMap = mapHeaders(headerRow)

        val validRows = mutableListOf<ParsedCsvRow>()
        val duplicateRows = mutableListOf<ParsedCsvRow>()
        val errorRows = mutableListOf<ParsedCsvRow>()

        for (i in 1 until lines.size) {
            val rawLine = lines[i]
            val tokens = parseCsvLine(rawLine)
            val rowNum = i + 1

            if (tokens.isEmpty()) continue

            try {
                // Date
                val rawDate = getColumnValue(tokens, columnMap, "date")
                val dateMillis = parseDate(rawDate)
                    ?: throw IllegalArgumentException("Invalid date format: '$rawDate'")

                // Amount
                val rawAmount = getColumnValue(tokens, columnMap, "amount")
                val amountPaisa = Normalizer.parsePaisa(rawAmount)
                    ?: throw IllegalArgumentException("Invalid amount: '$rawAmount'")

                // Type
                val rawType = getColumnValue(tokens, columnMap, "type")
                val type = parseType(rawType)

                // Category
                val categoryName = getColumnValue(tokens, columnMap, "category").ifBlank { "General" }

                // Note / Narration
                val note = getColumnValue(tokens, columnMap, "note").ifBlank { "Imported Entry" }

                // Transaction ID
                val txnId = getColumnValue(tokens, columnMap, "transaction_id").ifBlank { null }

                val isDup = txnId != null && existingTransactionIds.contains(txnId.trim())

                val parsedRow = ParsedCsvRow(
                    rowNumber = rowNum,
                    amountPaisa = amountPaisa,
                    type = type,
                    dateEpochMillis = dateMillis,
                    categoryName = categoryName,
                    note = note,
                    transactionId = txnId?.trim(),
                    isDuplicate = isDup
                )

                if (isDup) {
                    duplicateRows.add(parsedRow)
                } else {
                    validRows.add(parsedRow)
                }
            } catch (e: Exception) {
                errorRows.add(
                    ParsedCsvRow(
                        rowNumber = rowNum,
                        amountPaisa = 0L,
                        type = EntryType.EXPENSE,
                        dateEpochMillis = 0L,
                        categoryName = "",
                        note = rawLine,
                        transactionId = null,
                        errorMessage = e.message ?: "Unknown error"
                    )
                )
            }
        }

        return CsvImportPreview(
            totalRows = lines.size - 1,
            validRows = validRows,
            duplicateRows = duplicateRows,
            errorRows = errorRows
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    private fun mapHeaders(headers: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        headers.forEachIndexed { index, header ->
            val h = header.lowercase().trim()
            when {
                h.contains("date") || h.contains("time") -> map.putIfAbsent("date", index)
                h.contains("amount") || h.contains("debit") || h.contains("credit") -> map.putIfAbsent("amount", index)
                h.contains("type") || h.contains("dr/cr") -> map.putIfAbsent("type", index)
                h.contains("category") -> map.putIfAbsent("category", index)
                h.contains("note") || h.contains("remark") || h.contains("desc") || h.contains("narrat") || h.contains("particular") -> map.putIfAbsent("note", index)
                h.contains("trans") || h.contains("ref") || h.contains("txn") -> map.putIfAbsent("transaction_id", index)
            }
        }
        return map
    }

    private fun getColumnValue(tokens: List<String>, columnMap: Map<String, Int>, key: String): String {
        val index = columnMap[key] ?: return ""
        return tokens.getOrElse(index) { "" }
    }

    private fun parseType(rawType: String): EntryType {
        val t = rawType.uppercase().trim()
        return when {
            t.contains("INC") || t.contains("CR") || t.contains("CREDIT") || t.contains("RECEIVED") -> EntryType.INCOME
            t.contains("TRANS") -> EntryType.TRANSFER
            else -> EntryType.EXPENSE
        }
    }

    private fun parseDate(rawDate: String): Long? {
        val cleaned = rawDate.trim()
        val epoch = cleaned.toLongOrNull()
        if (epoch != null && epoch > 946684800000L) { // > year 2000 in millis
            return epoch
        }

        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (_: Exception) {}
            try {
                return LocalDateTime.parse(cleaned, formatter).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: Exception) {}
        }
        return null
    }
}
