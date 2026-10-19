package com.sijan.kharcha.ui.home

import androidx.compose.runtime.Immutable
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.FinancialHealthStatus
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.MonthlySummary

@Immutable
data class HomeCategoryExpense(
    val categoryId: Long,
    val categoryName: String,
    val amountPaisa: Long,
    val percentage: Float
)

@Immutable
data class HomeUiState(
    val monthlySummary: MonthlySummary = MonthlySummary(),
    val recentEntries: List<LedgerEntry> = emptyList(),
    val categoriesMap: Map<Long, Category> = emptyMap(),
    val categoryExpenses: List<HomeCategoryExpense> = emptyList(),
    val selectedYear: Int = 0,
    val selectedMonth: Int = 0,
    val selectedMonthLabel: String = "",
    val isCurrentMonth: Boolean = true,
    val savingsRatePercent: Int? = null,
    val dailyBurnRatePaisa: Long = 0L,
    val financialHealth: FinancialHealthStatus = FinancialHealthStatus.HEALTHY,
    val openingBalancePaisa: Long = 0L,
    val rolloverEnabled: Boolean = false,
    val isLoading: Boolean = true
)
