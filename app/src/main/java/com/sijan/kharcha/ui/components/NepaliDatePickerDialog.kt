package com.sijan.kharcha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.calendar.BikramSambatConverter
import com.sijan.kharcha.domain.calendar.NepaliDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NepaliDatePickerDialog(
    initialEpochMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val initialNepaliDate = remember(initialEpochMillis) {
        BikramSambatConverter.fromEpochMillis(initialEpochMillis)
    }

    var selectedYear by remember { mutableIntStateOf(initialNepaliDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialNepaliDate.month) }
    var selectedDay by remember { mutableIntStateOf(initialNepaliDate.day) }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        BikramSambatConverter.getDaysInMonth(selectedYear, selectedMonth)
    }

    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

    val currentNepaliDate = NepaliDate(selectedYear, selectedMonth, selectedDay)
    val adDateEquivalent = remember(selectedYear, selectedMonth, selectedDay) {
        BikramSambatConverter.toLocalDate(currentNepaliDate)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.nepali_date_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${currentNepaliDate.format()} / $adDateEquivalent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Year Navigator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedYear > BikramSambatConverter.START_YEAR_BS) selectedYear--
                        },
                        enabled = selectedYear > BikramSambatConverter.START_YEAR_BS
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_calendar_previous_month)
                        )
                    }

                    Text(
                        text = "$selectedYear BS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            if (selectedYear < BikramSambatConverter.END_YEAR_BS) selectedYear++
                        },
                        enabled = selectedYear < BikramSambatConverter.END_YEAR_BS
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.cd_calendar_next_month)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month Selector
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NepaliDate.MONTH_NAMES_EN.forEachIndexed { index, name ->
                        val monthNum = index + 1
                        FilterChip(
                            selected = selectedMonth == monthNum,
                            onClick = { selectedMonth = monthNum },
                            label = { Text(text = name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days Grid (1 to daysInMonth)
                val dayChunks = (1..daysInMonth).chunked(7)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayChunks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { dayNum ->
                                val isSelected = dayNum == selectedDay
                                val monthName = NepaliDate.MONTH_NAMES_EN.getOrElse(selectedMonth - 1) { "" }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .semantics {
                                            contentDescription = "$dayNum $monthName $selectedYear"
                                            role = Role.Button
                                            selected = isSelected
                                        }
                                        .clickable { selectedDay = dayNum },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            val remaining = 7 - week.size
                            repeat(remaining) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val epochMillis = BikramSambatConverter.toEpochMillis(currentNepaliDate)
                    onDateSelected(epochMillis)
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}
