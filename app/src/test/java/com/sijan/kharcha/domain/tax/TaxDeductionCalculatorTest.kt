package com.sijan.kharcha.domain.tax

import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.TaxCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TaxDeductionCalculatorTest {

    @Test
    fun fiscalYearLabel_computesCorrectNepaliFiscalYear() {
        // Shrawan 1, 2081 BS is approx July 16, 2024
        val shrawan2081 = LocalDate.of(2024, 7, 20).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("2081/82", TaxDeductionCalculator.getFiscalYearLabel(shrawan2081))

        // Baishakh 10, 2082 BS is approx April 23, 2025
        val baishakh2082 = LocalDate.of(2025, 4, 23).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("2081/82", TaxDeductionCalculator.getFiscalYearLabel(baishakh2082))

        // Shrawan 15, 2082 BS is approx July 30, 2025
        val shrawan2082 = LocalDate.of(2025, 7, 30).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("2082/83", TaxDeductionCalculator.getFiscalYearLabel(shrawan2082))
    }

    @Test
    fun calculateFiscalYearSummary_enforcesLegalCeilings() {
        val date2081 = LocalDate.of(2024, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val entries = listOf(
            // Life insurance exceeding ceiling of Rs. 40,000 (4,000,000 paisa)
            LedgerEntry(
                id = 1L,
                amountPaisa = 5000000L, // Rs. 50,000
                type = EntryType.EXPENSE,
                categoryId = 1L,
                date = date2081,
                note = "Nepal Life Insurance",
                isTaxDeductible = true,
                taxCategory = TaxCategory.LIFE_INSURANCE
            ),
            // Health insurance within ceiling of Rs. 20,000 (2,000,000 paisa)
            LedgerEntry(
                id = 2L,
                amountPaisa = 1500000L, // Rs. 15,000
                type = EntryType.EXPENSE,
                categoryId = 1L,
                date = date2081,
                note = "Sanima Health Insurance",
                isTaxDeductible = true,
                taxCategory = TaxCategory.HEALTH_INSURANCE
            ),
            // Non-deductible entry
            LedgerEntry(
                id = 3L,
                amountPaisa = 200000L,
                type = EntryType.EXPENSE,
                categoryId = 1L,
                date = date2081,
                note = "Grocery shopping",
                isTaxDeductible = false,
                taxCategory = null
            )
        )

        val summary = TaxDeductionCalculator.calculateFiscalYearSummary(entries, "2081/82")
        assertEquals("2081/82", summary.fiscalYearLabel)
        assertEquals(6500000L, summary.totalSpentPaisa)
        // 4,000,000 (capped life) + 1,500,000 (health) = 5,500,000 paisa
        assertEquals(5500000L, summary.totalDeductiblePaisa)

        val lifeSummary = summary.categorySummaries.first { it.category == TaxCategory.LIFE_INSURANCE }
        assertEquals(5000000L, lifeSummary.totalSpentPaisa)
        assertEquals(4000000L, lifeSummary.eligibleDeductionPaisa)
        assertEquals(0L, lifeSummary.remainingCapPaisa)

        val healthSummary = summary.categorySummaries.first { it.category == TaxCategory.HEALTH_INSURANCE }
        assertEquals(1500000L, healthSummary.totalSpentPaisa)
        assertEquals(1500000L, healthSummary.eligibleDeductionPaisa)
        assertEquals(500000L, healthSummary.remainingCapPaisa)
    }
}
