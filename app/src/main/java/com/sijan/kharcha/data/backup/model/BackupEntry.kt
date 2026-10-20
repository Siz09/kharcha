package com.sijan.kharcha.data.backup.model

import com.sijan.kharcha.domain.model.DocumentType
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.TaxCategory
import kotlinx.serialization.Serializable

@Serializable
data class BackupEntry(
    val id: Long,
    val amountPaisa: Long,
    val type: EntryType,
    val categoryId: Long,
    val accountId: Long = 1L,
    val toAccountId: Long? = null,
    val note: String = "",
    val date: Long,
    val source: String = "manual",
    val receiptPath: String? = null,
    val rawOcrText: String? = null,
    val transactionId: String? = null,
    val isTaxDeductible: Boolean = false,
    val taxCategory: TaxCategory? = null,
    val documentType: DocumentType? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
