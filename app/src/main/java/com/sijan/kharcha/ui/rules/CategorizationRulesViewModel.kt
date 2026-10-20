package com.sijan.kharcha.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.CategorizationRulesRepository
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.CategorizationRule
import com.sijan.kharcha.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategorizationRulesViewModel @Inject constructor(
    private val rulesRepository: CategorizationRulesRepository,
    private val ledgerRepository: LedgerRepository
) : ViewModel() {

    val rules: StateFlow<List<CategorizationRule>> = rulesRepository.getAllRules().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<Category>> = ledgerRepository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    private val _appliedCount = MutableStateFlow<Int?>(null)
    val appliedCount: StateFlow<Int?> = _appliedCount.asStateFlow()

    fun saveRule(rule: CategorizationRule) {
        viewModelScope.launch {
            rulesRepository.saveRule(rule)
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            rulesRepository.deleteRule(id)
        }
    }

    fun applyRulesToPastEntries() {
        viewModelScope.launch {
            _isApplying.value = true
            try {
                val count = rulesRepository.applyRulesToEntries().getOrDefault(0)
                _appliedCount.value = count
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun clearAppliedNotice() {
        _appliedCount.value = null
    }
}
