package com.sijan.kharcha.domain.model

data class Budget(
    val id: Long = 0L,
    val categoryId: Long,
    val monthYear: String, // Format: "YYYY-MM"
    val limitPaisa: Long,  // In paisa
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
