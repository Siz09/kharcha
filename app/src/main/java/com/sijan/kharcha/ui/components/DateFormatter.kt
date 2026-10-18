package com.sijan.kharcha.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    private val entryDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    fun formatDate(epochMillis: Long): String {
        return entryDateFormat.format(Date(epochMillis))
    }

    fun formatMonthYear(epochMillis: Long): String {
        return monthYearFormat.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long): String {
        return dateTimeFormat.format(Date(epochMillis))
    }
}
