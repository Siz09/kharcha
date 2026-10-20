package com.sijan.kharcha.ui.utility

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.UtilityAccount
import com.sijan.kharcha.domain.model.UtilityType
import com.sijan.kharcha.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityVaultScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UtilityVaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<UtilityAccount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.utility_vault_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    accountToEdit = null
                    showDialog = true
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.utility_add_account))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Type Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { viewModel.selectType(null) },
                    label = { Text(text = stringResource(R.string.utility_type_all)) }
                )
                FilterChip(
                    selected = selectedType == UtilityType.NEA,
                    onClick = { viewModel.selectType(UtilityType.NEA) },
                    label = { Text(text = stringResource(R.string.utility_type_nea)) }
                )
                FilterChip(
                    selected = selectedType == UtilityType.KHANEPANI,
                    onClick = { viewModel.selectType(UtilityType.KHANEPANI) },
                    label = { Text(text = stringResource(R.string.utility_type_khanepani)) }
                )
                FilterChip(
                    selected = selectedType == UtilityType.ISP,
                    onClick = { viewModel.selectType(UtilityType.ISP) },
                    label = { Text(text = stringResource(R.string.utility_type_isp)) }
                )
                FilterChip(
                    selected = selectedType == UtilityType.OTHER,
                    onClick = { viewModel.selectType(UtilityType.OTHER) },
                    label = { Text(text = stringResource(R.string.utility_type_other)) }
                )
            }

            if (accounts.isEmpty()) {
                EmptyState(
                    message = stringResource(R.string.utility_empty_title),
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { account ->
                        UtilityAccountCard(
                            account = account,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Consumer ID", account.consumerId))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, context.getString(R.string.utility_copied_toast), Toast.LENGTH_SHORT).show()
                            },
                            onEdit = {
                                accountToEdit = account
                                showDialog = true
                            },
                            onDelete = {
                                viewModel.deleteAccount(account.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
        UtilityAccountDialog(
            account = accountToEdit,
            onDismiss = { showDialog = false },
            onSave = { updatedAccount ->
                viewModel.saveAccount(updatedAccount) {
                    showDialog = false
                }
            }
        )
    }
}

@Composable
fun UtilityAccountCard(
    account: UtilityAccount,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = getUtilityIcon(account.utilityType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = account.providerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Consumer ID Monospace Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { onCopy() }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.utility_consumer_id),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = account.consumerId,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (!account.counterOrBranch.isNullOrBlank() || !account.accountHolderName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!account.counterOrBranch.isNullOrBlank()) {
                        Text(
                            text = "${stringResource(R.string.utility_counter)}: ${account.counterOrBranch}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!account.accountHolderName.isNullOrBlank()) {
                        Text(
                            text = account.accountHolderName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getUtilityIcon(type: UtilityType): ImageVector {
    return when (type) {
        UtilityType.NEA -> Icons.Default.Receipt
        UtilityType.KHANEPANI -> Icons.Default.Info
        UtilityType.ISP -> Icons.Default.Phone
        UtilityType.OTHER -> Icons.Default.Receipt
    }
}

@Composable
fun UtilityAccountDialog(
    account: UtilityAccount?,
    onDismiss: () -> Unit,
    onSave: (UtilityAccount) -> Unit
) {
    var selectedType by remember { mutableStateOf(account?.utilityType ?: UtilityType.NEA) }
    var providerName by remember { mutableStateOf(account?.providerName ?: "") }
    var consumerId by remember { mutableStateOf(account?.consumerId ?: "") }
    var counterOrBranch by remember { mutableStateOf(account?.counterOrBranch ?: "") }
    var holderName by remember { mutableStateOf(account?.accountHolderName ?: "") }
    var notes by remember { mutableStateOf(account?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(if (account == null) R.string.utility_add_account else R.string.utility_edit_account),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Utility type selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UtilityType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(text = type.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = providerName,
                    onValueChange = { providerName = it },
                    label = { Text(text = stringResource(R.string.utility_provider_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = consumerId,
                    onValueChange = { consumerId = it },
                    label = { Text(text = stringResource(R.string.utility_consumer_id)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = counterOrBranch,
                    onValueChange = { counterOrBranch = it },
                    label = { Text(text = stringResource(R.string.utility_counter)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text(text = stringResource(R.string.utility_holder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (providerName.isNotBlank() && consumerId.isNotBlank()) {
                        val newAccount = UtilityAccount(
                            id = account?.id ?: 0L,
                            utilityType = selectedType,
                            providerName = providerName.trim(),
                            consumerId = consumerId.trim(),
                            counterOrBranch = counterOrBranch.trim().ifEmpty { null },
                            accountHolderName = holderName.trim().ifEmpty { null },
                            notes = notes.trim().ifEmpty { null },
                            createdAt = account?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(newAccount)
                    }
                },
                enabled = providerName.isNotBlank() && consumerId.isNotBlank()
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
