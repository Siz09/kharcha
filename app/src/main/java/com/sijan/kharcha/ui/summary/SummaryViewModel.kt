package com.sijan.kharcha.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.Budget
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    private val currentCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    private val selectedMonthYear = MutableStateFlow(
        Pair(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH) + 1)
    )

    private val allCategories = ledgerRepository.getAllCategories()

    val uiState: StateFlow<SummaryUiState> = selectedMonthYear
        .flatMapLatest { (year, month) ->
            val (startMillis, endMillis) = calculateMonthBounds(year, month)
            val monthEntriesFlow = ledgerRepository.getFilteredEntries(
                startDate = startMillis,
                endDate = endMillis
            )
            val monthYearStr = String.format(Locale.US, "%04d-%02d", year, month)
            val budgetsFlow = ledgerRepository.getBudgetsForMonth(monthYearStr)

            combine(monthEntriesFlow, allCategories, budgetsFlow) { entries, categories, budgets ->
                computeSummary(year, month, entries, categories, budgets)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SummaryUiState(
                year = currentCal.get(Calendar.YEAR),
                month = currentCal.get(Calendar.MONTH) + 1,
                monthLabel = formatMonthLabel(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH) + 1)
            )
        )

    fun nextMonth() {
        val (year, month) = selectedMonthYear.value
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            add(Calendar.MONTH, 1)
        }
        selectedMonthYear.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun previousMonth() {
        val (year, month) = selectedMonthYear.value
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            add(Calendar.MONTH, -1)
        }
        selectedMonthYear.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun resetToCurrentMonth() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        selectedMonthYear.value = Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }

    fun setCategoryBudget(categoryId: Long, limitPaisa: Long) {
        val (year, month) = selectedMonthYear.value
        val monthYearStr = String.format(Locale.US, "%04d-%02d", year, month)
        viewModelScope.launch {
            ledgerRepository.saveBudget(categoryId, monthYearStr, limitPaisa)
        }
    }

    fun deleteCategoryBudget(categoryId: Long) {
        val (year, month) = selectedMonthYear.value
        val monthYearStr = String.format(Locale.US, "%04d-%02d", year, month)
        viewModelScope.launch {
            ledgerRepository.deleteBudget(categoryId, monthYearStr)
        }
    }

    private fun calculateMonthBounds(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMillis = cal.timeInMillis

        return Pair(startMillis, endMillis)
    }

    private fun computeSummary(
        year: Int,
        month: Int,
        entries: List<LedgerEntry>,
        categories: List<Category>,
        budgets: List<Budget>
    ): SummaryUiState {
        val categoryMap = categories.associateBy { it.id }
        val budgetMap = budgets.associate { it.categoryId to it.limitPaisa }

        var totalIncome = 0L
        var totalExpense = 0L

        val expenseCategoryTotals = mutableMapOf<Long, Long>()

        for (entry in entries) {
            when (entry.type) {
                EntryType.INCOME -> totalIncome += entry.amountPaisa
                EntryType.EXPENSE -> {
                    totalExpense += entry.amountPaisa
                    val catTotal = expenseCategoryTotals.getOrDefault(entry.categoryId, 0L)
                    expenseCategoryTotals[entry.categoryId] = catTotal + entry.amountPaisa
                }
                EntryType.TRANSFER -> Unit
            }
        }

        val categorySpendingList = expenseCategoryTotals.mapNotNull { (catId, amount) ->
            categoryMap[catId]?.let { category ->
                val percentage = if (totalExpense > 0L) amount.toFloat() / totalExpense.toFloat() else 0f
                CategorySpending(
                    category = category,
                    amountPaisa = amount,
                    percentage = percentage,
                    budgetLimitPaisa = budgetMap[catId]
                )
            }
        }.sortedByDescending { it.amountPaisa }

        return SummaryUiState(
            year = year,
            month = month,
            monthLabel = formatMonthLabel(year, month),
            totalIncomePaisa = totalIncome,
            totalExpensePaisa = totalExpense,
            balancePaisa = totalIncome - totalExpense,
            categorySpending = categorySpendingList,
            entryCount = entries.size,
            isLoading = false
        )
    }

    private fun formatMonthLabel(year: Int, month: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(cal.timeInMillis))
    }
}
