package com.sijan.kharcha.ui.udhar

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.parser.Normalizer
import com.sijan.kharcha.domain.model.UdharRecord
import com.sijan.kharcha.domain.model.UdharSettlement
import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharSummary
import com.sijan.kharcha.domain.model.UdharType
import com.sijan.kharcha.ui.components.AmountText
import com.sijan.kharcha.ui.theme.ExpenseRed
import com.sijan.kharcha.ui.theme.IncomeGreen
import com.sijan.kharcha.ui.theme.KharchaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdharScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UdharViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorDismissed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.udhar_title), fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onAddClicked,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.udhar_add_btn)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        UdharContent(
            uiState = uiState,
            onTabSelected = viewModel::onTabFilterChanged,
            onStatusFilterSelected = viewModel::onStatusFilterChanged,
            onSettleClicked = viewModel::onSettleClicked,
            onViewHistoryClicked = viewModel::onViewHistoryClicked,
            onDeleteClicked = viewModel::onDeleteUdharClicked,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )

        if (uiState.showAddDialog) {
            AddUdharDialog(
                onDismiss = viewModel::onDismissAddDialog,
                onConfirm = viewModel::onCreateUdhar
            )
        }

        uiState.selectedUdharForSettlement?.let { udhar ->
            RecordSettlementDialog(
                udhar = udhar,
                onDismiss = viewModel::onDismissSettleDialog,
                onConfirm = viewModel::onConfirmSettlement
            )
        }

        uiState.selectedUdharForHistory?.let { udhar ->
            UdharHistoryDialog(
                udhar = udhar,
                settlements = uiState.settlementsForHistory,
                onDismiss = viewModel::onDismissHistory,
                onDeleteSettlement = viewModel::onDeleteSettlement
            )
        }

        uiState.udharToDelete?.let { udhar ->
            AlertDialog(
                onDismissRequest = viewModel::onDismissDeleteConfirm,
                title = { Text(stringResource(R.string.udhar_delete_confirm_title)) },
                text = { Text(stringResource(R.string.udhar_delete_confirm_desc)) },
                confirmButton = {
                    TextButton(onClick = viewModel::onConfirmDeleteUdhar) {
                        Text(stringResource(R.string.udhar_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismissDeleteConfirm) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdharContent(
    uiState: UdharUiState,
    onTabSelected: (UdharTabFilter) -> Unit,
    onStatusFilterSelected: (UdharStatus?) -> Unit,
    onSettleClicked: (UdharRecord) -> Unit,
    onViewHistoryClicked: (UdharRecord) -> Unit,
    onDeleteClicked: (UdharRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Summary Cards
        UdharSummaryCards(summary = uiState.summary)

        // Tab Selector
        PrimaryTabRow(
            selectedTabIndex = uiState.tabFilter.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = uiState.tabFilter == UdharTabFilter.ALL,
                onClick = { onTabSelected(UdharTabFilter.ALL) },
                text = { Text(stringResource(R.string.udhar_all_tab)) }
            )
            Tab(
                selected = uiState.tabFilter == UdharTabFilter.LENT,
                onClick = { onTabSelected(UdharTabFilter.LENT) },
                text = { Text(stringResource(R.string.udhar_lent_tab)) }
            )
            Tab(
                selected = uiState.tabFilter == UdharTabFilter.BORROWED,
                onClick = { onTabSelected(UdharTabFilter.BORROWED) },
                text = { Text(stringResource(R.string.udhar_borrowed_tab)) }
            )
        }

        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.statusFilter == null,
                onClick = { onStatusFilterSelected(null) },
                label = { Text(stringResource(R.string.udhar_all_tab)) }
            )
            FilterChip(
                selected = uiState.statusFilter == UdharStatus.PENDING,
                onClick = { onStatusFilterSelected(UdharStatus.PENDING) },
                label = { Text(stringResource(R.string.udhar_pending_filter)) }
            )
            FilterChip(
                selected = uiState.statusFilter == UdharStatus.SETTLED,
                onClick = { onStatusFilterSelected(UdharStatus.SETTLED) },
                label = { Text(stringResource(R.string.udhar_settled_filter)) }
            )
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.udhar_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.items, key = { it.id }) { udhar ->
                    UdharCard(
                        udhar = udhar,
                        onSettleClicked = { onSettleClicked(udhar) },
                        onViewHistoryClicked = { onViewHistoryClicked(udhar) },
                        onDeleteClicked = { onDeleteClicked(udhar) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
fun UdharSummaryCards(
    summary: UdharSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.udhar_lent_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AmountText(
                        amountPaisa = summary.totalLentPaisa,
                        colorOverride = IncomeGreen,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.udhar_borrowed_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AmountText(
                        amountPaisa = summary.totalBorrowedPaisa,
                        colorOverride = ExpenseRed,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.udhar_net_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AmountText(
                    amountPaisa = summary.netReceivablePaisa,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun UdharCard(
    udhar: UdharRecord,
    onSettleClicked: () -> Unit,
    onViewHistoryClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (udhar.amountPaisa > 0L) {
        (udhar.settledPaisa.toFloat() / udhar.amountPaisa.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = udhar.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        udhar.phoneNumber?.let { phone ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = if (udhar.type == UdharType.LENT) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (udhar.type == UdharType.LENT) {
                            stringResource(R.string.udhar_lent_tab)
                        } else {
                            stringResource(R.string.udhar_borrowed_tab)
                        },
                        color = if (udhar.type == UdharType.LENT) IncomeGreen else ExpenseRed,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.udhar_remaining_format, Money.formatPaisa(udhar.remainingPaisa)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (udhar.isFullySettled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total: ${Money.formatPaisa(udhar.amountPaisa)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = when (udhar.status) {
                        UdharStatus.SETTLED -> IncomeGreen.copy(alpha = 0.15f)
                        UdharStatus.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer
                        UdharStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (udhar.status) {
                            UdharStatus.SETTLED -> stringResource(R.string.udhar_status_settled)
                            UdharStatus.PARTIAL -> stringResource(R.string.udhar_status_partial)
                            UdharStatus.PENDING -> stringResource(R.string.udhar_status_pending)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (udhar.isFullySettled) IncomeGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            udhar.dueDate?.let { due ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${stringResource(R.string.udhar_due_date)}: ${dateFormat.format(Date(due))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (udhar.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = udhar.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onViewHistoryClicked) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.udhar_history_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.udhar_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                if (!udhar.isFullySettled) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onSettleClicked,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.udhar_record_settlement))
                    }
                }
            }
        }
    }
}

@Composable
fun AddUdharDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, type: UdharType, amountPaisa: Long, dueDate: Long?, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(UdharType.LENT) }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.udhar_add_btn)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lent vs Borrowed switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == UdharType.LENT,
                        onClick = { type = UdharType.LENT },
                        label = { Text(stringResource(R.string.udhar_type_lent)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == UdharType.BORROWED,
                        onClick = { type = UdharType.BORROWED },
                        label = { Text(stringResource(R.string.udhar_type_borrowed)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.udhar_person_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError && name.isBlank(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.udhar_phone_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.udhar_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError && (amountText.isBlank() || (Normalizer.parsePaisa(amountText) ?: 0L) <= 0L),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.udhar_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPaisa = Normalizer.parsePaisa(amountText) ?: 0L
                    if (name.isNotBlank() && parsedPaisa > 0L) {
                        onConfirm(name, phone.takeIf { it.isNotBlank() }, type, parsedPaisa, null, notes)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(stringResource(R.string.udhar_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun RecordSettlementDialog(
    udhar: UdharRecord,
    onDismiss: () -> Unit,
    onConfirm: (amountPaisa: Long, date: Long, notes: String?) -> Unit
) {
    val remainingRupees = udhar.remainingPaisa / 100
    val remainingRem = udhar.remainingPaisa % 100
    val defaultAmount = if (remainingRem == 0L) remainingRupees.toString() else String.format(Locale.US, "%d.%02d", remainingRupees, remainingRem)

    var amountText by remember { mutableStateOf(defaultAmount) }
    var notes by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.udhar_settlement_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${udhar.personName} • Remaining: ${Money.formatPaisa(udhar.remainingPaisa)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.udhar_settlement_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError && (amountText.isBlank() || (Normalizer.parsePaisa(amountText) ?: 0L) <= 0L),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.udhar_settlement_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPaisa = Normalizer.parsePaisa(amountText) ?: 0L
                    if (parsedPaisa > 0L) {
                        onConfirm(parsedPaisa, System.currentTimeMillis(), notes.takeIf { it.isNotBlank() })
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(stringResource(R.string.udhar_settlement_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun UdharHistoryDialog(
    udhar: UdharRecord,
    settlements: List<UdharSettlement>,
    onDismiss: () -> Unit,
    onDeleteSettlement: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.udhar_history_title, udhar.personName)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                if (settlements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.udhar_no_settlements),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(settlements, key = { it.id }) { s ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = Money.formatPaisa(s.amountPaisa),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                        Text(
                                            text = dateFormat.format(Date(s.dateEpochMillis)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        s.notes?.let { note ->
                                            Text(
                                                text = note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteSettlement(s.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.udhar_delete_settlement),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
