package com.sijan.kharcha.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.data.storage.ReceiptStorage
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.parser.ReceiptOcrScanner
import com.sijan.kharcha.domain.parser.ReceiptProcessingPipeline
import com.sijan.kharcha.domain.parser.model.OcrResult
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ConfirmScanViewModel @Inject constructor(
    private val ledgerRepository: LedgerRepository,
    private val receiptStorage: ReceiptStorage,
    private val pipeline: ReceiptProcessingPipeline,
    private val ocrScanner: ReceiptOcrScanner,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmScanUiState())
    val uiState: StateFlow<ConfirmScanUiState> = _uiState.asStateFlow()

    private val _events = Channel<ConfirmScanEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadCategories()

        val imageUriString: String? = savedStateHandle["imageUri"]
        if (!imageUriString.isNullOrBlank()) {
            processImage(Uri.parse(imageUriString))
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            ledgerRepository.seedDefaultCategories()
            val cats = ledgerRepository.getCategoriesByType(EntryType.EXPENSE).first()
            _uiState.update { state ->
                val defaultCatId = state.selectedCategoryId ?: cats.firstOrNull()?.id
                state.copy(
                    categories = cats,
                    selectedCategoryId = defaultCatId,
                    categoryStatus = if (defaultCatId != null) ValidationStatus.VALID else ValidationStatus.ERROR
                )
            }
        }
    }

    fun processImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, imageUri = uri) }

            try {
                // 1. Copy image into app-private storage
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to open image stream for $uri")
                val saveResult = receiptStorage.saveImage(inputStream)
                val imagePath = saveResult.getOrThrow()

                // 2. Perform OCR recognition
                val ocrResult = ocrScanner.scanImage(uri).getOrElse {
                    OcrResult(fullText = "")
                }

                // 3. Run receipt parser pipeline
                val parseResult = pipeline.process(ocrResult, imagePath)

                // 4. Extract amount decimal string
                val initialAmountText = parseResult.amountPaisa.value?.let { paisa ->
                    val rupees = paisa / 100
                    val remPaisa = paisa % 100
                    if (remPaisa == 0L) rupees.toString() else String.format(Locale.US, "%d.%02d", rupees, remPaisa)
                } ?: ""

                // 5. Extract fee decimal string
                val initialFeeText = parseResult.feePaisa.value?.let { paisa ->
                    if (paisa > 0) {
                        val rupees = paisa / 100
                        val remPaisa = paisa % 100
                        if (remPaisa == 0L) rupees.toString() else String.format(Locale.US, "%d.%02d", rupees, remPaisa)
                    } else ""
                } ?: ""

                val txnId = parseResult.transactionId.value ?: ""

                // Check duplicate transaction ID if present
                val duplicates = if (txnId.isNotBlank()) {
                    ledgerRepository.checkDuplicateTransaction(txnId)
                } else emptyList()

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        receiptPath = imagePath,
                        rawOcrText = parseResult.rawOcrText,
                        source = parseResult.source,
                        amountText = initialAmountText,
                        amountStatus = parseResult.amountPaisa.status,
                        amountStatusMessage = parseResult.amountPaisa.errorMessage,
                        dateEpochMillis = parseResult.dateEpochMillis.value ?: System.currentTimeMillis(),
                        dateStatus = parseResult.dateEpochMillis.status,
                        dateStatusMessage = parseResult.dateEpochMillis.errorMessage,
                        transactionId = txnId,
                        transactionIdStatus = parseResult.transactionId.status,
                        transactionIdStatusMessage = parseResult.transactionId.errorMessage,
                        merchant = parseResult.merchant.value ?: "",
                        merchantStatus = parseResult.merchant.status,
                        merchantStatusMessage = parseResult.merchant.errorMessage,
                        feeText = initialFeeText,
                        feeStatus = parseResult.feePaisa.status,
                        remarks = parseResult.merchant.value ?: "",
                        duplicateEntry = duplicates.firstOrNull(),
                        isDuplicateDismissed = false,
                        isOcrPartiallyFailed = parseResult.amountPaisa.status == ValidationStatus.ERROR || parseResult.source == "unknown"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOcrPartiallyFailed = true,
                        amountStatus = ValidationStatus.ERROR,
                        amountStatusMessage = "Failed to process receipt: ${e.message}"
                    )
                }
            }
        }
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

    fun onCategorySelected(categoryId: Long) {
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                categoryStatus = ValidationStatus.VALID,
                categoryStatusMessage = null
            )
        }
    }

    fun onTransactionIdChanged(txnId: String) {
        viewModelScope.launch {
            val duplicates = if (txnId.isNotBlank()) {
                ledgerRepository.checkDuplicateTransaction(txnId.trim())
            } else emptyList()

            _uiState.update {
                it.copy(
                    transactionId = txnId,
                    transactionIdStatus = ValidationStatus.VALID,
                    transactionIdStatusMessage = null,
                    duplicateEntry = duplicates.firstOrNull(),
                    isDuplicateDismissed = false
                )
            }
        }
    }

    fun onMerchantChanged(merchant: String) {
        _uiState.update { it.copy(merchant = merchant) }
    }

    fun onFeeChanged(feeText: String) {
        _uiState.update { it.copy(feeText = feeText) }
    }

    fun onRemarksChanged(remarks: String) {
        _uiState.update { it.copy(remarks = remarks) }
    }

    fun onDismissDuplicateWarning() {
        _uiState.update { it.copy(isDuplicateDismissed = true) }
    }

    fun onViewExistingDuplicate() {
        val dupId = _uiState.value.duplicateEntry?.id ?: return
        viewModelScope.launch {
            _events.send(ConfirmScanEvent.NavigateToEntry(dupId))
        }
    }

    fun saveEntry() {
        val state = _uiState.value
        if (!state.isSaveEnabled) return

        val amountPaisa = Money.parsePaisa(state.amountText) ?: return
        val categoryId = state.selectedCategoryId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val note = when {
                state.remarks.isNotBlank() && state.merchant.isNotBlank() && state.remarks != state.merchant ->
                    "${state.merchant} - ${state.remarks}"
                state.remarks.isNotBlank() -> state.remarks
                else -> state.merchant
            }

            val entry = LedgerEntry(
                amountPaisa = amountPaisa,
                type = EntryType.EXPENSE,
                categoryId = categoryId,
                note = note,
                date = state.dateEpochMillis,
                source = "receipt:${state.source}",
                receiptPath = state.receiptPath,
                rawOcrText = state.rawOcrText,
                transactionId = state.transactionId.trim().ifBlank { null }
            )

            val result = ledgerRepository.saveEntry(entry)
            _uiState.update { it.copy(isSaving = false) }

            if (result.isSuccess) {
                _events.send(ConfirmScanEvent.SaveSuccess)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to save entry"
                _events.send(ConfirmScanEvent.ShowError(err))
            }
        }
    }
}
