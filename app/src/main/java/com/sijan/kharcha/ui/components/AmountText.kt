package com.sijan.kharcha.ui.components

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.ui.theme.ExpenseRed
import com.sijan.kharcha.ui.theme.IncomeGreen
import com.sijan.kharcha.ui.theme.KharchaTheme

@Composable
fun AmountText(
    amountPaisa: Long,
    type: EntryType? = null,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    colorOverride: Color? = null
) {
    val isNegative = amountPaisa < 0
    val absPaisa = kotlin.math.abs(amountPaisa)
    val formatted = Money.formatPaisa(absPaisa)
    val (displayText, textColor) = when (type) {
        EntryType.INCOME -> "+$formatted" to IncomeGreen
        EntryType.EXPENSE -> "-$formatted" to ExpenseRed
        null -> {
            val text = if (isNegative) "-$formatted" else formatted
            val color = if (isNegative) ExpenseRed else (colorOverride ?: MaterialTheme.colorScheme.onSurface)
            text to color
        }
    }

    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = colorOverride ?: textColor
    )
}

@Preview(name = "Light Mode")
@Composable
private fun AmountTextPreview() {
    KharchaTheme {
        AmountText(amountPaisa = 150000L, type = EntryType.EXPENSE)
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AmountTextDarkPreview() {
    KharchaTheme {
        AmountText(amountPaisa = 5000000L, type = EntryType.INCOME)
    }
}
