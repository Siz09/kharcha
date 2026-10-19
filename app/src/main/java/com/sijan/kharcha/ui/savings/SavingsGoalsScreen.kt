package com.sijan.kharcha.ui.savings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.model.SavingsContribution
import com.sijan.kharcha.domain.model.SavingsGoal
import com.sijan.kharcha.domain.model.SavingsGoalSummary
import com.sijan.kharcha.domain.parser.Normalizer
import com.sijan.kharcha.ui.components.AmountText
import com.sijan.kharcha.ui.components.NepaliDatePickerDialog
import com.sijan.kharcha.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavingsGoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.savings_title), fontWeight = FontWeight.Bold) },
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
                onClick = viewModel::openAddGoalDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.savings_add_btn)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SavingsGoalsContent(
            uiState = uiState,
            onTabSelected = viewModel::setTabFilter,
            onDepositClicked = viewModel::openDepositDialog,
            onHistoryClicked = viewModel::openHistoryDialog,
            onEditClicked = viewModel::openEditGoalDialog,
            onDeleteClicked = viewModel::promptDeleteGoal,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )

        if (uiState.showAddEditDialog) {
            AddEditGoalDialog(
                goalToEdit = uiState.goalToEdit,
                onDismiss = viewModel::closeAddEditDialog,
                onSave = viewModel::saveGoal
            )
        }

        uiState.selectedGoalForDeposit?.let { goal ->
            DepositDialog(
                goal = goal,
                onDismiss = viewModel::closeDepositDialog,
                onConfirm = { amountPaisa, dateMillis, notes ->
                    viewModel.addDeposit(goal.id, amountPaisa, dateMillis, notes)
                }
            )
        }

        uiState.selectedGoalForHistory?.let { goal ->
            SavingsHistoryDialog(
                goal = goal,
                contributions = uiState.contributionsForHistory,
                onDismiss = viewModel::closeHistoryDialog,
                onDeleteContribution = viewModel::deleteContribution
            )
        }

        uiState.goalToDelete?.let { goal ->
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteDialog,
                title = { Text(stringResource(R.string.savings_delete_confirm_title)) },
                text = { Text(stringResource(R.string.savings_delete_confirm_desc)) },
                confirmButton = {
                    Button(onClick = viewModel::confirmDeleteGoal) {
                        Text(stringResource(R.string.savings_delete_btn))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteDialog) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SavingsGoalsContent(
    uiState: SavingsGoalsUiState,
    onTabSelected: (SavingsTabFilter) -> Unit,
    onDepositClicked: (SavingsGoal) -> Unit,
    onHistoryClicked: (SavingsGoal) -> Unit,
    onEditClicked: (SavingsGoal) -> Unit,
    onDeleteClicked: (SavingsGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SavingsSummaryCard(
            summary = uiState.summary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        TabRow(
            selectedTabIndex = uiState.tabFilter.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            SavingsTabFilter.entries.forEach { tab ->
                val tabText = when (tab) {
                    SavingsTabFilter.ALL -> stringResource(R.string.savings_all_tab)
                    SavingsTabFilter.ACTIVE -> stringResource(R.string.savings_active_tab)
                    SavingsTabFilter.COMPLETED -> stringResource(R.string.savings_completed_tab)
                }
                Tab(
                    selected = uiState.tabFilter == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(tabText) }
                )
            }
        }

        if (uiState.goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.savings_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.goals, key = { it.id }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onDepositClicked = { onDepositClicked(goal) },
                        onHistoryClicked = { onHistoryClicked(goal) },
                        onEditClicked = { onEditClicked(goal) },
                        onDeleteClicked = { onDeleteClicked(goal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsSummaryCard(
    summary: SavingsGoalSummary,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (summary.totalTargetPaisa > 0L) {
        (summary.totalSavedPaisa.toFloat() / summary.totalTargetPaisa.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val savingsCardDesc = stringResource(R.string.cd_savings_card)

    Card(
        modifier = modifier.semantics { contentDescription = savingsCardDesc },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_savings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${summary.overallProgressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(
                            R.string.savings_saved_of_target_format,
                            Money.formatPaisa(summary.totalSavedPaisa),
                            Money.formatPaisa(summary.totalTargetPaisa)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.savings_remaining_format, Money.formatPaisa(summary.remainingPaisa)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${summary.activeGoalsCount} ${stringResource(R.string.savings_active_tab)}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (summary.completedGoalsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IncomeGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${summary.completedGoalsCount} ${stringResource(R.string.savings_completed_tab)}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = IncomeGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    onDepositClicked: () -> Unit,
    onHistoryClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (goal.targetAmountPaisa > 0L) {
        (goal.savedAmountPaisa.toFloat() / goal.targetAmountPaisa.toFloat()).coerceIn(0f, 1f)
    } else 1f

    val now = System.currentTimeMillis()
    val targetDateDesc = goal.targetDateMillis?.let { targetDate ->
        val diffMillis = targetDate - now
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
        if (days >= 0) {
            stringResource(R.string.savings_days_left_format, days)
        } else {
            stringResource(R.string.savings_overdue_days_format, -days)
        }
    }

    val monthlyPace = if (!goal.isCompleted && goal.targetDateMillis != null && goal.targetDateMillis > now) {
        val daysLeft = TimeUnit.MILLISECONDS.toDays(goal.targetDateMillis - now).coerceAtLeast(1)
        val monthsLeft = (daysLeft.toFloat() / 30f).coerceAtLeast(1f)
        val pacePaisa = (goal.remainingPaisa.toFloat() / monthsLeft).toLong()
        Money.formatPaisa(pacePaisa)
    } else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (goal.isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IncomeGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.savings_completed_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = IncomeGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${goal.progressPercent}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (goal.isCompleted) IncomeGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AmountText(
                        amountPaisa = goal.savedAmountPaisa,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = " / ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AmountText(
                        amountPaisa = goal.targetAmountPaisa,
                        style = MaterialTheme.typography.bodyMedium,
                        colorOverride = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!goal.isCompleted) {
                    Text(
                        text = stringResource(R.string.savings_remaining_format, Money.formatPaisa(goal.remainingPaisa)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (targetDateDesc != null || monthlyPace != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    targetDateDesc?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    monthlyPace?.let {
                        Text(
                            text = stringResource(R.string.savings_monthly_pace_format, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            goal.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = onHistoryClicked) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.savings_history_title, goal.name),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onEditClicked) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.savings_edit_btn),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDeleteClicked) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.savings_delete_btn),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (!goal.isCompleted) {
                    Button(
                        onClick = onDepositClicked,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.savings_record_deposit))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditGoalDialog(
    goalToEdit: SavingsGoal?,
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmountPaisa: Long, targetDateMillis: Long?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(goalToEdit?.name ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (goalToEdit != null) (goalToEdit.targetAmountPaisa / 100L).toString() else ""
        )
    }
    var targetDateMillis by remember { mutableStateOf(goalToEdit?.targetDateMillis) }
    var notes by remember { mutableStateOf(goalToEdit?.notes ?: "") }
    var showNepaliDatePicker by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (goalToEdit == null) stringResource(R.string.savings_add_btn)
                else stringResource(R.string.savings_edit_btn)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.savings_name_label)) },
                    placeholder = { Text(stringResource(R.string.savings_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError && name.isBlank(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.savings_target_amount_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError && (amountText.isBlank() || (Normalizer.parsePaisa(amountText) ?: 0L) <= 0L),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetDateMillis?.let { dateFormat.format(Date(it)) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.savings_target_date_label)) },
                    trailingIcon = {
                        IconButton(onClick = { showNepaliDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.nepali_date_picker_title)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNepaliDatePicker = true }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.savings_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPaisa = Normalizer.parsePaisa(amountText)
                    if (name.isNotBlank() && parsedPaisa != null && parsedPaisa > 0L) {
                        onSave(name, parsedPaisa, targetDateMillis, notes.ifBlank { null })
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(stringResource(R.string.savings_save_goal))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    if (showNepaliDatePicker) {
        NepaliDatePickerDialog(
            initialEpochMillis = targetDateMillis ?: System.currentTimeMillis(),
            onDateSelected = { selectedDate ->
                targetDateMillis = selectedDate
                showNepaliDatePicker = false
            },
            onDismissRequest = { showNepaliDatePicker = false }
        )
    }
}

@Composable
private fun DepositDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onConfirm: (amountPaisa: Long, dateMillis: Long, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.savings_deposit_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.savings_deposit_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError && (amountText.isBlank() || (Normalizer.parsePaisa(amountText) ?: 0L) <= 0L),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dateFormat.format(Date(dateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.savings_deposit_date)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.nepali_date_picker_title)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.savings_deposit_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPaisa = Normalizer.parsePaisa(amountText)
                    if (parsedPaisa != null && parsedPaisa > 0L) {
                        onConfirm(parsedPaisa, dateMillis, notes.ifBlank { null })
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(stringResource(R.string.savings_deposit_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    if (showDatePicker) {
        NepaliDatePickerDialog(
            initialEpochMillis = dateMillis,
            onDateSelected = { selectedDate ->
                dateMillis = selectedDate
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}

@Composable
private fun SavingsHistoryDialog(
    goal: SavingsGoal,
    contributions: List<SavingsContribution>,
    onDismiss: () -> Unit,
    onDeleteContribution: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.savings_history_title, goal.name)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                if (contributions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.savings_no_contributions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(contributions, key = { it.id }) { contribution ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        AmountText(
                                            amountPaisa = contribution.amountPaisa,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            colorOverride = IncomeGreen
                                        )
                                        Text(
                                            text = dateFormat.format(Date(contribution.dateMillis)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        contribution.notes?.takeIf { it.isNotBlank() }?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteContribution(contribution.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.savings_delete_contribution),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
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
