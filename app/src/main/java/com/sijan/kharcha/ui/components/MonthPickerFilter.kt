package com.sijan.kharcha.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sijan.kharcha.R
import java.util.Calendar
import java.util.TimeZone

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

@Composable
fun MonthPickerFilterCard(
    selectedMonthLabel: String,
    isCurrentMonth: Boolean,
    onSelectMonth: (year: Int, month: Int) -> Unit,
    onResetCurrentMonth: () -> Unit,
    currentYear: Int,
    currentMonth: Int,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = stringResource(R.string.cd_select_month),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = selectedMonthLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrentMonth) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                ) {
                    Text(
                        text = if (isCurrentMonth) {
                            stringResource(R.string.home_current_month_badge)
                        } else {
                            stringResource(R.string.home_viewing_month_badge, selectedMonthLabel)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentMonth) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    if (showDialog) {
        MonthPickerDialog(
            initialYear = currentYear,
            initialMonth = currentMonth,
            onDismiss = { showDialog = false },
            onConfirm = { year, month ->
                onSelectMonth(year, month)
                showDialog = false
            },
            onResetToCurrentMonth = {
                onResetCurrentMonth()
                showDialog = false
            }
        )
    }
}

@Composable
fun MonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
    onResetToCurrentMonth: () -> Unit
) {
    val realNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val realYear = realNow.get(Calendar.YEAR)
    val realMonth = realNow.get(Calendar.MONTH) + 1

    var pickerYear by remember { mutableIntStateOf(if (initialYear > 0) initialYear else realYear) }
    var pickerMonth by remember { mutableIntStateOf(if (initialMonth in 1..12) initialMonth else realMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.home_select_month_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Year Navigator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pickerYear -= 1 }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_previous_year)
                        )
                    }

                    Text(
                        text = pickerYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { pickerYear += 1 }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.cd_next_year)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 12 Months Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(MONTH_NAMES) { index, monthName ->
                        val monthNum = index + 1
                        val isSelected = (monthNum == pickerMonth)
                        val isRealCurrent = (pickerYear == realYear && monthNum == realMonth)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                pickerMonth = monthNum
                                onConfirm(pickerYear, monthNum)
                            },
                            label = {
                                Text(
                                    text = if (isRealCurrent) "$monthName •" else monthName,
                                    fontWeight = if (isSelected || isRealCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerYear, pickerMonth) }) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onResetToCurrentMonth) {
                    Text(text = stringResource(R.string.home_reset_to_current_month))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
