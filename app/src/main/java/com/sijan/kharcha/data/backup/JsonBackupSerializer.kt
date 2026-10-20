package com.sijan.kharcha.data.backup

import com.sijan.kharcha.data.backup.model.ArchiveManifest
import com.sijan.kharcha.data.backup.model.BackupPayload
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

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
     * Validates schemaVersion and record invariants.
     */
    fun deserialize(inputStream: InputStream): Result<BackupPayload> = runCatching {
        val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val payload = json.decodeFromString(BackupPayload.serializer(), jsonString)

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

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
