package com.sijan.kharcha.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupSavingsContribution(
    val id: Long,
    val goalId: Long,
    val amountPaisa: Long,
    val dateEpochMillis: Long,
    val notes: String? = null,
    val createdAt: Long = 0L
)
