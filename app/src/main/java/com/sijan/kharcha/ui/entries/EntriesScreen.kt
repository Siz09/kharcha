package com.sijan.kharcha.ui.entries

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.ui.components.EmptyState
import com.sijan.kharcha.ui.components.EntryRow
import com.sijan.kharcha.ui.theme.KharchaTheme

@Composable
fun EntriesScreen(
    modifier: Modifier = Modifier,
    viewModel: EntriesViewModel = hiltViewModel(),
    onEntryClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntriesScreenContent(
        uiState = uiState,
        modifier = modifier,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onTypeFilterChange = viewModel::onTypeFilterChange,
        onEntryClick = onEntryClick
    )
}

@Composable
fun EntriesScreenContent(
    uiState: EntriesUiState,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    onTypeFilterChange: (EntryType?) -> Unit = {},
    onEntryClick: (Long) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(text = stringResource(R.string.entries_search_hint))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.cd_search)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedType == null,
                onClick = { onTypeFilterChange(null) },
                label = { Text(text = stringResource(R.string.entries_filter_all)) }
            )

            FilterChip(
                selected = uiState.selectedType == EntryType.EXPENSE,
                onClick = {
                    onTypeFilterChange(
                        if (uiState.selectedType == EntryType.EXPENSE) null else EntryType.EXPENSE
                    )
                },
                label = { Text(text = stringResource(R.string.entries_filter_expense)) }
            )

            FilterChip(
                selected = uiState.selectedType == EntryType.INCOME,
                onClick = {
                    onTypeFilterChange(
                        if (uiState.selectedType == EntryType.INCOME) null else EntryType.INCOME
                    )
                },
                label = { Text(text = stringResource(R.string.entries_filter_income)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.entries.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.entries_no_results),
                modifier = Modifier.padding(vertical = 48.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.entries,
                    key = { it.id }
                ) { entry ->
                    val categoryName = uiState.categoriesMap[entry.categoryId]?.name ?: ""
                    EntryRow(
                        entry = entry,
                        categoryName = categoryName,
                        onClick = { onEntryClick(entry.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Preview(name = "Light Mode")
@Composable
private fun EntriesScreenPreview() {
    KharchaTheme {
        Surface {
            EntriesScreenContent(
                uiState = EntriesUiState(
                    entries = listOf(
                        LedgerEntry(
                            id = 1L,
                            amountPaisa = 120000L,
                            type = EntryType.EXPENSE,
                            categoryId = 1L,
                            note = "Grocery shopping",
                            date = System.currentTimeMillis()
                        ),
                        LedgerEntry(
                            id = 2L,
                            amountPaisa = 5000000L,
                            type = EntryType.INCOME,
                            categoryId = 2L,
                            note = "Salary payment",
                            date = System.currentTimeMillis()
                        )
                    ),
                    categoriesMap = mapOf(
                        1L to Category(id = 1L, name = "Food & Dining", type = EntryType.EXPENSE),
                        2L to Category(id = 2L, name = "Salary", type = EntryType.INCOME)
                    ),
                    isLoading = false
                )
            )
        }
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EntriesScreenDarkPreview() {
    KharchaTheme {
        Surface {
            EntriesScreenContent(
                uiState = EntriesUiState(
                    entries = emptyList(),
                    isLoading = false
                )
            )
        }
    }
}
