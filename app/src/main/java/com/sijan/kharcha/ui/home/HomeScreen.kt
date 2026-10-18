package com.sijan.kharcha.ui.home

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.MonthlySummary
import com.sijan.kharcha.ui.components.AmountText
import com.sijan.kharcha.ui.components.EmptyState
import com.sijan.kharcha.ui.components.EntryRow
import com.sijan.kharcha.ui.theme.KharchaTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToEntries: () -> Unit = {},
    onEntryClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        modifier = modifier,
        onNavigateToEntries = onNavigateToEntries,
        onEntryClick = onEntryClick
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onNavigateToEntries: () -> Unit = {},
    onEntryClick: (Long) -> Unit = {}
) {
    if (uiState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            MonthlySummaryCard(summary = uiState.monthlySummary)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_recent_entries),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.recentEntries.isNotEmpty()) {
                    TextButton(onClick = onNavigateToEntries) {
                        Text(text = stringResource(R.string.home_view_all))
                    }
                }
            }
        }

        if (uiState.recentEntries.isEmpty()) {
            item {
                EmptyState(
                    message = stringResource(R.string.home_empty_entries),
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        } else {
            items(
                items = uiState.recentEntries,
                key = { it.id }
            ) { entry ->
                val categoryName = uiState.categoriesMap[entry.categoryId]?.name ?: ""
                EntryRow(
                    entry = entry,
                    categoryName = categoryName,
                    onClick = { onEntryClick(entry.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MonthlySummaryCard(
    summary: MonthlySummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.home_net_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            AmountText(
                amountPaisa = summary.balancePaisa,
                type = if (summary.balancePaisa >= 0) EntryType.INCOME else EntryType.EXPENSE,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_total_income),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AmountText(
                        amountPaisa = summary.totalIncomePaisa,
                        type = EntryType.INCOME,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.home_total_expense),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AmountText(
                        amountPaisa = summary.totalExpensePaisa,
                        type = EntryType.EXPENSE,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode")
@Composable
private fun HomeScreenPreview() {
    KharchaTheme {
        Surface {
            HomeScreenContent(
                uiState = HomeUiState(
                    monthlySummary = MonthlySummary(
                        totalIncomePaisa = 12000000L,
                        totalExpensePaisa = 4500000L,
                        balancePaisa = 7500000L
                    ),
                    recentEntries = listOf(
                        LedgerEntry(
                            id = 1L,
                            amountPaisa = 150000L,
                            type = EntryType.EXPENSE,
                            categoryId = 1L,
                            note = "Grocery shopping",
                            date = System.currentTimeMillis()
                        ),
                        LedgerEntry(
                            id = 2L,
                            amountPaisa = 8000000L,
                            type = EntryType.INCOME,
                            categoryId = 2L,
                            note = "Monthly salary",
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
private fun HomeScreenDarkPreview() {
    KharchaTheme {
        Surface {
            HomeScreenContent(
                uiState = HomeUiState(
                    monthlySummary = MonthlySummary(
                        totalIncomePaisa = 12000000L,
                        totalExpensePaisa = 4500000L,
                        balancePaisa = 7500000L
                    ),
                    recentEntries = emptyList(),
                    isLoading = false
                )
            )
        }
    }
}
