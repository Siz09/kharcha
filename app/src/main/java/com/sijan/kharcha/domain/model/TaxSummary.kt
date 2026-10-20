package com.sijan.kharcha.domain.model

data class TaxDeductionCategorySummary(
    val category: TaxCategory,
    val totalSpentPaisa: Long,
    val eligibleDeductionPaisa: Long,
    val legalCeilingPaisa: Long?,
    val remainingCapPaisa: Long?
)

data class FiscalYearTaxSummary(
    val fiscalYearLabel: String,
    val startEpochUtc: Long,
    val endEpochUtc: Long,
    val totalSpentPaisa: Long,
    val totalDeductiblePaisa: Long,
    val categorySummaries: List<TaxDeductionCategorySummary>
)
