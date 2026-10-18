package com.sijan.kharcha.data.backup.model

import com.sijan.kharcha.domain.model.EntryType
import kotlinx.serialization.Serializable

@Serializable
data class BackupEntry(
    val id: Long,
    val amountPaisa: Long,
    val type: EntryType,
    val categoryId: Long,
    val note: String = "",
    val date: Long,
    val source: String = "manual",
    val receiptPath: String? = null,
    val rawOcrText: String? = null,
    val transactionId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
