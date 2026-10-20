package com.sijan.kharcha.data.backup

import com.sijan.kharcha.data.backup.model.ArchiveManifest
import com.sijan.kharcha.data.backup.model.BackupAccount
import com.sijan.kharcha.data.backup.model.BackupCategory
import com.sijan.kharcha.data.backup.model.BackupEntry
import com.sijan.kharcha.data.backup.model.BackupPayload
import com.sijan.kharcha.domain.model.AccountType
import com.sijan.kharcha.domain.model.EntryType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class JsonBackupSerializer @Inject constructor() {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
    }

    /**
     * Serializes BackupPayload into UTF-8 JSON bytes.
     */
    fun serializeToBytes(payload: BackupPayload): Result<ByteArray> = runCatching {
        val jsonString = json.encodeToString(BackupPayload.serializer(), payload)
        jsonString.toByteArray(Charsets.UTF_8)
    }

    /**
     * Serializes ArchiveManifest into UTF-8 JSON.
     */
    fun serializeManifest(manifest: ArchiveManifest): String {
        return json.encodeToString(ArchiveManifest.serializer(), manifest)
    }

    /**
     * Deserializes UTF-8 JSON into ArchiveManifest.
     */
    fun deserializeManifest(jsonString: String): Result<ArchiveManifest> = runCatching {
        json.decodeFromString(ArchiveManifest.serializer(), jsonString)
    }

    /**
     * Serializes BackupPayload into UTF-8 JSON and writes to output stream.
     */
    fun serialize(payload: BackupPayload, outputStream: OutputStream): Result<Unit> = runCatching {
        val jsonString = json.encodeToString(BackupPayload.serializer(), payload)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(jsonString)
            writer.flush()
        }
    }

    /**
     * Deserializes UTF-8 JSON from input stream into BackupPayload.
     * Supports both modern BackupPayload schema and legacy Kharcha export format (`{"transactions": [...]}`).
     * Validates schemaVersion and record invariants.
     */
    fun deserialize(inputStream: InputStream): Result<BackupPayload> = runCatching {
        val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

        val payload = if (jsonString.contains("\"transactions\"") && !jsonString.contains("\"schemaVersion\"")) {
            parseLegacyExport(jsonString) ?: throw IllegalArgumentException("Failed to parse legacy export format")
        } else {
            json.decodeFromString(BackupPayload.serializer(), jsonString)
        }

        // Validate schema version
        if (payload.schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException(
                "Unsupported backup schema version: ${payload.schemaVersion}. App supports up to version $CURRENT_SCHEMA_VERSION."
            )
        }

        // Validate entries
        for (entry in payload.entries) {
            require(entry.amountPaisa > 0) {
                "Invalid entry amount in paisa: ${entry.amountPaisa}. Must be strictly positive."
            }
        }

        // Validate categories
        for (category in payload.categories) {
            require(category.name.isNotBlank()) {
                "Invalid category with blank name."
            }
        }

        payload
    }

    /**
     * Parses legacy export format (`{"transactions": [...]}`) into standard BackupPayload.
     */
    private fun parseLegacyExport(jsonString: String): BackupPayload? {
        val root = try {
            json.parseToJsonElement(jsonString).jsonObject
        } catch (e: Exception) {
            return null
        }
        val transactionsArray = root["transactions"]?.jsonArray ?: return null

        val categoriesMap = mutableMapOf<Pair<String, EntryType>, Long>()
        var nextCatId = 1L
        val entries = mutableListOf<BackupEntry>()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (elem in transactionsArray) {
            val obj = elem.jsonObject
            val id = obj["id"]?.jsonPrimitive?.longOrNull ?: continue
            val amountDouble = obj["amount"]?.jsonPrimitive?.doubleOrNull ?: continue
            if (amountDouble <= 0.0) continue
            val amountPaisa = (amountDouble * 100.0).roundToLong()

            val typeStr = obj["type"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "EXPENSE"
            val type = if (typeStr == "INCOME") EntryType.INCOME else EntryType.EXPENSE

            val catName = obj["category"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { "Other" } ?: "Other"
            val catKey = catName to type
            val categoryId = categoriesMap.getOrPut(catKey) { nextCatId++ }

            val dateStr = obj["date"]?.jsonPrimitive?.contentOrNull
            val createdAt = obj["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L
            val dateEpoch = if (createdAt > 0L) {
                createdAt
            } else if (!dateStr.isNullOrBlank()) {
                try {
                    dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }

            var note = obj["note"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
            val remarks = obj["remarks"]?.jsonPrimitive?.contentOrNull?.trim()
            if (!remarks.isNullOrBlank() && !note.contains(remarks)) {
                note = if (note.isBlank()) remarks else "$note ($remarks)"
            }

            // Skip manual monthly rollover entries created as phantom income in legacy app
            val isManualRollover = type == EntryType.INCOME && (
                note.contains("previous month", ignoreCase = true) ||
                note.contains("previous mounth", ignoreCase = true) ||
                note.contains("leftover", ignoreCase = true) ||
                note.contains("rollover", ignoreCase = true)
            )
            if (isManualRollover) continue

            val legacyMeta = mutableListOf<String>()
            val paymentMethod = obj["paymentMethod"]?.jsonPrimitive?.contentOrNull?.trim()
            if (!paymentMethod.isNullOrBlank()) legacyMeta.add("paymentMethod: $paymentMethod")
            val processedBy = obj["processedBy"]?.jsonPrimitive?.contentOrNull?.trim()
            if (!processedBy.isNullOrBlank()) legacyMeta.add("processedBy: $processedBy")
            val purpose = obj["purpose"]?.jsonPrimitive?.contentOrNull?.trim()
            if (!purpose.isNullOrBlank()) legacyMeta.add("purpose: $purpose")
            val receiverName = obj["receiverName"]?.jsonPrimitive?.contentOrNull?.trim()
            if (!receiverName.isNullOrBlank()) legacyMeta.add("receiverName: $receiverName")
            val receiverId = obj["receiverId"]?.jsonPrimitive?.contentOrNull?.trim()
            if (!receiverId.isNullOrBlank()) legacyMeta.add("receiverId: $receiverId")
            val initiatorName = obj["initiatorName"]?.jsonPrimitive?.contentOrNull?.trim()
            if (!initiatorName.isNullOrBlank()) legacyMeta.add("initiatorName: $initiatorName")
            val mood = obj["mood"]?.jsonPrimitive?.contentOrNull?.trim()
            if (!mood.isNullOrBlank()) legacyMeta.add("mood: $mood")

            val rawOcrText = if (legacyMeta.isNotEmpty()) legacyMeta.joinToString(" | ") else null
            val imagePath = obj["imagePath"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
            val transactionCode = obj["transactionCode"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }

            entries.add(
                BackupEntry(
                    id = id,
                    amountPaisa = amountPaisa,
                    type = type,
                    categoryId = categoryId,
                    accountId = 1L,
                    toAccountId = null,
                    note = note,
                    date = dateEpoch,
                    source = if (imagePath != null) "scan" else "manual",
                    receiptPath = imagePath,
                    rawOcrText = rawOcrText,
                    transactionId = transactionCode,
                    isTaxDeductible = false,
                    taxCategory = null,
                    documentType = null,
                    createdAt = if (createdAt > 0L) createdAt else dateEpoch,
                    updatedAt = if (createdAt > 0L) createdAt else dateEpoch
                )
            )
        }

        val categories = categoriesMap.map { (key, id) ->
            BackupCategory(id = id, name = key.first, type = key.second)
        }

        val accounts = listOf(
            BackupAccount(
                id = 1L,
                name = "Cash in Hand",
                type = AccountType.CASH,
                openingBalancePaisa = 0L,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        return BackupPayload(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            categories = categories,
            entries = entries,
            accounts = accounts
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
