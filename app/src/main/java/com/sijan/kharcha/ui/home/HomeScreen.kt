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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.AccountWithBalance
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.ui.components.DateFormatter
import com.sijan.kharcha.domain.model.FinancialHealthStatus
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.model.MonthlySummary
import com.sijan.kharcha.ui.components.AmountText
import com.sijan.kharcha.ui.components.EmptyState
import com.sijan.kharcha.ui.components.EntryRow
import com.sijan.kharcha.ui.components.MonthPickerFilterCard
import com.sijan.kharcha.ui.theme.KharchaTheme
import com.sijan.kharcha.ui.theme.KharchaThemeDefaults

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToEntries: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onEntryClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        modifier = modifier,
        onNavigateToEntries = onNavigateToEntries,
        onNavigateToCalendar = onNavigateToCalendar,
        onEntryClick = onEntryClick,
        onSelectMonth = viewModel::selectMonth,
        onResetCurrentMonth = viewModel::resetToCurrentMonth
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onNavigateToEntries: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onEntryClick: (Long) -> Unit = {},
    onSelectMonth: (year: Int, month: Int) -> Unit = { _, _ -> },
    onResetCurrentMonth: () -> Unit = {}
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
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = DateFormatter.formatDualDate(System.currentTimeMillis()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(R.string.calendar_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                MonthPickerFilterCard(
                    selectedMonthLabel = uiState.selectedMonthLabel,
                    isCurrentMonth = uiState.isCurrentMonth,
                    onSelectMonth = onSelectMonth,
                    onResetCurrentMonth = onResetCurrentMonth,
                    currentYear = uiState.selectedYear,
                    currentMonth = uiState.selectedMonth
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MonthlySummaryCard(
                summary = uiState.monthlySummary,
                openingBalancePaisa = uiState.openingBalancePaisa,
                rolloverEnabled = uiState.rolloverEnabled,
                dailyBurnRatePaisa = uiState.dailyBurnRatePaisa
            )
            if (uiState.accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AccountsSummaryCard(
                    accounts = uiState.accounts,
                    totalNetWorthPaisa = uiState.totalNetWorthPaisa
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MonthlySavingIndexCard(
                financialHealth = uiState.financialHealth,
                savingsRatePercent = uiState.savingsRatePercent
            )
            Spacer(modifier = Modifier.height(12.dp))
            ExpenseBreakdownCard(
                categoryExpenses = uiState.categoryExpenses,
                totalExpensePaisa = uiState.monthlySummary.totalExpensePaisa
            )
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
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
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
private fun rememberPieChartColors(): List<Color> {
    val isDark = isSystemInDarkTheme()
    return remember(isDark) {
        if (isDark) {
            listOf(
                Color(0xFF4DB6AC), // Teal 300
                Color(0xFFFFD54F), // Amber 300
                Color(0xFF81C784), // Light Green 300
                Color(0xFFE57373), // Red 300
                Color(0xFF7986CB), // Indigo 300
                Color(0xFFBA68C8), // Purple 300
                Color(0xFF90A4AE)  // Blue Grey 300
            )
        } else {
            listOf(
                Color(0xFF176B52), // Forest Green
                Color(0xFFE5A020), // Accent Amber
                Color(0xFF00796B), // Deep Teal
                Color(0xFFD32F2F), // Red
                Color(0xFF3949AB), // Indigo
                Color(0xFF7B1FA2), // Purple
                Color(0xFF546E7A)  // Blue Grey
            )
        }
    }
}

@Composable
fun MonthlySavingIndexCard(
    financialHealth: FinancialHealthStatus,
    savingsRatePercent: Int?,
    modifier: Modifier = Modifier
) {
    val colors = KharchaThemeDefaults.colors
    val statusBg: Color
    val statusTextColor: Color
    val statusTextRes: Int
    val descRes: Int

    when (financialHealth) {
        FinancialHealthStatus.HEALTHY -> {
            statusBg = colors.incomeContainer
            statusTextColor = colors.income
            statusTextRes = R.string.financial_health_healthy
            descRes = R.string.monthly_saving_index_healthy_desc
        }
        FinancialHealthStatus.MODERATE -> {
            statusBg = MaterialTheme.colorScheme.secondaryContainer
            statusTextColor = MaterialTheme.colorScheme.onSecondaryContainer
            statusTextRes = R.string.financial_health_moderate
            descRes = R.string.monthly_saving_index_moderate_desc
        }
        FinancialHealthStatus.SLIM -> {
            statusBg = colors.warningContainer
            statusTextColor = colors.warning
            statusTextRes = R.string.financial_health_slim
            descRes = R.string.monthly_saving_index_slim_desc
        }
        FinancialHealthStatus.DEFICIT -> {
            statusBg = colors.errorContainer
            statusTextColor = colors.error
            statusTextRes = R.string.financial_health_deficit
            descRes = R.string.monthly_saving_index_deficit_desc
        }
    }

    val statusBadgeText = if (savingsRatePercent != null) {
        stringResource(
            R.string.financial_health_status_format,
            stringResource(statusTextRes),
            savingsRatePercent
        )
    } else {
        stringResource(
            R.string.financial_health_status_format_no_rate,
            stringResource(statusTextRes)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = stringResource(R.string.monthly_saving_index_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusBg,
                    modifier = Modifier.semantics {
                        contentDescription = statusBadgeText
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = statusTextColor,
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Text(
                            text = statusBadgeText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            SavingsIndexMeter(
                financialHealth = financialHealth
            )
        }
    }
}

@Composable
private fun SavingsIndexMeter(
    financialHealth: FinancialHealthStatus,
    modifier: Modifier = Modifier
) {
    val colors = KharchaThemeDefaults.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(MaterialTheme.shapes.small),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (financialHealth == FinancialHealthStatus.DEFICIT) colors.error
                        else colors.error.copy(alpha = 0.25f)
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (financialHealth == FinancialHealthStatus.SLIM) colors.warning
                        else colors.warning.copy(alpha = 0.25f)
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .background(
                        if (financialHealth == FinancialHealthStatus.MODERATE) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
            )
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(
                        if (financialHealth == FinancialHealthStatus.HEALTHY) colors.income
                        else colors.income.copy(alpha = 0.25f)
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.monthly_saving_index_meter_deficit),
                style = MaterialTheme.typography.labelSmall,
                color = if (financialHealth == FinancialHealthStatus.DEFICIT) colors.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.monthly_saving_index_meter_slim),
                style = MaterialTheme.typography.labelSmall,
                color = if (financialHealth == FinancialHealthStatus.SLIM) colors.warning else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.monthly_saving_index_meter_moderate),
                style = MaterialTheme.typography.labelSmall,
                color = if (financialHealth == FinancialHealthStatus.MODERATE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.monthly_saving_index_meter_healthy),
                style = MaterialTheme.typography.labelSmall,
                color = if (financialHealth == FinancialHealthStatus.HEALTHY) colors.income else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExpenseBreakdownCard(
    categoryExpenses: List<HomeCategoryExpense>,
    totalExpensePaisa: Long,
    modifier: Modifier = Modifier
) {
    val pieChartColors = rememberPieChartColors()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = stringResource(R.string.home_expense_breakdown_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )

                if (categoryExpenses.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.home_expense_breakdown_total_format,
                            Money.formatPaisa(totalExpensePaisa)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_expense_breakdown_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(110.dp)) {
                            var startAngle = -90f
                            val strokeWidth = 18.dp.toPx()
                            categoryExpenses.forEachIndexed { index, item ->
                                val sweepAngle = (item.percentage / 100f) * 360f
                                val color = pieChartColors[index % pieChartColors.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryExpenses.take(4).forEachIndexed { index, item ->
                            val color = pieChartColors[index % pieChartColors.size]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = color,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Text(
                                        text = item.categoryName,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", item.percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlySummaryCard(
    summary: MonthlySummary,
    openingBalancePaisa: Long = 0L,
    rolloverEnabled: Boolean = false,
    dailyBurnRatePaisa: Long = 0L,
    modifier: Modifier = Modifier
) {
    val totalAvailablePaisa = if (rolloverEnabled) {
        summary.balancePaisa + openingBalancePaisa
    } else {
        summary.balancePaisa
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (rolloverEnabled) {
                        stringResource(R.string.home_net_balance_with_rollover)
                    } else {
                        stringResource(R.string.home_net_balance)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (dailyBurnRatePaisa > 0L) {
                    Text(
                        text = stringResource(
                            R.string.financial_health_burn_format,
                            Money.formatPaisa(dailyBurnRatePaisa)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AmountText(
                amountPaisa = totalAvailablePaisa,
                type = if (totalAvailablePaisa >= 0) EntryType.INCOME else EntryType.EXPENSE,
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

                if (rolloverEnabled) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.home_opening_balance),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        AmountText(
                            amountPaisa = openingBalancePaisa,
                            type = null,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
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
                    categoryExpenses = listOf(
                        HomeCategoryExpense(1L, "Food & Dining", 150000L, 100f)
                    ),
                    savingsRatePercent = 62,
                    dailyBurnRatePaisa = 150000L,
                    financialHealth = FinancialHealthStatus.HEALTHY,
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
                    savingsRatePercent = 62,
                    dailyBurnRatePaisa = 150000L,
                    financialHealth = FinancialHealthStatus.HEALTHY,
                    isLoading = false
                )
            )
        }
    }
}

@Composable
fun AccountsSummaryCard(
    accounts: List<AccountWithBalance>,
    totalNetWorthPaisa: Long,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = stringResource(R.string.account_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "${stringResource(R.string.account_net_worth)}: ${Money.formatPaisa(totalNetWorthPaisa)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.filter { it.account.isActive }.forEach { item ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.account.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = Money.formatPaisa(item.currentBalancePaisa),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.currentBalancePaisa < 0) KharchaThemeDefaults.colors.expense else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
