package com.sijan.kharcha.ui.festival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.FestivalBudgetRepository
import com.sijan.kharcha.domain.model.FestivalBudget
import com.sijan.kharcha.domain.model.FestivalBudgetProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FestivalPlannerViewModel @Inject constructor(
    private val festivalBudgetRepository: FestivalBudgetRepository
) : ViewModel() {

    val activeBudgets: StateFlow<List<FestivalBudgetProgress>> = festivalBudgetRepository.getActiveFestivalBudgets().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveFestivalBudget(budget: FestivalBudget) {
        viewModelScope.launch {
            festivalBudgetRepository.saveFestivalBudget(budget)
        }
    }

    fun deleteFestivalBudget(id: Long) {
        viewModelScope.launch {
            festivalBudgetRepository.deleteFestivalBudget(id)
        }
    }
}
