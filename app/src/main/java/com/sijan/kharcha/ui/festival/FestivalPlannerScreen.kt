package com.sijan.kharcha.ui.festival

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.FestivalBudget
import com.sijan.kharcha.domain.model.FestivalType
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.parser.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivalPlannerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FestivalPlannerViewModel = hiltViewModel()
) {
    val activeBudgets by viewModel.activeBudgets.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.festival_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.festival_add_btn))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (activeBudgets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.festival_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeBudgets, key = { it.budget.id }) { item ->
                        val b = item.budget
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = b.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = b.festivalType.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteFestivalBudget(b.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.festival_progress_format,
                                            Money.formatPaisa(item.spentPaisa),
                                            Money.formatPaisa(b.targetBudgetPaisa),
                                            item.percentageUsed
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val statusColor = if (item.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    val statusText = if (item.isOverBudget) {
                                        stringResource(R.string.festival_over_format, Money.formatPaisa(-item.remainingPaisa))
                                    } else {
                                        stringResource(R.string.festival_remaining_format, Money.formatPaisa(item.remainingPaisa))
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val progressRatio = (item.percentageUsed.toFloat() / 100f).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (item.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddFestivalBudgetDialog(
                onDismiss = { showAddDialog = false },
                onSave = { budget ->
                    viewModel.saveFestivalBudget(budget)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun AddFestivalBudgetDialog(
    onDismiss: () -> Unit,
    onSave: (FestivalBudget) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetBudgetInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(FestivalType.DASHAIN) }

    val parsedTarget = Normalizer.parsePaisa(targetBudgetInput) ?: 0L
    val isValid = name.isNotBlank() && parsedTarget > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.festival_add_btn), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.festival_name_label)) },
                    placeholder = { Text(stringResource(R.string.festival_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetBudgetInput,
                    onValueChange = { targetBudgetInput = it },
                    label = { Text(stringResource(R.string.festival_target_budget)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.field_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        FestivalBudget(
                            name = name.trim(),
                            festivalType = selectedType,
                            targetBudgetPaisa = parsedTarget,
                            startDateEpoch = now,
                            endDateEpoch = now + (30L * 24L * 3600L * 1000L), // 30-day window
                            notes = notes.ifBlank { null }
                        )
                    )
                },
                enabled = isValid
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}
