package com.sijan.kharcha.ui.scan

import android.net.Uri
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.parser.model.ValidationStatus

data class ConfirmScanUiState(
    val isLoading: Boolean = false,
    val imageUri: Uri? = null,
    val receiptPath: String? = null,
    val rawOcrText: String? = null,
    val source: String = "receipt",

    // Editable fields
    val amountText: String = "",
    val amountStatus: ValidationStatus = ValidationStatus.VALID,
    val amountStatusMessage: String? = null,

    val dateEpochMillis: Long = System.currentTimeMillis(),
    val dateStatus: ValidationStatus = ValidationStatus.VALID,
    val dateStatusMessage: String? = null,

    val selectedCategoryId: Long? = null,
    val categoryStatus: ValidationStatus = ValidationStatus.VALID,
    val categoryStatusMessage: String? = null,

    val transactionId: String = "",
    val transactionIdStatus: ValidationStatus = ValidationStatus.VALID,
    val transactionIdStatusMessage: String? = null,

    val merchant: String = "",
    val merchantStatus: ValidationStatus = ValidationStatus.VALID,
    val merchantStatusMessage: String? = null,

    val feeText: String = "",
    val feeStatus: ValidationStatus = ValidationStatus.VALID,

    val remarks: String = "",

    // Categories
    val categories: List<Category> = emptyList(),

    // Duplicate detection
    val duplicateEntry: LedgerEntry? = null,
    val isDuplicateDismissed: Boolean = false,

    // Parsing failure warning
    val isOcrPartiallyFailed: Boolean = false,

    // Operation flags
    val isSaving: Boolean = false
) {
    val isSaveEnabled: Boolean
        get() {
            if (isLoading || isSaving) return false
            if (amountStatus == ValidationStatus.ERROR || amountText.isBlank()) return false
            if (dateStatus == ValidationStatus.ERROR) return false
            if (categoryStatus == ValidationStatus.ERROR || selectedCategoryId == null) return false
            if (duplicateEntry != null && !isDuplicateDismissed) return false
            return true
        }
}

sealed interface ConfirmScanEvent {
    data object SaveSuccess : ConfirmScanEvent
    data class NavigateToEntry(val entryId: Long) : ConfirmScanEvent
    data class ShowError(val message: String) : ConfirmScanEvent
}
