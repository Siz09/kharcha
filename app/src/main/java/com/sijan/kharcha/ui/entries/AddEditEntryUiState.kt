package com.sijan.kharcha.ui.entries

import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.parser.model.ValidationStatus

data class AddEditEntryUiState(
    val isEditMode: Boolean = false,
    val entryId: Long? = null,
    val isLoading: Boolean = false,
    val type: EntryType = EntryType.EXPENSE,
    val amountText: String = "",
    val amountStatus: ValidationStatus = ValidationStatus.VALID,
    val amountStatusMessage: String? = null,
    val selectedCategoryId: Long? = null,
    val categoryStatus: ValidationStatus = ValidationStatus.VALID,
    val categoryStatusMessage: String? = null,
    val categories: List<Category> = emptyList(),
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val dateStatus: ValidationStatus = ValidationStatus.VALID,
    val dateStatusMessage: String? = null,
    val note: String = "",
    val transactionId: String = "",
    val receiptPath: String? = null,
    val accounts: List<com.sijan.kharcha.domain.model.Account> = emptyList(),
    val selectedAccountId: Long = 1L,
    val showDeleteConfirmDialog: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false
) {
    val isSaveEnabled: Boolean
        get() {
            if (isLoading || isSaving || isDeleting) return false
            if (amountStatus == ValidationStatus.ERROR || amountText.isBlank()) return false
            if (categoryStatus == ValidationStatus.ERROR || selectedCategoryId == null) return false
            if (dateStatus == ValidationStatus.ERROR) return false
            return true
        }
}

sealed interface AddEditEntryEvent {
    data object SaveSuccess : AddEditEntryEvent
    data object DeleteSuccess : AddEditEntryEvent
    data class ShowError(val message: String) : AddEditEntryEvent
}
