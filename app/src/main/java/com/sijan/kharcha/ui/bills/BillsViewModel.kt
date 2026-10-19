package com.sijan.kharcha.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.bills.BillReminderScheduler
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.Bill
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val reminderScheduler: BillReminderScheduler
) : ViewModel() {

    private val isAddEditSheetOpen = MutableStateFlow(false)
    private val editingBill = MutableStateFlow<Bill?>(null)

    init {
        reminderScheduler.scheduleDailyBillCheck()
    }

    val uiState: StateFlow<BillsUiState> = combine(
        repository.getAllBills(),
        repository.getCategoriesByType(EntryType.EXPENSE),
        isAddEditSheetOpen,
        editingBill
    ) { bills, categories, isOpen, editing ->
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        val catMap = categories.associateBy { it.id }

        val items = bills.map { bill ->
            val isPaid = bill.isPaidForMonth(currentYear, currentMonth)
            val daysUntil = bill.dueDayOfMonth - currentDay
            val isOverdue = !isPaid && (daysUntil < 0)

            BillItemState(
                bill = bill,
                categoryName = catMap[bill.categoryId]?.name ?: "",
                isPaidThisMonth = isPaid,
                daysUntilDue = daysUntil,
                isOverdue = isOverdue
            )
        }

        val totalPaisa = bills.sumOf { it.amountPaisa }
        val remainingPaisa = items.filter { !it.isPaidThisMonth }.sumOf { it.bill.amountPaisa }

        BillsUiState(
            bills = items,
            categories = categories,
            totalMonthlyBillsPaisa = totalPaisa,
            remainingUnpaidPaisa = remainingPaisa,
            isAddEditSheetOpen = isOpen,
            editingBill = editing,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillsUiState()
    )

    fun openAddDialog() {
        editingBill.value = null
        isAddEditSheetOpen.value = true
    }

    fun openEditDialog(bill: Bill) {
        editingBill.value = bill
        isAddEditSheetOpen.value = true
    }

    fun dismissDialog() {
        editingBill.value = null
        isAddEditSheetOpen.value = false
    }

    fun saveBill(
        name: String,
        amountPaisa: Long,
        categoryId: Long,
        dueDay: Int,
        isAutoReminder: Boolean,
        billId: Long = 0L,
        onComplete: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = if (billId > 0L) {
                val existing = editingBill.value
                val updated = Bill(
                    id = billId,
                    name = name.trim(),
                    amountPaisa = amountPaisa,
                    categoryId = categoryId,
                    dueDayOfMonth = dueDay,
                    frequency = "MONTHLY",
                    isAutoReminder = isAutoReminder,
                    lastPaidDate = existing?.lastPaidDate,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateBill(updated)
            } else {
                val newBill = Bill(
                    id = 0L,
                    name = name.trim(),
                    amountPaisa = amountPaisa,
                    categoryId = categoryId,
                    dueDayOfMonth = dueDay,
                    frequency = "MONTHLY",
                    isAutoReminder = isAutoReminder,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveBill(newBill).map { }
            }
            if (result.isSuccess) {
                dismissDialog()
            }
            onComplete(result)
        }
    }

    fun deleteBill(billId: Long, onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteBill(billId)
            onComplete(result)
        }
    }

    fun payBill(billId: Long, onResult: (Result<LedgerEntry>) -> Unit) {
        viewModelScope.launch {
            val result = repository.payBill(billId)
            onResult(result)
        }
    }
}
