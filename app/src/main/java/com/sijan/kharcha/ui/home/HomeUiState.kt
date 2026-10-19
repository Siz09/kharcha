package com.sijan.kharcha.ui.home

import androidx.compose.runtime.Immutable
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.MonthlySummary

@Immutable
data class HomeUiState(
    val monthlySummary: MonthlySummary = MonthlySummary(),
    val recentEntries: List<LedgerEntry> = emptyList(),
    val categoriesMap: Map<Long, Category> = emptyMap(),
    val selectedYear: Int = 0,
    val selectedMonth: Int = 0,
    val selectedMonthLabel: String = "",
    val isCurrentMonth: Boolean = true,
    val isLoading: Boolean = true
)
