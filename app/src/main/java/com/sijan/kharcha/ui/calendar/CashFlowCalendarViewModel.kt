package com.sijan.kharcha.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.EntryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CashFlowCalendarViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {

    private val nowCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private val realCurrentYear = nowCal.get(Calendar.YEAR)
    private val realCurrentMonth = nowCal.get(Calendar.MONTH) + 1
    private val realCurrentDay = nowCal.get(Calendar.DAY_OF_MONTH)

    private val selectedMonthYear = MutableStateFlow(Pair(realCurrentYear, realCurrentMonth))
    private val selectedDay = MutableStateFlow(realCurrentDay)

    val uiState: StateFlow<CashFlowCalendarUiState> = combine(
        selectedMonthYear,
        selectedDay
    ) { (year, month), day ->
        Triple(year, month, day)
    }.flatMapLatest { (year, month, day) ->
        val metadata = calculateMonthMetadata(year, month)
        val entriesFlow = repository.getFilteredEntries(startDate = metadata.startMillis, endDate = metadata.endMillis)
        val categoriesFlow = repository.getAllCategories()

        combine(entriesFlow, categoriesFlow) { entries, categories ->
            val dailyMap = mutableMapOf<Int, DayCashFlow>()
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

            var totalInc = 0L
            var totalExp = 0L

            val grouped = entries.groupBy { entry ->
                cal.timeInMillis = entry.date
                cal.get(Calendar.DAY_OF_MONTH)
            }

            for (d in 1..metadata.daysInMonth) {
                val dayEntries = grouped[d] ?: emptyList()
                val inc = dayEntries.filter { it.type == EntryType.INCOME }.sumOf { it.amountPaisa }
                val exp = dayEntries.filter { it.type == EntryType.EXPENSE }.sumOf { it.amountPaisa }
                totalInc += inc
                totalExp += exp
                dailyMap[d] = DayCashFlow(
                    dayOfMonth = d,
                    totalIncomePaisa = inc,
                    totalExpensePaisa = exp,
                    entries = dayEntries
                )
            }

            val validDay = day.coerceIn(1, metadata.daysInMonth)

            CashFlowCalendarUiState(
                selectedYear = year,
                selectedMonth = month,
                selectedMonthLabel = formatMonthLabel(year, month),
                selectedDay = validDay,
                daysInMonth = metadata.daysInMonth,
                firstDayOfWeekOffset = metadata.firstDayOffset,
                dailyCashFlowMap = dailyMap,
                categoriesMap = categories.associateBy { it.id },
                totalMonthIncomePaisa = totalInc,
                totalMonthExpensePaisa = totalExp,
                totalMonthBalancePaisa = totalInc - totalExp,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CashFlowCalendarUiState(
            selectedYear = realCurrentYear,
            selectedMonth = realCurrentMonth,
            selectedMonthLabel = formatMonthLabel(realCurrentYear, realCurrentMonth),
            selectedDay = realCurrentDay
        )
    )

    fun selectDay(day: Int) {
        selectedDay.value = day
    }

    fun previousMonth() {
        val (year, month) = selectedMonthYear.value
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            add(Calendar.MONTH, -1)
        }
        selectedMonthYear.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        selectedDay.value = 1
    }

    fun nextMonth() {
        val (year, month) = selectedMonthYear.value
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            add(Calendar.MONTH, 1)
        }
        selectedMonthYear.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        selectedDay.value = 1
    }

    private data class MonthMetadata(
        val startMillis: Long,
        val endMillis: Long,
        val daysInMonth: Int,
        val firstDayOffset: Int
    )

    private fun calculateMonthMetadata(year: Int, month: Int): MonthMetadata {
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
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

        cal.set(Calendar.DAY_OF_MONTH, daysInMonth)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMillis = cal.timeInMillis

        return MonthMetadata(startMillis, endMillis, daysInMonth, firstDayOffset)
    }

    private fun formatMonthLabel(year: Int, month: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
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
