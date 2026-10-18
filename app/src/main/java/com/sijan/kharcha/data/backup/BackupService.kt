package com.sijan.kharcha.data.backup

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
}
