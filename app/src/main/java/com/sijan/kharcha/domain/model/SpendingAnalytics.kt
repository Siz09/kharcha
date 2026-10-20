package com.sijan.kharcha.domain.model

import java.time.DayOfWeek

data class MonthOverMonthComparison(
    val currentMonthTotalExpensePaisa: Long,
    val previousMonthTotalExpensePaisa: Long,
    val netDifferencePaisa: Long,
    val percentageChange: Int,
    val categoryDeltas: List<CategorySpendDelta>
)

data class CategorySpendDelta(
    val categoryId: Long,
    val categoryName: String,
    val currentMonthPaisa: Long,
    val previousMonthPaisa: Long,
    val differencePaisa: Long,
    val percentageChange: Int
)

data class DayOfWeekSpend(
    val dayOfWeek: DayOfWeek,
    val totalExpensePaisa: Long,
    val transactionCount: Int
)

data class SpendingAnalyticsSummary(
    val year: Int,
    val month: Int,
    val momComparison: MonthOverMonthComparison,
    val dayOfWeekSpends: List<DayOfWeekSpend>,
    val topMerchants: List<Pair<String, Long>>,
    val highestSpendDayDate: Long?,
    val highestSpendDayAmountPaisa: Long
)
