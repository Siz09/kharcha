package com.sijan.kharcha.ui.entries

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.model.TaxCategory
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AddEditEntryViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditEntryUiState())
    val uiState: StateFlow<AddEditEntryUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddEditEntryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var categoriesJob: Job? = null
    private var existingEntry: LedgerEntry? = null

    init {
        val parseLongArg: (String) -> Long? = { key ->
            when (val raw = savedStateHandle.get<Any>(key)) {
                is Long -> raw
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull()
                else -> null
            }
        }
        val entryIdArg: Long? = parseLongArg("entryId")
        val prefillAmountPaisa: Long? = parseLongArg("prefillAmountPaisa")
        val prefillNote: String? = savedStateHandle.get<String>("prefillNote")
        val prefillTypeStr: String? = savedStateHandle.get<String>("prefillType")
        val prefillDate: Long? = parseLongArg("prefillDate")
        val prefillTxnId: String? = savedStateHandle.get<String>("prefillTxnId")

        if (entryIdArg != null && entryIdArg > 0L) {
            _uiState.update { it.copy(isEditMode = true, entryId = entryIdArg, isLoading = true) }
            loadEntry(entryIdArg)
        } else if (prefillAmountPaisa != null && prefillAmountPaisa > 0L) {
            val prefillType = if (prefillTypeStr == EntryType.INCOME.name) EntryType.INCOME else EntryType.EXPENSE
            val rupees = prefillAmountPaisa / 100
            val remPaisa = prefillAmountPaisa % 100
            val formattedAmount = if (remPaisa == 0L) rupees.toString() else String.format(Locale.US, "%d.%02d", rupees, remPaisa)

            _uiState.update {
                it.copy(
                    type = prefillType,
                    amountText = formattedAmount,
                    amountStatus = ValidationStatus.VALID,
                    note = prefillNote ?: "",
                    dateEpochMillis = prefillDate ?: System.currentTimeMillis(),
                    transactionId = prefillTxnId ?: ""
                )
            }
            loadCategoriesForType(prefillType)
        } else {
            loadCategoriesForType(EntryType.EXPENSE)
        }

        viewModelScope.launch {
            ledgerRepository.getActiveAccounts().collect { accounts ->
                _uiState.update { state ->
                    val selectedId = if (accounts.any { it.id == state.selectedAccountId }) {
                        state.selectedAccountId
                    } else {
                        accounts.firstOrNull()?.id ?: 1L
                    }
                    state.copy(accounts = accounts, selectedAccountId = selectedId)
                }
            }
        }
    }

    private fun loadEntry(id: Long) {
        viewModelScope.launch {
            val entry = ledgerRepository.getEntryById(id).firstOrNull()
            if (entry != null) {
                existingEntry = entry
                val rupees = entry.amountPaisa / 100
                val remPaisa = entry.amountPaisa % 100
                val formattedAmount = if (remPaisa == 0L) {
                    rupees.toString()
                } else {
                    String.format(Locale.US, "%d.%02d", rupees, remPaisa)
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        type = entry.type,
                        amountText = formattedAmount,
                        selectedCategoryId = entry.categoryId,
                        selectedAccountId = entry.accountId,
                        dateEpochMillis = entry.date,
                        note = entry.note,
                        transactionId = entry.transactionId ?: "",
                        receiptPath = entry.receiptPath,
                        isTaxDeductible = entry.isTaxDeductible,
                        taxCategory = entry.taxCategory
                    )
                }
                loadCategoriesForType(entry.type, selectedId = entry.categoryId)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(AddEditEntryEvent.ShowError("Entry not found"))
            }
        }
    }

    private fun loadCategoriesForType(type: EntryType, selectedId: Long? = null) {
        viewModelScope.launch {
            ledgerRepository.seedDefaultCategories()
            val cats = ledgerRepository.getCategoriesByType(type).first()
            _uiState.update { state ->
                val chosenId = selectedId
                    ?: (if (cats.any { it.id == state.selectedCategoryId }) state.selectedCategoryId else cats.firstOrNull()?.id)
                state.copy(
                    categories = cats,
                    selectedCategoryId = chosenId,
                    categoryStatus = if (chosenId != null) ValidationStatus.VALID else ValidationStatus.ERROR
                )
            }
        }
    }

    fun onTypeChanged(newType: EntryType) {
        if (_uiState.value.type == newType) return
        _uiState.update {
            it.copy(
                type = newType,
                selectedCategoryId = null,
                categoryStatus = ValidationStatus.ERROR
            )
        }
        loadCategoriesForType(newType)
    }

    fun onAmountChanged(newText: String) {
        val parsedPaisa = Money.parsePaisa(newText)
        val (status, errorMsg) = when {
            newText.isBlank() -> ValidationStatus.ERROR to "Amount is required"
            parsedPaisa == null -> ValidationStatus.ERROR to "Invalid amount format (max 2 decimals)"
            parsedPaisa <= 0 -> ValidationStatus.ERROR to "Amount must be greater than zero"
            else -> ValidationStatus.VALID to null
        }

        _uiState.update {
            it.copy(
                amountText = newText,
                amountStatus = status,
                amountStatusMessage = errorMsg
            )
        }
    }

    fun onCategorySelected(categoryId: Long) {
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                categoryStatus = ValidationStatus.VALID,
                categoryStatusMessage = null
            )
        }
    }

    fun onDateChanged(epochMillis: Long) {
        val now = System.currentTimeMillis()
        val (status, errorMsg) = if (epochMillis > now + 60_000) {
            ValidationStatus.ERROR to "Date cannot be in the future"
        } else {
            ValidationStatus.VALID to null
        }

        _uiState.update {
            it.copy(
                dateEpochMillis = epochMillis,
                dateStatus = status,
                dateStatusMessage = errorMsg
            )
        }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onTransactionIdChanged(transactionId: String) {
        _uiState.update { it.copy(transactionId = transactionId) }
    }

    fun onAccountChanged(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun onTaxDeductibleChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isTaxDeductible = enabled,
                taxCategory = if (enabled) (it.taxCategory ?: TaxCategory.LIFE_INSURANCE) else null
            )
        }
    }

    fun onTaxCategoryChanged(category: TaxCategory?) {
        _uiState.update { it.copy(taxCategory = category) }
    }

    fun onDeleteClicked() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun onDeleteConfirmed() {
        val id = _uiState.value.entryId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteConfirmDialog = false) }
            val result = ledgerRepository.deleteEntry(id)
            _uiState.update { it.copy(isDeleting = false) }
            if (result.isSuccess) {
                _events.send(AddEditEntryEvent.DeleteSuccess)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to delete entry"
                _events.send(AddEditEntryEvent.ShowError(err))
            }
        }
    }

    fun saveEntry() {
        val state = _uiState.value
        if (!state.isSaveEnabled) return

        val amountPaisa = Money.parsePaisa(state.amountText) ?: return
        val categoryId = state.selectedCategoryId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val result = if (state.isEditMode && state.entryId != null) {
                val base = existingEntry ?: LedgerEntry(
                    id = state.entryId,
                    amountPaisa = amountPaisa,
                    type = state.type,
                    categoryId = categoryId,
                    date = state.dateEpochMillis
                )
                val updated = base.copy(
                    amountPaisa = amountPaisa,
                    type = state.type,
                    categoryId = categoryId,
                    accountId = state.selectedAccountId,
                    note = state.note.trim(),
                    date = state.dateEpochMillis,
                    transactionId = state.transactionId.trim().ifBlank { null },
                    isTaxDeductible = state.isTaxDeductible,
                    taxCategory = if (state.isTaxDeductible) state.taxCategory else null
                )
                ledgerRepository.updateEntry(updated)
            } else {
                val newEntry = LedgerEntry(
                    amountPaisa = amountPaisa,
                    type = state.type,
                    categoryId = categoryId,
                    accountId = state.selectedAccountId,
                    note = state.note.trim(),
                    date = state.dateEpochMillis,
                    source = "manual",
                    transactionId = state.transactionId.trim().ifBlank { null },
                    isTaxDeductible = state.isTaxDeductible,
                    taxCategory = if (state.isTaxDeductible) state.taxCategory else null
                )
                ledgerRepository.saveEntry(newEntry).map { Unit }
            }

            _uiState.update { it.copy(isSaving = false) }

            if (result.isSuccess) {
                _events.send(AddEditEntryEvent.SaveSuccess)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to save entry"
                _events.send(AddEditEntryEvent.ShowError(err))
            }
        }
    }
}
