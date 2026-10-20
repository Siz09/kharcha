package com.sijan.kharcha.domain.model

data class SplitItem(
    val id: Long = 0L,
    val entryId: Long = 0L,
    val categoryId: Long,
    val amountPaisa: Long,
    val note: String? = null
) {
    init {
        require(amountPaisa > 0L) { "Split item amount must be positive" }
    }
}

data class SplitTransaction(
    val entry: LedgerEntry,
    val splits: List<SplitItem>
) {
    init {
        if (splits.isNotEmpty()) {
            val totalSplits = splits.sumOf { it.amountPaisa }
            require(totalSplits == entry.amountPaisa) {
                "Sum of split amounts ($totalSplits) must strictly equal entry total (${entry.amountPaisa})"
            }
        }
    }
}
