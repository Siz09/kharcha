package com.sijan.kharcha.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.ui.theme.ExpenseRed
import com.sijan.kharcha.ui.theme.IncomeGreen
import com.sijan.kharcha.ui.theme.KharchaTheme

@Composable
fun EntryRow(
    entry: LedgerEntry,
    categoryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = when (entry.type) {
            EntryType.INCOME -> IncomeGreen
            EntryType.EXPENSE -> ExpenseRed
        }
        val iconRes = CategoryIconHelper.getIconRes(categoryName)

        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = stringResource(R.string.cd_entry_icon),
                tint = tint,
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = DateFormatter.formatDate(entry.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        AmountText(
            amountPaisa = entry.amountPaisa,
            type = entry.type,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Preview(name = "Light Mode")
@Composable
private fun EntryRowPreview() {
    KharchaTheme {
        Surface {
            EntryRow(
                entry = LedgerEntry(
                    amountPaisa = 125000L,
                    type = EntryType.EXPENSE,
                    categoryId = 1L,
                    note = "Bhatbhateni Groceries",
                    date = System.currentTimeMillis()
                ),
                categoryName = "Groceries",
                onClick = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EntryRowDarkPreview() {
    KharchaTheme {
        Surface {
            EntryRow(
                entry = LedgerEntry(
                    amountPaisa = 8500000L,
                    type = EntryType.INCOME,
                    categoryId = 2L,
                    note = "Monthly Salary",
                    date = System.currentTimeMillis()
                ),
                categoryName = "Salary",
                onClick = {}
            )
        }
    }
}
