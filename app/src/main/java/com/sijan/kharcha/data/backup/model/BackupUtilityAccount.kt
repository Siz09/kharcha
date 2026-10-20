package com.sijan.kharcha.data.backup.model

import com.sijan.kharcha.domain.model.UtilityType
import kotlinx.serialization.Serializable

@Serializable
data class BackupUtilityAccount(
    val id: Long,
    val utilityType: UtilityType,
    val providerName: String,
    val consumerId: String,
    val counterOrBranch: String? = null,
    val accountHolderName: String? = null,
    val notes: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
