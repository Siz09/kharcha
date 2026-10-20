package com.sijan.kharcha.domain.tax

import com.sijan.kharcha.domain.calendar.BikramSambatConverter
import com.sijan.kharcha.domain.model.FiscalYearTaxSummary
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.TaxCategory
import com.sijan.kharcha.domain.model.TaxDeductionCategorySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

object TaxDeductionCalculator {

    fun getFiscalYearLabel(dateEpochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(dateEpochMillis).atZone(ZoneId.of("UTC")).toLocalDate()
        val bsDate = BikramSambatConverter.fromLocalDate(localDate)
        return if (bsDate.month >= 4) {
            "${bsDate.year}/${(bsDate.year + 1) % 100}"
        } else {
            "${bsDate.year - 1}/${bsDate.year % 100}"
        }
    }

    fun calculateFiscalYearSummary(
        entries: List<LedgerEntry>,
        targetFiscalYearLabel: String? = null
    ): FiscalYearTaxSummary {
        val deductibleEntries = entries.filter { it.isTaxDeductible && it.taxCategory != null }

        val activeFyLabel = targetFiscalYearLabel ?: if (deductibleEntries.isNotEmpty()) {
            getFiscalYearLabel(deductibleEntries.maxOf { it.date })
        } else {
            getFiscalYearLabel(System.currentTimeMillis())
        }

        val fyEntries = deductibleEntries.filter { getFiscalYearLabel(it.date) == activeFyLabel }

        val categorySummaries = TaxCategory.entries.map { category ->
            val catEntries = fyEntries.filter { it.taxCategory == category }
            val totalSpent = catEntries.sumOf { it.amountPaisa }
            val ceiling = category.annualCeilingPaisa
            val eligible = if (ceiling != null) minOf(totalSpent, ceiling) else totalSpent
            val remainingCap = if (ceiling != null) maxOf(0L, ceiling - totalSpent) else null

            TaxDeductionCategorySummary(
                category = category,
                totalSpentPaisa = totalSpent,
                eligibleDeductionPaisa = eligible,
                legalCeilingPaisa = ceiling,
                remainingCapPaisa = remainingCap
            )
        }

        val totalSpent = categorySummaries.sumOf { it.totalSpentPaisa }
        val totalDeductible = categorySummaries.sumOf { it.eligibleDeductionPaisa }

        val startEpoch = fyEntries.minOfOrNull { it.date } ?: 0L
        val endEpoch = fyEntries.maxOfOrNull { it.date } ?: 0L

        return FiscalYearTaxSummary(
            fiscalYearLabel = activeFyLabel,
            startEpochUtc = startEpoch,
            endEpochUtc = endEpoch,
            totalSpentPaisa = totalSpent,
            totalDeductiblePaisa = totalDeductible,
            categorySummaries = categorySummaries
        )
    }
}
