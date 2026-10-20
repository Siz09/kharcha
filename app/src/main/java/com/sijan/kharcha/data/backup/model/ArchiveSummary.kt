package com.sijan.kharcha.data.backup.model

data class ArchiveSummary(
    val fileName: String,
    val archiveVersion: Int,
    val schemaVersion: Int,
    val createdAt: Long,
    val totalEntries: Int,
    val totalAssets: Int,
    val archiveSizeBytes: Long
)
