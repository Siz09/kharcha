package com.sijan.kharcha.ui.scan

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import com.sijan.kharcha.ui.components.CategoryPicker
import com.sijan.kharcha.ui.components.DateFormatter
import com.sijan.kharcha.ui.components.FieldWithStatus
import com.sijan.kharcha.ui.theme.KharchaTheme

@Composable
fun ConfirmScanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfirmScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ConfirmScanEvent.SaveSuccess -> onNavigateBack()
                is ConfirmScanEvent.NavigateToEntry -> onNavigateToEntry(event.entryId)
                is ConfirmScanEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    ConfirmScanContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onAmountChanged = viewModel::onAmountChanged,
        onDateChanged = viewModel::onDateChanged,
        onCategorySelected = { viewModel.onCategorySelected(it.id) },
        onTransactionIdChanged = viewModel::onTransactionIdChanged,
        onMerchantChanged = viewModel::onMerchantChanged,
        onFeeChanged = viewModel::onFeeChanged,
        onRemarksChanged = viewModel::onRemarksChanged,
        onDismissDuplicateWarning = viewModel::onDismissDuplicateWarning,
        onViewExistingDuplicate = viewModel::onViewExistingDuplicate,
        onSaveClicked = viewModel::saveEntry,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmScanContent(
    uiState: ConfirmScanUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onDateChanged: (Long) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onTransactionIdChanged: (String) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onFeeChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onDismissDuplicateWarning: () -> Unit,
    onViewExistingDuplicate: () -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.confirm_scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onSaveClicked,
                    enabled = uiState.isSaveEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = stringResource(R.string.action_save))
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.confirm_scan_processing),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReceiptPreviewCard(receiptPath = uiState.receiptPath)

                if (uiState.duplicateEntry != null && !uiState.isDuplicateDismissed) {
                    DuplicateWarningCard(
                        transactionId = uiState.transactionId,
                        onViewExisting = onViewExistingDuplicate,
                        onSaveAnyway = onDismissDuplicateWarning
                    )
                }

                if (uiState.isOcrPartiallyFailed) {
                    OcrPartialFailureCard()
                }

                ConfirmScanForm(
                    uiState = uiState,
                    onAmountChanged = onAmountChanged,
                    onDatePickerClick = { showDatePicker = true },
                    onCategorySelected = onCategorySelected,
                    onTransactionIdChanged = onTransactionIdChanged,
                    onMerchantChanged = onMerchantChanged,
                    onFeeChanged = onFeeChanged,
                    onRemarksChanged = onRemarksChanged
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = uiState.dateEpochMillis
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { onDateChanged(it) }
                        showDatePicker = false
                    }) {
                        Text(text = stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun ReceiptPreviewCard(
    receiptPath: String?,
    modifier: Modifier = Modifier
) {
    if (receiptPath.isNullOrBlank()) return

    val bitmap = remember(receiptPath) {
        BitmapFactory.decodeFile(receiptPath)
    }

    if (bitmap != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.confirm_scan_preview_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_receipt_preview),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun DuplicateWarningCard(
    transactionId: String,
    onViewExisting: () -> Unit,
    onSaveAnyway: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.confirm_scan_duplicate_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.confirm_scan_duplicate_warning, transactionId),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onViewExisting) {
                    Text(text = stringResource(R.string.confirm_scan_view_existing))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSaveAnyway) {
                    Text(text = stringResource(R.string.confirm_scan_save_anyway))
                }
            }
        }
    }
}

@Composable
fun OcrPartialFailureCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.confirm_scan_ocr_failed_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.confirm_scan_ocr_failed_desc),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ConfirmScanForm(
    uiState: ConfirmScanUiState,
    onAmountChanged: (String) -> Unit,
    onDatePickerClick: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onTransactionIdChanged: (String) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onFeeChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Amount
        FieldWithStatus(
            value = uiState.amountText,
            onValueChange = onAmountChanged,
            label = stringResource(R.string.field_amount),
            placeholder = stringResource(R.string.field_amount_hint),
            leadingIcon = { Text(text = stringResource(R.string.field_currency_prefix)) },
            status = uiState.amountStatus,
            statusMessage = uiState.amountStatusMessage,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Category Picker
        CategoryPicker(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = onCategorySelected,
            isError = uiState.categoryStatus == ValidationStatus.ERROR,
            errorMessage = uiState.categoryStatusMessage
        )

        // Date Field
        OutlinedTextField(
            value = DateFormatter.formatDate(uiState.dateEpochMillis),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(R.string.field_date)) },
            trailingIcon = {
                IconButton(onClick = onDatePickerClick) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = stringResource(R.string.cd_select_date)
                    )
                }
            },
            isError = uiState.dateStatus == ValidationStatus.ERROR,
            supportingText = if (uiState.dateStatus == ValidationStatus.ERROR && uiState.dateStatusMessage != null) {
                { Text(text = uiState.dateStatusMessage) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDatePickerClick() }
        )

        // Merchant / Payee
        FieldWithStatus(
            value = uiState.merchant,
            onValueChange = onMerchantChanged,
            label = stringResource(R.string.field_merchant),
            placeholder = stringResource(R.string.field_merchant_hint),
            status = uiState.merchantStatus,
            statusMessage = uiState.merchantStatusMessage
        )

        // Transaction ID
        FieldWithStatus(
            value = uiState.transactionId,
            onValueChange = onTransactionIdChanged,
            label = stringResource(R.string.field_transaction_id),
            placeholder = stringResource(R.string.field_transaction_id_hint),
            status = uiState.transactionIdStatus,
            statusMessage = uiState.transactionIdStatusMessage
        )

        // Fee
        FieldWithStatus(
            value = uiState.feeText,
            onValueChange = onFeeChanged,
            label = stringResource(R.string.field_fee),
            placeholder = stringResource(R.string.field_fee_hint),
            leadingIcon = { Text(text = stringResource(R.string.field_currency_prefix)) },
            status = uiState.feeStatus,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // Remarks / Notes
        FieldWithStatus(
            value = uiState.remarks,
            onValueChange = onRemarksChanged,
            label = stringResource(R.string.field_note),
            placeholder = stringResource(R.string.field_note_hint),
            singleLine = false,
            maxLines = 3
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun ConfirmScanPreviewLight() {
    KharchaTheme {
        ConfirmScanContent(
            uiState = ConfirmScanUiState(
                amountText = "1500.00",
                merchant = "Nepal Electricity Authority",
                transactionId = "TXN998811",
                categories = listOf(Category(id = 1, name = "Bills", type = EntryType.EXPENSE)),
                selectedCategoryId = 1
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onAmountChanged = {},
            onDateChanged = {},
            onCategorySelected = {},
            onTransactionIdChanged = {},
            onMerchantChanged = {},
            onFeeChanged = {},
            onRemarksChanged = {},
            onDismissDuplicateWarning = {},
            onViewExistingDuplicate = {},
            onSaveClicked = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ConfirmScanPreviewDark() {
    KharchaTheme {
        ConfirmScanContent(
            uiState = ConfirmScanUiState(
                amountText = "500.00",
                merchant = "Bhatbhateni",
                transactionId = "BB12345",
                duplicateEntry = LedgerEntry(
                    id = 42L,
                    amountPaisa = 50000L,
                    type = EntryType.EXPENSE,
                    categoryId = 1L,
                    date = System.currentTimeMillis(),
                    transactionId = "BB12345"
                ),
                categories = listOf(Category(id = 1, name = "Shopping", type = EntryType.EXPENSE)),
                selectedCategoryId = 1
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onAmountChanged = {},
            onDateChanged = {},
            onCategorySelected = {},
            onTransactionIdChanged = {},
            onMerchantChanged = {},
            onFeeChanged = {},
            onRemarksChanged = {},
            onDismissDuplicateWarning = {},
            onViewExistingDuplicate = {},
            onSaveClicked = {}
        )
    }
}
