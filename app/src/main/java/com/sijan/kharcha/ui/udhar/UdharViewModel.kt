package com.sijan.kharcha.ui.udhar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.UdharRepository
import com.sijan.kharcha.domain.model.UdharRecord
import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UdharViewModel @Inject constructor(
    private val repository: UdharRepository
) : ViewModel() {

    private val _tabFilter = MutableStateFlow(UdharTabFilter.ALL)
    private val _statusFilter = MutableStateFlow<UdharStatus?>(null)
    private val _showAddDialog = MutableStateFlow(false)
    private val _selectedForSettlement = MutableStateFlow<UdharRecord?>(null)
    private val _selectedForHistory = MutableStateFlow<UdharRecord?>(null)
    private val _udharToDelete = MutableStateFlow<UdharRecord?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _historySettlements = _selectedForHistory.flatMapLatest { udhar ->
        if (udhar == null) flowOf(emptyList())
        else repository.getSettlementsForUdhar(udhar.id)
    }

    val uiState: StateFlow<UdharUiState> = combine(
        repository.getAllUdhars(),
        repository.getUdharSummary(),
        _tabFilter,
        _statusFilter,
        _showAddDialog,
        _selectedForSettlement,
        _selectedForHistory,
        _historySettlements,
        _udharToDelete,
        _errorMessage
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allUdhars = args[0] as List<UdharRecord>
        val summary = args[1] as com.sijan.kharcha.domain.model.UdharSummary
        val tab = args[2] as UdharTabFilter
        val status = args[3] as UdharStatus?
        val showAdd = args[4] as Boolean
        val forSettlement = args[5] as UdharRecord?
        val forHistory = args[6] as UdharRecord?
        @Suppress("UNCHECKED_CAST")
        val settlements = args[7] as List<com.sijan.kharcha.domain.model.UdharSettlement>
        val toDelete = args[8] as UdharRecord?
        val errorMsg = args[9] as String?

        val filteredItems = allUdhars.filter { record ->
            val matchesTab = when (tab) {
                UdharTabFilter.ALL -> true
                UdharTabFilter.LENT -> record.type == UdharType.LENT
                UdharTabFilter.BORROWED -> record.type == UdharType.BORROWED
            }
            val matchesStatus = (status == null || record.status == status)
            matchesTab && matchesStatus
        }

        UdharUiState(
            tabFilter = tab,
            statusFilter = status,
            summary = summary,
            items = filteredItems,
            selectedUdharForSettlement = forSettlement,
            selectedUdharForHistory = forHistory,
            settlementsForHistory = settlements,
            showAddDialog = showAdd,
            udharToDelete = toDelete,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UdharUiState(isLoading = true)
    )

    fun onTabFilterChanged(tab: UdharTabFilter) {
        _tabFilter.value = tab
    }

    fun onStatusFilterChanged(status: UdharStatus?) {
        _statusFilter.value = status
    }

    fun onAddClicked() {
        _showAddDialog.value = true
    }

    fun onDismissAddDialog() {
        _showAddDialog.value = false
    }

    fun onCreateUdhar(
        name: String,
        phone: String?,
        type: UdharType,
        amountPaisa: Long,
        dueDate: Long?,
        notes: String
    ) {
        viewModelScope.launch {
            val result = repository.createUdhar(
                personName = name,
                phoneNumber = phone,
                type = type,
                amountPaisa = amountPaisa,
                dueDate = dueDate,
                notes = notes
            )
            if (result.isSuccess) {
                _showAddDialog.value = false
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save udhar"
            }
        }
    }

    fun onSettleClicked(udhar: UdharRecord) {
        _selectedForSettlement.value = udhar
    }

    fun onDismissSettleDialog() {
        _selectedForSettlement.value = null
    }

    fun onConfirmSettlement(amountPaisa: Long, date: Long, notes: String?) {
        val udhar = _selectedForSettlement.value ?: return
        viewModelScope.launch {
            val result = repository.recordSettlement(
                udharId = udhar.id,
                amountPaisa = amountPaisa,
                dateEpochMillis = date,
                notes = notes
            )
            if (result.isSuccess) {
                _selectedForSettlement.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to record payment"
            }
        }
    }

    fun onViewHistoryClicked(udhar: UdharRecord) {
        _selectedForHistory.value = udhar
    }

    fun onDismissHistory() {
        _selectedForHistory.value = null
    }

    fun onDeleteSettlement(settlementId: Long) {
        viewModelScope.launch {
            val result = repository.deleteSettlement(settlementId)
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete payment"
            }
        }
    }

    fun onDeleteUdharClicked(udhar: UdharRecord) {
        _udharToDelete.value = udhar
    }

    fun onDismissDeleteConfirm() {
        _udharToDelete.value = null
    }

    fun onConfirmDeleteUdhar() {
        val udhar = _udharToDelete.value ?: return
        viewModelScope.launch {
            val result = repository.deleteUdhar(udhar.id)
            _udharToDelete.value = null
            if (_selectedForHistory.value?.id == udhar.id) {
                _selectedForHistory.value = null
            }
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete udhar"
            }
        }
    }

    fun onErrorDismissed() {
        _errorMessage.value = null
    }
}
