package com.sijan.kharcha.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.LedgerRepository
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
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LedgerRepository
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

    val uiState: StateFlow<HomeUiState> = selectedMonthYear
        .flatMapLatest { (year, month) ->
            val (startMillis, endMillis) = calculateMonthBounds(year, month)
            val monthEntriesFlow = repository.getFilteredEntries(startDate = startMillis, endDate = endMillis)
            val summaryFlow = repository.getMonthlySummary(year, month)
            val categoriesFlow = repository.getAllCategories()

            combine(summaryFlow, monthEntriesFlow, categoriesFlow) { summary, entries, categories ->
                HomeUiState(
                    monthlySummary = summary,
                    recentEntries = entries.take(10),
                    categoriesMap = categories.associateBy { it.id },
                    selectedYear = year,
                    selectedMonth = month,
                    selectedMonthLabel = formatMonthLabel(year, month),
                    isCurrentMonth = (year == realCurrentYear && month == realCurrentMonth),
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
