package com.sijan.kharcha.ui.bills

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.sijan.kharcha.domain.model.Bill
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.ui.components.AmountText
import com.sijan.kharcha.ui.components.EmptyState
import com.sijan.kharcha.ui.theme.KharchaTheme
import com.sijan.kharcha.ui.theme.KharchaThemeDefaults
import kotlinx.coroutines.launch

@Composable
fun BillsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BillsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAddBillClick = viewModel::openAddDialog,
        onEditBillClick = viewModel::openEditDialog,
        onPayBill = { billId, billName ->
            viewModel.payBill(billId) { result ->
                scope.launch {
                    if (result.isSuccess) {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.bills_paid_success, billName)
                        )
                    }
                }
            }
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    if (uiState.isAddEditSheetOpen) {
        AddEditBillDialog(
            bill = uiState.editingBill,
            categories = uiState.categories,
            onDismiss = viewModel::dismissDialog,
            onSave = { name, amountPaisa, categoryId, dueDay, isAutoReminder ->
                viewModel.saveBill(
                    name = name,
                    amountPaisa = amountPaisa,
                    categoryId = categoryId,
                    dueDay = dueDay,
                    isAutoReminder = isAutoReminder,
                    billId = uiState.editingBill?.id ?: 0L
                )
            },
            onDelete = { billId ->
                viewModel.deleteBill(billId)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreenContent(
    uiState: BillsUiState,
    onNavigateBack: () -> Unit,
    onAddBillClick: () -> Unit,
    onEditBillClick: (Bill) -> Unit,
    onPayBill: (Long, String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.bills_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
                onClick = onAddBillClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_bill)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.bills_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmountText(
                                amountPaisa = uiState.totalMonthlyBillsPaisa,
                                type = null,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.home_net_balance),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmountText(
                                amountPaisa = uiState.remainingUnpaidPaisa,
                                type = if (uiState.remainingUnpaidPaisa > 0) EntryType.EXPENSE else null,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            if (uiState.bills.isEmpty()) {
                item {
                    EmptyState(
                        message = stringResource(R.string.bills_empty),
                        modifier = Modifier.padding(vertical = 48.dp)
                    )
                }
            } else {
                items(
                    items = uiState.bills,
                    key = { it.bill.id }
                ) { itemState ->
                    BillCard(
                        itemState = itemState,
                        onPay = { onPayBill(itemState.bill.id, itemState.bill.name) },
                        onEdit = { onEditBillClick(itemState.bill) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun BillCard(
    itemState: BillItemState,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KharchaThemeDefaults.colors
    val bill = itemState.bill

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = itemState.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AmountText(
                    amountPaisa = bill.amountPaisa,
                    type = EntryType.EXPENSE,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                val (badgeBg, badgeText, badgeColor) = when {
                    itemState.isPaidThisMonth -> Triple(
                        colors.incomeContainer,
                        stringResource(R.string.bills_paid_this_month),
                        colors.income
                    )
                    itemState.isOverdue -> Triple(
                        colors.errorContainer,
                        stringResource(R.string.bills_overdue),
                        colors.error
                    )
                    itemState.daysUntilDue == 0 -> Triple(
                        colors.warningContainer,
                        stringResource(R.string.bills_due_today),
                        colors.warning
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.surfaceVariant,
                        stringResource(R.string.bills_due_in_days, itemState.daysUntilDue),
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeColor
                    )
                }

                // Action
                if (!itemState.isPaidThisMonth) {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.bills_pay_action),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.income,
                            modifier = Modifier.size(18.dp)
                        )
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.action_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(
    bill: Bill?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (name: String, amountPaisa: Long, categoryId: Long, dueDay: Int, isAutoReminder: Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    var name by remember(bill) { mutableStateOf(bill?.name ?: "") }
    var amountText by remember(bill) {
        mutableStateOf(if (bill != null && bill.amountPaisa > 0) (bill.amountPaisa / 100).toString() else "")
    }
    var selectedCategoryId by remember(bill, categories) {
        mutableLongStateOf(bill?.categoryId ?: (categories.firstOrNull()?.id ?: 0L))
    }
    var dueDay by remember(bill) { mutableIntStateOf(bill?.dueDayOfMonth ?: 1) }
    var isAutoReminder by remember(bill) { mutableStateOf(bill?.isAutoReminder ?: true) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (bill == null) stringResource(R.string.bills_add) else stringResource(R.string.bills_edit)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(R.string.bills_name_label)) },
                    placeholder = { Text(text = stringResource(R.string.bills_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text(text = stringResource(R.string.field_amount)) },
                    prefix = { Text(text = stringResource(R.string.field_currency_prefix)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category selector
                if (categories.isNotEmpty()) {
                    val currentCatName = categories.find { it.id == selectedCategoryId }?.name ?: ""
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentCatName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(text = stringResource(R.string.field_category)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(text = cat.name) },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Due Day selector
                OutlinedTextField(
                    value = dueDay.toString(),
                    onValueChange = { input ->
                        val parsed = input.filter { it.isDigit() }.toIntOrNull()
                        if (parsed != null && parsed in 1..31) {
                            dueDay = parsed
                        }
                    },
                    label = { Text(text = stringResource(R.string.bills_day_of_month_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.bills_notification_title),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isAutoReminder,
                        onCheckedChange = { isAutoReminder = it }
                    )
                }
            }
        },
        confirmButton = {
            val amountValue = amountText.toLongOrNull() ?: 0L
            val isValid = name.isNotBlank() && amountValue > 0L && selectedCategoryId > 0L
            Button(
                onClick = {
                    onSave(name, amountValue * 100L, selectedCategoryId, dueDay, isAutoReminder)
                },
                enabled = isValid
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bill != null) {
                    IconButton(onClick = { onDelete(bill.id); onDismiss() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        }
    )
}

@Preview(name = "Light Mode")
@Composable
private fun BillsScreenPreview() {
    KharchaTheme {
        Surface {
            BillsScreenContent(
                uiState = BillsUiState(
                    bills = listOf(
                        BillItemState(
                            bill = Bill(
                                id = 1L,
                                name = "WorldLink WiFi",
                                amountPaisa = 150000L,
                                categoryId = 1L,
                                dueDayOfMonth = 15
                            ),
                            categoryName = "Utilities",
                            isPaidThisMonth = false,
                            daysUntilDue = 2,
                            isOverdue = false
                        ),
                        BillItemState(
                            bill = Bill(
                                id = 2L,
                                name = "Netflix Subscription",
                                amountPaisa = 120000L,
                                categoryId = 2L,
                                dueDayOfMonth = 5
                            ),
                            categoryName = "Entertainment",
                            isPaidThisMonth = true,
                            daysUntilDue = -10,
                            isOverdue = false
                        )
                    ),
                    totalMonthlyBillsPaisa = 270000L,
                    remainingUnpaidPaisa = 150000L,
                    isLoading = false
                ),
                onNavigateBack = {},
                onAddBillClick = {},
                onEditBillClick = {},
                onPayBill = { _, _ -> }
            )
        }
    }
}
