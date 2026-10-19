package com.sijan.kharcha.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupUdharSettlement(
    val id: Long,
    val udharId: Long,
    val amountPaisa: Long,
    val dateEpochMillis: Long,
    val notes: String? = null,
    val createdAt: Long = 0L
)
