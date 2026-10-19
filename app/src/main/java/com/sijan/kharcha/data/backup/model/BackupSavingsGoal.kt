package com.sijan.kharcha.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupSavingsGoal(
    val id: Long,
    val name: String,
    val targetAmountPaisa: Long,
    val savedAmountPaisa: Long = 0L,
    val targetDateEpochMillis: Long? = null,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
