package com.sijan.kharcha.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.FinancialHealthStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val nowCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private val realCurrentYear = nowCalendar.get(Calendar.YEAR)
    private val realCurrentMonth = nowCalendar.get(Calendar.MONTH) + 1

    private val selectedMonthYear = MutableStateFlow(Pair(realCurrentYear, realCurrentMonth))

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        selectedMonthYear,
        userPreferencesRepository.preferences
    ) { (year, month), prefs ->
        Triple(year, month, prefs.rolloverEnabled)
    }.flatMapLatest { (year, month, rolloverEnabled) ->
        val (startMillis, endMillis) = calculateMonthBounds(year, month)
        val monthEntriesFlow = repository.getFilteredEntries(startDate = startMillis, endDate = endMillis)
        val summaryFlow = repository.getMonthlySummary(year, month)
        val categoriesFlow = repository.getAllCategories()
        val openingBalanceFlow = if (rolloverEnabled) {
            repository.getOpeningBalance(startMillis)
        } else {
            flowOf(0L)
        }

        val accountsFlow = repository.getAccountsWithBalances()

        combine(summaryFlow, monthEntriesFlow, categoriesFlow, openingBalanceFlow, accountsFlow) { summary, entries, categories, openingBal, accounts ->
            val isCurrent = (year == realCurrentYear && month == realCurrentMonth)
            val isFuture = (year > realCurrentYear) || (year == realCurrentYear && month > realCurrentMonth)
            val (savingsRate, healthStatus) = calculateSavingsRateAndHealth(
                incomePaisa = summary.totalIncomePaisa,
                expensePaisa = summary.totalExpensePaisa
            )
            val burnRate = calculateDailyBurnRate(
                expensePaisa = summary.totalExpensePaisa,
                year = year,
                month = month,
                isCurrentMonth = isCurrent,
                isFutureMonth = isFuture
            )

            val categoryExpenses = if (summary.totalExpensePaisa > 0L) {
                entries.filter { it.type == EntryType.EXPENSE }
                    .groupBy { it.categoryId }
                    .map { (catId, catEntries) ->
                        val sum = catEntries.sumOf { it.amountPaisa }
                        val catName = categories.find { it.id == catId }?.name ?: "Other"
                        HomeCategoryExpense(
                            categoryId = catId,
                            categoryName = catName,
                            amountPaisa = sum,
                            percentage = (sum.toFloat() / summary.totalExpensePaisa.toFloat()) * 100f
                        )
                    }
                    .sortedByDescending { it.amountPaisa }
            } else {
                emptyList()
            }

            val netWorth = accounts.filter { it.account.isActive }.sumOf { it.currentBalancePaisa }

            HomeUiState(
                monthlySummary = summary,
                recentEntries = entries.take(10),
                categoriesMap = categories.associateBy { it.id },
                categoryExpenses = categoryExpenses,
                selectedYear = year,
                selectedMonth = month,
                selectedMonthLabel = formatMonthLabel(year, month),
                isCurrentMonth = isCurrent,
                savingsRatePercent = savingsRate,
                dailyBurnRatePaisa = burnRate,
                financialHealth = healthStatus,
                openingBalancePaisa = openingBal,
                rolloverEnabled = rolloverEnabled,
                accounts = accounts,
                totalNetWorthPaisa = netWorth,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            selectedYear = realCurrentYear,
            selectedMonth = realCurrentMonth,
            selectedMonthLabel = formatMonthLabel(realCurrentYear, realCurrentMonth),
            isCurrentMonth = true
        )
    )

    fun selectMonth(year: Int, month: Int) {
        selectedMonthYear.value = Pair(year, month)
    }

    fun resetToCurrentMonth() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        selectedMonthYear.value = Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }

    private fun calculateSavingsRateAndHealth(
        incomePaisa: Long,
        expensePaisa: Long
    ): Pair<Int?, FinancialHealthStatus> {
        return when {
            incomePaisa > 0L -> {
                val rate = (((incomePaisa - expensePaisa) * 100) / incomePaisa).toInt()
                val status = when {
                    rate > 30 -> FinancialHealthStatus.HEALTHY
                    rate in 10..30 -> FinancialHealthStatus.MODERATE
                    rate in 0..9 -> FinancialHealthStatus.SLIM
                    else -> FinancialHealthStatus.DEFICIT
                }
                Pair(rate, status)
            }
            expensePaisa > 0L -> {
                Pair(-100, FinancialHealthStatus.DEFICIT)
            }
            else -> {
                Pair(null, FinancialHealthStatus.HEALTHY)
            }
        }
    }

    private fun calculateDailyBurnRate(
        expensePaisa: Long,
        year: Int,
        month: Int,
        isCurrentMonth: Boolean,
        isFutureMonth: Boolean
    ): Long {
        if (isFutureMonth || expensePaisa <= 0L) return 0L

        val daysCount = if (isCurrentMonth) {
            val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        } else {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        }
        return expensePaisa / daysCount
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

    private fun formatMonthLabel(year: Int, month: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(cal.time)
    }
}
