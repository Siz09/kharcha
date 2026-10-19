package com.sijan.kharcha.ui.summary

import com.sijan.kharcha.domain.model.Category

data class CategorySpending(
    val category: Category,
    val amountPaisa: Long,
    val percentage: Float,
    val budgetLimitPaisa: Long? = null
)

data class SummaryUiState(
    val year: Int = 0,
    val month: Int = 0,
    val monthLabel: String = "",
    val totalIncomePaisa: Long = 0L,
    val totalExpensePaisa: Long = 0L,
    val balancePaisa: Long = 0L,
    val categorySpending: List<CategorySpending> = emptyList(),
    val entryCount: Int = 0,
    val isLoading: Boolean = true
)
