package com.sijan.kharcha.ui.bills

import androidx.compose.runtime.Immutable
import com.sijan.kharcha.domain.model.Bill
import com.sijan.kharcha.domain.model.Category

@Immutable
data class BillItemState(
    val bill: Bill,
    val categoryName: String,
    val isPaidThisMonth: Boolean,
    val daysUntilDue: Int,
    val isOverdue: Boolean
)

@Immutable
data class BillsUiState(
    val bills: List<BillItemState> = emptyList(),
    val categories: List<Category> = emptyList(),
    val totalMonthlyBillsPaisa: Long = 0L,
    val remainingUnpaidPaisa: Long = 0L,
    val isAddEditSheetOpen: Boolean = false,
    val editingBill: Bill? = null,
    val isLoading: Boolean = true
)
