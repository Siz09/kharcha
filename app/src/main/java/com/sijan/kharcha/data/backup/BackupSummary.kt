package com.sijan.kharcha.data.backup

data class BackupSummary(
    val schemaVersion: Int,
    val exportedAt: Long,
    val categoryCount: Int,
    val entryCount: Int,
    val assetCount: Int = 0
)
