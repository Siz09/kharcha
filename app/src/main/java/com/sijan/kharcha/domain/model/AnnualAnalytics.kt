package com.sijan.kharcha.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryAnnualSpend(
    val categoryId: Long,
    val categoryName: String,
    val totalExpensePaisa: Long,
    val percentageOfTotal: Int
)

@Serializable
data class MonthlyFinancialPoint(
    val monthIndex: Int, // 1..12
    val monthName: String,
    val incomePaisa: Long,
    val expensePaisa: Long,
    val netPaisa: Long
)

data class AnnualAnalyticsSummary(
    val year: Int,
    val calendarSystem: CalendarSystem,
    val totalIncomePaisa: Long,
    val totalExpensePaisa: Long,
    val netSavingsPaisa: Long,
    val savingsRatePercentage: Int,
    val dailyBurnRatePaisa: Long,
    val entryCount: Int,
    val categoryBreakdown: List<CategoryAnnualSpend>,
    val monthlyTotals: List<MonthlyFinancialPoint>,
    val priorYearExpensePaisa: Long? = null,
    val priorYearIncomePaisa: Long? = null,
    val yearOverYearExpenseChangePercentage: Int? = null,
    val isArchived: Boolean = false
)
