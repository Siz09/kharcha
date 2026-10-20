package com.sijan.kharcha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.model.SplitItem
import com.sijan.kharcha.domain.parser.Normalizer

data class SplitLineState(
    val categoryId: Long,
    val amountInput: String,
    val note: String
)

@Composable
fun SplitTransactionDialog(
    totalAmountPaisa: Long,
    categories: List<Category>,
    initialSplits: List<SplitItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmSplits: (List<SplitItem>) -> Unit
) {
    var splitLines by remember {
        mutableStateOf(
            if (initialSplits.isNotEmpty()) {
                initialSplits.map {
                    SplitLineState(
                        categoryId = it.categoryId,
                        amountInput = (it.amountPaisa / 100).toString(),
                        note = it.note ?: ""
                    )
                }
            } else {
                listOf(
                    SplitLineState(
                        categoryId = categories.firstOrNull()?.id ?: 1L,
                        amountInput = "",
                        note = ""
                    )
                )
            }
        )
    }

    val allocatedPaisa = splitLines.sumOf {
        Normalizer.parsePaisa(it.amountInput) ?: 0L
    }
    val remainingPaisa = totalAmountPaisa - allocatedPaisa
    val isValid = remainingPaisa == 0L && splitLines.all { (Normalizer.parsePaisa(it.amountInput) ?: 0L) > 0L }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.split_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.split_total, Money.formatPaisa(totalAmountPaisa)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.split_allocated, Money.formatPaisa(allocatedPaisa)),
                                style = MaterialTheme.typography.bodySmall
                            )
                            val remainingColor = if (remainingPaisa == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            Text(
                                text = stringResource(R.string.split_remaining, Money.formatPaisa(remainingPaisa)),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = remainingColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(splitLines) { index, line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = line.amountInput,
                                onValueChange = { newVal ->
                                    val updated = splitLines.toMutableList()
                                    updated[index] = line.copy(amountInput = newVal)
                                    splitLines = updated
                                },
                                label = { Text(stringResource(R.string.split_amount)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = line.note,
                                onValueChange = { newNote ->
                                    val updated = splitLines.toMutableList()
                                    updated[index] = line.copy(note = newNote)
                                    splitLines = updated
                                },
                                label = { Text(stringResource(R.string.field_note)) },
                                modifier = Modifier.weight(1.2f)
                            )
                            if (splitLines.size > 1) {
                                IconButton(onClick = {
                                    val updated = splitLines.toMutableList()
                                    updated.removeAt(index)
                                    splitLines = updated
                                }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val firstCatId = categories.firstOrNull()?.id ?: 1L
                        splitLines = splitLines + SplitLineState(firstCatId, "", "")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.split_add_line))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val splits = splitLines.mapNotNull { line ->
                        val amount = Normalizer.parsePaisa(line.amountInput) ?: return@mapNotNull null
                        SplitItem(
                            categoryId = line.categoryId,
                            amountPaisa = amount,
                            note = line.note.ifBlank { null }
                        )
                    }
                    onConfirmSplits(splits)
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
