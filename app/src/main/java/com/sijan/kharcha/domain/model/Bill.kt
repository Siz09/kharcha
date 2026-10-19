package com.sijan.kharcha.domain.model

data class Bill(
    val id: Long = 0L,
    val name: String,
    val amountPaisa: Long,
    val categoryId: Long,
    val dueDayOfMonth: Int,
    val frequency: String = "MONTHLY",
    val isAutoReminder: Boolean = true,
    val lastPaidDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isPaidForMonth(year: Int, month: Int): Boolean {
        if (lastPaidDate == null) return false
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = lastPaidDate
        }
        return cal.get(java.util.Calendar.YEAR) == year && (cal.get(java.util.Calendar.MONTH) + 1) == month
    }
}
