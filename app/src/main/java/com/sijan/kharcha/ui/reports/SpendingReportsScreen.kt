package com.sijan.kharcha.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.calendar.BikramSambatConverter
import com.sijan.kharcha.domain.model.AnnualAnalyticsSummary
import com.sijan.kharcha.domain.model.CalendarSystem
import com.sijan.kharcha.domain.model.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingReportsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpendingReportsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedReportTab.collectAsStateWithLifecycle()

    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val summary by viewModel.analyticsSummary.collectAsStateWithLifecycle()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()

    val annualYear by viewModel.selectedAnnualYear.collectAsStateWithLifecycle()
    val calendarSystem by viewModel.selectedCalendarSystem.collectAsStateWithLifecycle()
    val annualSummary by viewModel.annualAnalyticsSummary.collectAsStateWithLifecycle()
    val isArchiving by viewModel.isArchiving.collectAsStateWithLifecycle()

    val monthDate = LocalDate.of(year, month, 1)
    val bsDate = BikramSambatConverter.fromLocalDate(monthDate)
    val monthLabel = "${monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))} (BS ${bsDate.monthName} ${bsDate.year})"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tabs: Monthly vs Annual
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.onSelectTab(0) },
                        text = { Text(stringResource(R.string.reports_tab_monthly)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.onSelectTab(1) },
                        text = { Text(stringResource(R.string.reports_tab_annual)) }
                    )
                }
            }

            if (selectedTab == 0) {
                // Monthly Report Content
                monthlyReportContent(
                    monthLabel = monthLabel,
                    summary = summary,
                    isGeneratingPdf = isGeneratingPdf,
                    onPreviousMonth = { viewModel.onPreviousMonth() },
                    onNextMonth = { viewModel.onNextMonth() },
                    onExportPdf = { viewModel.exportPdf(context) }
                )
            } else {
                // Annual Analytics Content
                annualAnalyticsContent(
                    annualYear = annualYear,
                    calendarSystem = calendarSystem,
                    annualSummary = annualSummary,
                    isArchiving = isArchiving,
                    onPreviousYear = { viewModel.onPreviousAnnualYear() },
                    onNextYear = { viewModel.onNextAnnualYear() },
                    onSetCalendar = { viewModel.onSetCalendarSystem(it) },
                    onArchiveYear = { viewModel.archiveCurrentYear() }
                )
            }
        }
    }
}

private fun LazyListScope.monthlyReportContent(
    monthLabel: String,
    summary: com.sijan.kharcha.domain.model.SpendingAnalyticsSummary?,
    isGeneratingPdf: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onExportPdf: () -> Unit
) {
    // Month Selector
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }
    }

    // MoM Comparison Card
    item {
        summary?.momComparison?.let { mom ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.reports_mom_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val isIncrease = mom.netDifferencePaisa > 0L
                    val deltaColor = if (isIncrease) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    val deltaText = if (isIncrease) {
                        stringResource(R.string.reports_spending_increase, Money.formatPaisa(mom.netDifferencePaisa), mom.percentageChange)
                    } else if (mom.netDifferencePaisa < 0L) {
                        stringResource(R.string.reports_spending_decrease, Money.formatPaisa(-mom.netDifferencePaisa), -mom.percentageChange)
                    } else {
                        stringResource(R.string.reports_spending_equal)
                    }

                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = deltaColor
                    )
                }
            }
        }
    }

    // Day of Week Canvas Bar Chart
    item {
        summary?.let { s ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.reports_day_of_week_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxSpend = (s.dayOfWeekSpends.maxOfOrNull { it.totalExpensePaisa } ?: 1L).coerceAtLeast(1L)
                    val barColor = MaterialTheme.colorScheme.primary

                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val count = s.dayOfWeekSpends.size.coerceAtLeast(1)
                            val barWidth = size.width / count.toFloat()
                            val maxHeight = size.height - 20f

                            s.dayOfWeekSpends.forEachIndexed { index, daySpend ->
                                val heightRatio = (daySpend.totalExpensePaisa.toFloat() / maxSpend.toFloat()).coerceIn(0f, 1f)
                                val barHeight = (heightRatio * maxHeight).coerceAtLeast(6f)
                                val left = index * barWidth + (barWidth * 0.2f)
                                val top = maxHeight - barHeight

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth * 0.6f, barHeight),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                    }

                    // Day labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        labels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Top Merchants / Notes
    item {
        summary?.let { s ->
            if (s.topMerchants.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.reports_top_merchants_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        s.topMerchants.forEach { (merchant, amountPaisa) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = merchant, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = Money.formatPaisa(amountPaisa),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Export PDF Statement Button
    item {
        Button(
            onClick = onExportPdf,
            enabled = !isGeneratingPdf,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isGeneratingPdf) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.reports_generating_pdf))
            } else {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.reports_export_pdf_button), fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun LazyListScope.annualAnalyticsContent(
    annualYear: Int,
    calendarSystem: CalendarSystem,
    annualSummary: AnnualAnalyticsSummary?,
    isArchiving: Boolean,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onSetCalendar: (CalendarSystem) -> Unit,
    onArchiveYear: () -> Unit
) {
    // Calendar System selector & Year selector
    item {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = calendarSystem == CalendarSystem.BS,
                    onClick = { onSetCalendar(CalendarSystem.BS) },
                    label = { Text(stringResource(R.string.reports_calendar_bs)) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = calendarSystem == CalendarSystem.AD,
                    onClick = { onSetCalendar(CalendarSystem.AD) },
                    label = { Text(stringResource(R.string.reports_calendar_ad)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousYear) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                }
                val yearLabel = if (calendarSystem == CalendarSystem.BS) "$annualYear BS" else "$annualYear AD"
                Text(
                    text = yearLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNextYear) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Year")
                }
            }
        }
    }

    // Annual Overview Card
    item {
        annualSummary?.let { s ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reports_annual_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (s.isArchived) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.reports_archived_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = stringResource(R.string.reports_total_income), style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = Money.formatPaisa(s.totalIncomePaisa),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = stringResource(R.string.reports_total_expense), style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = Money.formatPaisa(s.totalExpensePaisa),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = stringResource(R.string.reports_net_savings), style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = Money.formatPaisa(s.netSavingsPaisa),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.reports_savings_rate, s.savingsRatePercentage),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.reports_daily_burn, Money.formatPaisa(s.dailyBurnRatePaisa)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    s.yearOverYearExpenseChangePercentage?.let { yoy ->
                        Spacer(modifier = Modifier.height(12.dp))
                        val isYoyIncrease = yoy > 0
                        val yoyColor = if (isYoyIncrease) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        val sign = if (isYoyIncrease) "+" else ""
                        Text(
                            text = stringResource(R.string.reports_yoy_change, sign, yoy),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = yoyColor
                        )
                    }
                }
            }
        }
    }

    // 12-Month Flow Chart
    item {
        annualSummary?.let { s ->
            if (s.monthlyTotals.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.reports_monthly_trend),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val maxPoint = s.monthlyTotals.maxOfOrNull { maxOf(it.expensePaisa, it.incomePaisa) } ?: 1L
                        val safeMax = maxPoint.coerceAtLeast(1L)
                        val incomeColor = MaterialTheme.colorScheme.primary
                        val expenseColor = MaterialTheme.colorScheme.error

                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val monthCount = s.monthlyTotals.size.coerceAtLeast(1)
                                val monthWidth = size.width / monthCount.toFloat()
                                val barWidth = (monthWidth * 0.35f).coerceAtLeast(3f)
                                val maxHeight = size.height - 20f

                                s.monthlyTotals.forEachIndexed { i, m ->
                                    val incHeight = ((m.incomePaisa.toFloat() / safeMax.toFloat()) * maxHeight).coerceIn(0f, maxHeight)
                                    val expHeight = ((m.expensePaisa.toFloat() / safeMax.toFloat()) * maxHeight).coerceIn(0f, maxHeight)

                                    val centerX = i * monthWidth + (monthWidth / 2f)

                                    // Draw income bar
                                    if (incHeight > 0f) {
                                        drawRoundRect(
                                            color = incomeColor,
                                            topLeft = Offset(centerX - barWidth - 1f, maxHeight - incHeight),
                                            size = Size(barWidth, incHeight),
                                            cornerRadius = CornerRadius(4f, 4f)
                                        )
                                    }

                                    // Draw expense bar
                                    if (expHeight > 0f) {
                                        drawRoundRect(
                                            color = expenseColor,
                                            topLeft = Offset(centerX + 1f, maxHeight - expHeight),
                                            size = Size(barWidth, expHeight),
                                            cornerRadius = CornerRadius(4f, 4f)
                                        )
                                    }
                                }
                            }
                        }

                        // Month labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            s.monthlyTotals.forEach { m ->
                                Text(
                                    text = m.monthName.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Category Breakdown Progress Bars
    item {
        annualSummary?.let { s ->
            if (s.categoryBreakdown.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.reports_category_breakdown),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        s.categoryBreakdown.forEach { cat ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = cat.categoryName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${Money.formatPaisa(cat.totalExpensePaisa)} (${cat.percentageOfTotal}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (cat.percentageOfTotal.toFloat() / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Archive Button if not already archived
    item {
        annualSummary?.let { s ->
            if (!s.isArchived) {
                OutlinedButton(
                    onClick = onArchiveYear,
                    enabled = !isArchiving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isArchiving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = stringResource(R.string.reports_archive_button))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
