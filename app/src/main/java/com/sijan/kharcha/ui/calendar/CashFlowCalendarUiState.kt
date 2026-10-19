package com.sijan.kharcha.ui.calendar

import androidx.compose.runtime.Immutable
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.LedgerEntry

@Immutable
data class DayCashFlow(
    val dayOfMonth: Int,
    val totalIncomePaisa: Long = 0L,
    val totalExpensePaisa: Long = 0L,
    val entries: List<LedgerEntry> = emptyList()
) {
    val netPaisa: Long get() = totalIncomePaisa - totalExpensePaisa
    val hasEntries: Boolean get() = entries.isNotEmpty()
}

@Immutable
data class CashFlowCalendarUiState(
    val selectedYear: Int = 0,
    val selectedMonth: Int = 0,
    val selectedMonthLabel: String = "",
    val selectedDay: Int = 1,
    val daysInMonth: Int = 30,
    val firstDayOfWeekOffset: Int = 0,
    val dailyCashFlowMap: Map<Int, DayCashFlow> = emptyMap(),
    val categoriesMap: Map<Long, Category> = emptyMap(),
    val totalMonthIncomePaisa: Long = 0L,
    val totalMonthExpensePaisa: Long = 0L,
    val totalMonthBalancePaisa: Long = 0L,
    val isLoading: Boolean = true
) {
    val selectedDayCashFlow: DayCashFlow
        get() = dailyCashFlowMap[selectedDay] ?: DayCashFlow(dayOfMonth = selectedDay)
}
