package com.sijan.kharcha.domain.model

data class UdharSummary(
    val totalLentPaisa: Long = 0L,         // Total remaining receivable from people
    val totalBorrowedPaisa: Long = 0L,     // Total remaining payable to people
    val totalSettledPaisa: Long = 0L,      // Total historical settled debt
    val netReceivablePaisa: Long = 0L      // totalLentPaisa - totalBorrowedPaisa
)
