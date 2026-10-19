package com.sijan.kharcha.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.SavingsGoalRepository
import com.sijan.kharcha.domain.model.SavingsContribution
import com.sijan.kharcha.domain.model.SavingsGoal
import com.sijan.kharcha.domain.model.SavingsGoalSummary
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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SavingsGoalsViewModel @Inject constructor(
    private val repository: SavingsGoalRepository
) : ViewModel() {

    private val _tabFilter = MutableStateFlow(SavingsTabFilter.ALL)
    private val _showAddEditDialog = MutableStateFlow(false)
    private val _goalToEdit = MutableStateFlow<SavingsGoal?>(null)
    private val _selectedForDeposit = MutableStateFlow<SavingsGoal?>(null)
    private val _selectedForHistory = MutableStateFlow<SavingsGoal?>(null)
    private val _goalToDelete = MutableStateFlow<SavingsGoal?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _historyContributions = _selectedForHistory.flatMapLatest { goal ->
        if (goal == null) flowOf(emptyList())
        else repository.getContributionsForGoal(goal.id)
    }

    val uiState: StateFlow<SavingsGoalsUiState> = combine(
        repository.getAllGoals(),
        repository.getSummary(),
        _tabFilter,
        _showAddEditDialog,
        _goalToEdit,
        _selectedForDeposit,
        _selectedForHistory,
        _historyContributions,
        _goalToDelete,
        _errorMessage
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allGoals = args[0] as List<SavingsGoal>
        val summary = args[1] as SavingsGoalSummary
        val tab = args[2] as SavingsTabFilter
        val showDialog = args[3] as Boolean
        val editing = args[4] as SavingsGoal?
        val forDeposit = args[5] as SavingsGoal?
        val forHistory = args[6] as SavingsGoal?
        @Suppress("UNCHECKED_CAST")
        val contributions = args[7] as List<SavingsContribution>
        val toDelete = args[8] as SavingsGoal?
        val errorMsg = args[9] as String?

        val filteredGoals = allGoals.filter { goal ->
            when (tab) {
                SavingsTabFilter.ALL -> true
                SavingsTabFilter.ACTIVE -> !goal.isCompleted
                SavingsTabFilter.COMPLETED -> goal.isCompleted
            }
        }

        SavingsGoalsUiState(
            tabFilter = tab,
            summary = summary,
            goals = filteredGoals,
            selectedGoalForDeposit = forDeposit,
            selectedGoalForHistory = forHistory,
            contributionsForHistory = contributions,
            goalToEdit = editing,
            showAddEditDialog = showDialog,
            goalToDelete = toDelete,
            isLoading = false,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsGoalsUiState(isLoading = true)
    )

    fun setTabFilter(tab: SavingsTabFilter) {
        _tabFilter.value = tab
    }

    fun openAddGoalDialog() {
        _goalToEdit.value = null
        _showAddEditDialog.value = true
    }

    fun openEditGoalDialog(goal: SavingsGoal) {
        _goalToEdit.value = goal
        _showAddEditDialog.value = true
    }

    fun closeAddEditDialog() {
        _showAddEditDialog.value = false
        _goalToEdit.value = null
    }

    fun saveGoal(
        name: String,
        targetAmountPaisa: Long,
        targetDateMillis: Long?,
        notes: String?
    ) {
        if (name.isBlank() || targetAmountPaisa <= 0L) {
            _errorMessage.value = "Please provide valid goal details"
            return
        }
        viewModelScope.launch {
            try {
                val currentEdit = _goalToEdit.value
                if (currentEdit != null) {
                    repository.updateGoal(
                        id = currentEdit.id,
                        name = name,
                        targetAmountPaisa = targetAmountPaisa,
                        targetDateMillis = targetDateMillis,
                        notes = notes,
                        isCompleted = currentEdit.isCompleted
                    )
                } else {
                    repository.createGoal(
                        name = name,
                        targetAmountPaisa = targetAmountPaisa,
                        targetDateMillis = targetDateMillis,
                        notes = notes
                    )
                }
                closeAddEditDialog()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save goal"
            }
        }
    }

    fun promptDeleteGoal(goal: SavingsGoal) {
        _goalToDelete.value = goal
    }

    fun dismissDeleteDialog() {
        _goalToDelete.value = null
    }

    fun confirmDeleteGoal() {
        val goal = _goalToDelete.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteGoal(goal.id)
                _goalToDelete.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete goal"
            }
        }
    }

    fun openDepositDialog(goal: SavingsGoal) {
        _selectedForDeposit.value = goal
    }

    fun closeDepositDialog() {
        _selectedForDeposit.value = null
    }

    fun addDeposit(
        goalId: Long,
        amountPaisa: Long,
        dateMillis: Long,
        notes: String?
    ) {
        if (amountPaisa <= 0L) {
            _errorMessage.value = "Deposit amount must be positive"
            return
        }
        viewModelScope.launch {
            try {
                repository.addContribution(
                    goalId = goalId,
                    amountPaisa = amountPaisa,
                    dateMillis = dateMillis,
                    notes = notes
                )
                closeDepositDialog()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to record deposit"
            }
        }
    }

    fun openHistoryDialog(goal: SavingsGoal) {
        _selectedForHistory.value = goal
    }

    fun closeHistoryDialog() {
        _selectedForHistory.value = null
    }

    fun deleteContribution(contributionId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteContribution(contributionId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete deposit"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
