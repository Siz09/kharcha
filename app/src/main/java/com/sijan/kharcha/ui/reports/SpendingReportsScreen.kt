package com.sijan.kharcha.ui.reports

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.calendar.BikramSambatConverter
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
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val summary by viewModel.analyticsSummary.collectAsStateWithLifecycle()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()

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
            // Month Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.onPreviousMonth() }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.onNextMonth() }) {
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
                    onClick = { viewModel.exportPdf(context) },
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
    }
}
