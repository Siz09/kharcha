package com.sijan.kharcha.domain.model

data class UtilityAccount(
    val id: Long = 0L,
    val utilityType: UtilityType,
    val providerName: String,
    val consumerId: String,
    val counterOrBranch: String? = null,
    val accountHolderName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
