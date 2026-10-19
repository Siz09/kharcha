package com.sijan.kharcha.ui.entries

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import com.sijan.kharcha.ui.components.CategoryPicker
import com.sijan.kharcha.ui.components.DateFormatter
import com.sijan.kharcha.ui.components.FieldWithStatus
import com.sijan.kharcha.ui.components.NepaliDatePickerDialog
import com.sijan.kharcha.ui.scan.ReceiptPreviewCard
import com.sijan.kharcha.ui.theme.KharchaTheme

@Composable
fun AddEditEntryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditEntryEvent.SaveSuccess -> onNavigateBack()
                is AddEditEntryEvent.DeleteSuccess -> onNavigateBack()
                is AddEditEntryEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AddEditEntryContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onTypeChanged = viewModel::onTypeChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onCategorySelected = { viewModel.onCategorySelected(it.id) },
        onDateChanged = viewModel::onDateChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onTransactionIdChanged = viewModel::onTransactionIdChanged,
        onDeleteClicked = viewModel::onDeleteClicked,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onSaveClicked = viewModel::saveEntry,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryContent(
    uiState: AddEditEntryUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onTypeChanged: (EntryType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onDateChanged: (Long) -> Unit,
    onNoteChanged: (String) -> Unit,
    onTransactionIdChanged: (String) -> Unit,
    onDeleteClicked: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showNepaliDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditMode) R.string.edit_entry_title else R.string.add_entry_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = onDeleteClicked) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_entry),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
                CircularProgressIndicator()
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
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.type == EntryType.EXPENSE,
                        onClick = { onTypeChanged(EntryType.EXPENSE) },
                        label = { Text(text = stringResource(R.string.entry_type_expense)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.type == EntryType.INCOME,
                        onClick = { onTypeChanged(EntryType.INCOME) },
                        label = { Text(text = stringResource(R.string.entry_type_income)) },
                        modifier = Modifier.weight(1f)
                    )
                }

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

                // Category
                CategoryPicker(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = onCategorySelected,
                    isError = uiState.categoryStatus == ValidationStatus.ERROR,
                    errorMessage = uiState.categoryStatusMessage
                )

                // Date
                OutlinedTextField(
                    value = DateFormatter.formatDualDate(uiState.dateEpochMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = stringResource(R.string.field_date)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
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
                        .clickable { showDatePicker = true }
                )

                // Note / Description
                FieldWithStatus(
                    value = uiState.note,
                    onValueChange = onNoteChanged,
                    label = stringResource(R.string.field_note),
                    placeholder = stringResource(R.string.field_note_hint),
                    singleLine = false,
                    maxLines = 3
                )

                // Transaction ID (optional)
                FieldWithStatus(
                    value = uiState.transactionId,
                    onValueChange = onTransactionIdChanged,
                    label = stringResource(R.string.field_transaction_id),
                    placeholder = stringResource(R.string.field_transaction_id_hint)
                )

                // Attached receipt image preview if editing a scanned entry
                if (!uiState.receiptPath.isNullOrBlank()) {
                    ReceiptPreviewCard(receiptPath = uiState.receiptPath)
                }

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
                    Row {
                        TextButton(onClick = {
                            showDatePicker = false
                            showNepaliDatePicker = true
                        }) {
                            Text(text = stringResource(R.string.nepali_date_picker_switch_bs))
                        }
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(text = stringResource(R.string.action_cancel))
                        }
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showNepaliDatePicker) {
            NepaliDatePickerDialog(
                initialEpochMillis = uiState.dateEpochMillis,
                onDateSelected = onDateChanged,
                onDismissRequest = { showNepaliDatePicker = false }
            )
        }

        if (uiState.showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = onDeleteDismissed,
                title = { Text(text = stringResource(R.string.delete_entry_dialog_title)) },
                text = { Text(text = stringResource(R.string.delete_entry_dialog_message)) },
                confirmButton = {
                    Button(
                        onClick = onDeleteConfirmed,
                        enabled = !uiState.isDeleting
                    ) {
                        Text(text = stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDeleteDismissed) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun AddEditEntryPreviewLight() {
    KharchaTheme {
        AddEditEntryContent(
            uiState = AddEditEntryUiState(
                amountText = "120.00",
                categories = listOf(
                    Category(id = 1, name = "Food & Dining", type = EntryType.EXPENSE)
                ),
                selectedCategoryId = 1,
                note = "Lunch at cafe"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onTypeChanged = {},
            onAmountChanged = {},
            onCategorySelected = {},
            onDateChanged = {},
            onNoteChanged = {},
            onTransactionIdChanged = {},
            onDeleteClicked = {},
            onDeleteDismissed = {},
            onDeleteConfirmed = {},
            onSaveClicked = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AddEditEntryPreviewDark() {
    KharchaTheme {
        AddEditEntryContent(
            uiState = AddEditEntryUiState(
                isEditMode = true,
                amountText = "3500.00",
                type = EntryType.INCOME,
                categories = listOf(
                    Category(id = 2, name = "Salary", type = EntryType.INCOME)
                ),
                selectedCategoryId = 2,
                note = "Part-time consulting"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onTypeChanged = {},
            onAmountChanged = {},
            onCategorySelected = {},
            onDateChanged = {},
            onNoteChanged = {},
            onTransactionIdChanged = {},
            onDeleteClicked = {},
            onDeleteDismissed = {},
            onDeleteConfirmed = {},
            onSaveClicked = {}
        )
    }
}
