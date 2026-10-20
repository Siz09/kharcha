package com.sijan.kharcha.data.backup

import com.sijan.kharcha.data.backup.model.ArchiveSummary
import java.io.InputStream
import java.io.OutputStream

interface BackupService {

    /**
     * Serializes current database state into JSON schema v1 and writes to output stream.
     * Returns total number of exported entries.
     */
    suspend fun exportBackup(outputStream: OutputStream): Result<Int>

    /**
     * Atomically restores database state from JSON schema v1 input stream.
     * If validation or restoration fails, existing data remains completely untouched.
     */
    suspend fun restoreBackup(inputStream: InputStream): Result<BackupSummary>

    /**
     * Exports human-readable CSV of all entries to output stream.
     * Returns total number of exported rows.
     */
    suspend fun exportCsv(outputStream: OutputStream): Result<Int>

    /**
     * Packages a full .kharcha ZIP archive containing manifest.json (with asset SHA-256 hashes),
     * database.json (complete Room tables snapshot), and assets/ (receipt images and document attachments).
     */
    suspend fun exportArchive(outputStream: OutputStream): Result<ArchiveSummary>

    /**
     * Pre-flight staging and checksum integrity verification of a .kharcha ZIP archive.
     * Unpacks archive into an isolated staging cache directory, verifies manifest and SHA-256
     * hashes of all bundled assets, and deserializes database.json without touching Room database.
     */
    suspend fun stageArchiveRestore(inputStream: InputStream): Result<BackupSummary>

    /**
     * Executes atomic multi-table database restoration and moves verified staged assets
     * into app-private receipts and documents directories.
     * If an error occurs, rolls back transaction and keeps existing data and assets untouched.
     */
    suspend fun confirmArchiveRestore(): Result<BackupSummary>

    /**
     * Cleans up any staging files if restore is cancelled or fails.
     */
    suspend fun clearRestoreStaging()
}
