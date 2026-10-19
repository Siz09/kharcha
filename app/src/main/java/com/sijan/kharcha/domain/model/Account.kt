package com.sijan.kharcha.domain.model

data class Account(
    val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val institutionName: String? = null,
    val accountNumberLast4: String? = null,
    val openingBalancePaisa: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(name.isNotBlank()) { "Account name cannot be blank" }
        require(openingBalancePaisa >= 0L) { "Opening balance in paisa cannot be negative" }
    }
}
