package com.sijan.kharcha.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val categories: List<BackupCategory>,
    val entries: List<BackupEntry>
)
