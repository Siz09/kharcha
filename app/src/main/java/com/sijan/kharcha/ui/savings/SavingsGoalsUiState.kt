package com.sijan.kharcha.ui.savings

import com.sijan.kharcha.domain.model.SavingsContribution
import com.sijan.kharcha.domain.model.SavingsGoal
import com.sijan.kharcha.domain.model.SavingsGoalSummary

enum class SavingsTabFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

data class SavingsGoalsUiState(
    val tabFilter: SavingsTabFilter = SavingsTabFilter.ALL,
    val summary: SavingsGoalSummary = SavingsGoalSummary(),
    val goals: List<SavingsGoal> = emptyList(),
    val selectedGoalForDeposit: SavingsGoal? = null,
    val selectedGoalForHistory: SavingsGoal? = null,
    val contributionsForHistory: List<SavingsContribution> = emptyList(),
    val goalToEdit: SavingsGoal? = null,
    val showAddEditDialog: Boolean = false,
    val goalToDelete: SavingsGoal? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
