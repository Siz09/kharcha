package com.sijan.kharcha.data.reports

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.MonthlySummary
import com.sijan.kharcha.ui.reports.PdfStatementGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfStatementGeneratorTest {

    @Test
    fun generateStatement_createsFileWithCorrectNameAndContent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = PdfStatementGenerator(context)

        val summary = MonthlySummary(
            totalIncomePaisa = 5000000L,
            totalExpensePaisa = 2500000L,
            balancePaisa = 2500000L
        )

        val entries = listOf(
            LedgerEntry(
                id = 1L,
                amountPaisa = 150000L,
                type = EntryType.EXPENSE,
                categoryId = 1L,
                date = 1761800000000L,
                note = "Groceries shopping"
            )
        )

        val categories = listOf(
            Category(id = 1L, name = "Groceries", type = EntryType.EXPENSE)
        )

        val file = generator.generateMonthlyStatement(
            year = 2026,
            month = 10,
            summary = summary,
            entries = entries,
            categories = categories
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertEquals("Kharcha_Statement_2026_10.pdf", file.name)
        assertTrue(file.length() > 0L)
    }

    @Test
    fun helperMethods_calculateAccurately() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = PdfStatementGenerator(context)

        assertEquals("Kharcha_Statement_2026_5.pdf", generator.getStatementFileName(2026, 5))

        val periodLabel = generator.formatPeriodLabel(2026, 10)
        assertTrue(periodLabel.contains("October 2026"))
        assertTrue(periodLabel.contains("BS"))

        val summary = MonthlySummary(
            totalIncomePaisa = 10000000L,
            totalExpensePaisa = 4000000L,
            balancePaisa = 6000000L
        )
        assertEquals(60, generator.calculateSavingsRate(summary))

        val zeroIncomeSummary = MonthlySummary(
            totalIncomePaisa = 0L,
            totalExpensePaisa = 100000L,
            balancePaisa = -100000L
        )
        assertEquals(0, generator.calculateSavingsRate(zeroIncomeSummary))
    }
}
