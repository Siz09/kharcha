package com.sijan.kharcha.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {

    private val currentCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private val currentYear = currentCalendar.get(Calendar.YEAR)
    private val currentMonth = currentCalendar.get(Calendar.MONTH) + 1

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getMonthlySummary(currentYear, currentMonth),
        repository.getAllEntries(),
        repository.getAllCategories()
    ) { summary, entries, categories ->
        HomeUiState(
            monthlySummary = summary,
            recentEntries = entries.take(10),
            categoriesMap = categories.associateBy { it.id },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
