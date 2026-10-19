package com.sijan.kharcha.data.backup.model

import com.sijan.kharcha.domain.model.AccountType
import kotlinx.serialization.Serializable

@Serializable
data class BackupAccount(
    val id: Long,
    val name: String,
    val type: AccountType,
    val institutionName: String? = null,
    val accountNumberLast4: String? = null,
    val openingBalancePaisa: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
