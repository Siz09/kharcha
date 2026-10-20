package com.sijan.kharcha.domain.model

data class AccountReconciliation(
    val id: Long = 0L,
    val accountId: Long,
    val statementDate: Long,
    val statementBalancePaisa: Long,
    val computedBalancePaisa: Long,
    val differencePaisa: Long,
    val isReconciled: Boolean,
    val reconciledAt: Long,
    val notes: String? = null
)
