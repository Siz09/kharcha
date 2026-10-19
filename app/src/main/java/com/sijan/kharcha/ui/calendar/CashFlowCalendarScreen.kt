package com.sijan.kharcha.ui.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.ui.components.AmountText
import com.sijan.kharcha.ui.components.EmptyState
import com.sijan.kharcha.ui.components.EntryRow
import com.sijan.kharcha.ui.theme.KharchaTheme
import com.sijan.kharcha.ui.theme.KharchaThemeDefaults
import java.text.DateFormatSymbols
import java.util.Locale

@Composable
fun CashFlowCalendarScreen(
    onNavigateBack: () -> Unit,
    onEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CashFlowCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CashFlowCalendarScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEntryClick = onEntryClick,
        onSelectDay = viewModel::selectDay,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowCalendarScreenContent(
    uiState: CashFlowCalendarUiState,
    onNavigateBack: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onSelectDay: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.calendar_title),
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
            // Month Selector Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.cd_calendar_previous_month)
                        )
                    }

                    Text(
                        text = uiState.selectedMonthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.cd_calendar_next_month)
                        )
                    }
                }
            }

            // Month Overview Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.home_total_income),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AmountText(
                                amountPaisa = uiState.totalMonthIncomePaisa,
                                type = EntryType.INCOME,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.home_total_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AmountText(
                                amountPaisa = uiState.totalMonthExpensePaisa,
                                type = EntryType.EXPENSE,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.home_net_balance),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AmountText(
                                amountPaisa = uiState.totalMonthBalancePaisa,
                                type = if (uiState.totalMonthBalancePaisa >= 0) EntryType.INCOME else EntryType.EXPENSE,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Calendar Grid Card
            item {
                CalendarGridCard(
                    daysInMonth = uiState.daysInMonth,
                    firstDayOffset = uiState.firstDayOfWeekOffset,
                    selectedDay = uiState.selectedDay,
                    dailyCashFlowMap = uiState.dailyCashFlowMap,
                    onSelectDay = onSelectDay
                )
            }

            // Selected Day Header
            item {
                val selectedFlow = uiState.selectedDayCashFlow
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_daily_flow),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        AmountText(
                            amountPaisa = selectedFlow.netPaisa,
                            type = if (selectedFlow.netPaisa >= 0) EntryType.INCOME else EntryType.EXPENSE,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Selected Day Entries
            val dayEntries = uiState.selectedDayCashFlow.entries
            if (dayEntries.isEmpty()) {
                item {
                    EmptyState(
                        message = stringResource(R.string.calendar_no_entries_for_day),
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(
                    items = dayEntries,
                    key = { it.id }
                ) { entry ->
                    val catName = uiState.categoriesMap[entry.categoryId]?.name ?: ""
                    EntryRow(
                        entry = entry,
                        categoryName = catName,
                        onClick = { onEntryClick(entry.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CalendarGridCard(
    daysInMonth: Int,
    firstDayOffset: Int,
    selectedDay: Int,
    dailyCashFlowMap: Map<Int, DayCashFlow>,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = remember {
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays
        listOf(symbols[1], symbols[2], symbols[3], symbols[4], symbols[5], symbols[6], symbols[7])
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Day of Week Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName.take(3),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Days Grid (Rows of 7)
            val totalCells = firstDayOffset + daysInMonth
            val totalRows = (totalCells + 6) / 7

            for (row in 0 until totalRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOffset + 1

                        if (dayNumber in 1..daysInMonth) {
                            val isSelected = (dayNumber == selectedDay)
                            val dayFlow = dailyCashFlowMap[dayNumber]

                            CalendarDayCell(
                                day = dayNumber,
                                isSelected = isSelected,
                                dayCashFlow = dayFlow,
                                onClick = { onSelectDay(dayNumber) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Blank cell outside month range
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (row < totalRows - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    dayCashFlow: DayCashFlow?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KharchaThemeDefaults.colors
    val dotColor = when {
        dayCashFlow == null || !dayCashFlow.hasEntries -> null
        dayCashFlow.netPaisa > 0 -> colors.income
        dayCashFlow.netPaisa < 0 -> colors.expense
        else -> MaterialTheme.colorScheme.outline
    }

    val selectedBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    val selectedBorderModifier = if (isSelected) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(selectedBg)
            .then(selectedBorderModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            } else {
                Spacer(modifier = Modifier.size(5.dp))
            }
        }
    }
}

@Preview(name = "Light Mode")
@Composable
private fun CashFlowCalendarPreview() {
    KharchaTheme {
        Surface {
            CashFlowCalendarScreenContent(
                uiState = CashFlowCalendarUiState(
                    selectedYear = 2026,
                    selectedMonth = 10,
                    selectedMonthLabel = "October 2026",
                    selectedDay = 19,
                    daysInMonth = 31,
                    firstDayOfWeekOffset = 4,
                    totalMonthIncomePaisa = 10000000L,
                    totalMonthExpensePaisa = 4500000L,
                    totalMonthBalancePaisa = 5500000L,
                    isLoading = false
                ),
                onNavigateBack = {},
                onEntryClick = {},
                onSelectDay = {},
                onPreviousMonth = {},
                onNextMonth = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CashFlowCalendarDarkPreview() {
    KharchaTheme {
        Surface {
            CashFlowCalendarScreenContent(
                uiState = CashFlowCalendarUiState(
                    selectedYear = 2026,
                    selectedMonth = 10,
                    selectedMonthLabel = "October 2026",
                    selectedDay = 19,
                    daysInMonth = 31,
                    firstDayOfWeekOffset = 4,
                    totalMonthIncomePaisa = 10000000L,
                    totalMonthExpensePaisa = 4500000L,
                    totalMonthBalancePaisa = 5500000L,
                    isLoading = false
                ),
                onNavigateBack = {},
                onEntryClick = {},
                onSelectDay = {},
                onPreviousMonth = {},
                onNextMonth = {}
            )
        }
    }
}
