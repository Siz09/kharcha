package com.sijan.kharcha.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.parser.Normalizer

@Composable
fun AccountReconciliationDialog(
    accountName: String,
    currentComputedBalancePaisa: Long,
    unclearedEntries: List<LedgerEntry>,
    onDismiss: () -> Unit,
    onConfirmReconcile: (statementBalancePaisa: Long, clearedEntryIds: List<Long>, createAdjustment: Boolean) -> Unit
) {
    var statementBalanceInput by remember { mutableStateOf("") }
    var createAdjustment by remember { mutableStateOf(false) }
    val selectedEntryIds = remember { mutableStateListOf<Long>() }

    val parsedStatementBalance = Normalizer.parsePaisa(statementBalanceInput) ?: 0L
    val differencePaisa = parsedStatementBalance - currentComputedBalancePaisa
    val isValid = statementBalanceInput.isNotBlank() && (differencePaisa == 0L || createAdjustment)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${stringResource(R.string.reconcile_title)}: $accountName",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.reconcile_computed_balance, Money.formatPaisa(currentComputedBalancePaisa)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (statementBalanceInput.isNotBlank()) {
                            val diffColor = if (differencePaisa == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            val sign = if (differencePaisa > 0) "+" else ""
                            Text(
                                text = "${stringResource(R.string.reconcile_difference, sign + Money.formatPaisa(differencePaisa))}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = diffColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = statementBalanceInput,
                    onValueChange = { statementBalanceInput = it },
                    label = { Text(stringResource(R.string.reconcile_statement_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (unclearedEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.reconcile_uncleared_entries, unclearedEntries.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(unclearedEntries) { entry ->
                            val isSelected = selectedEntryIds.contains(entry.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedEntryIds.remove(entry.id)
                                        else selectedEntryIds.add(entry.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedEntryIds.add(entry.id)
                                        else selectedEntryIds.remove(entry.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = entry.note.ifBlank { "Transaction #${entry.id}" }, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = Money.formatPaisa(entry.amountPaisa), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (differencePaisa != 0L && statementBalanceInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { createAdjustment = !createAdjustment },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = createAdjustment,
                            onCheckedChange = { createAdjustment = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.reconcile_create_adjustment),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmReconcile(parsedStatementBalance, selectedEntryIds.toList(), createAdjustment)
                },
                enabled = isValid
            ) {
                Text(text = stringResource(R.string.reconcile_confirm_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}
