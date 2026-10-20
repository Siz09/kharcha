package com.sijan.kharcha.ui.utility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.UtilityRepository
import com.sijan.kharcha.domain.model.UtilityAccount
import com.sijan.kharcha.domain.model.UtilityType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UtilityVaultViewModel @Inject constructor(
    private val utilityRepository: UtilityRepository
) : ViewModel() {

    private val _selectedType = MutableStateFlow<UtilityType?>(null)
    val selectedType: StateFlow<UtilityType?> = _selectedType.asStateFlow()

    val accounts: StateFlow<List<UtilityAccount>> = _selectedType.flatMapLatest { type ->
        if (type == null) {
            utilityRepository.getAllUtilityAccounts()
        } else {
            utilityRepository.getUtilityAccountsByType(type)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectType(type: UtilityType?) {
        _selectedType.value = type
    }

    fun saveAccount(account: UtilityAccount, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (account.id == 0L) {
                utilityRepository.saveUtilityAccount(account)
            } else {
                utilityRepository.updateUtilityAccount(account)
            }
            onComplete()
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            utilityRepository.deleteUtilityAccount(id)
        }
    }
}
