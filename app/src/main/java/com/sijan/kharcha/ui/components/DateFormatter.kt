package com.sijan.kharcha.ui.components

import com.sijan.kharcha.domain.calendar.BikramSambatConverter
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

    fun formatDualDate(epochMillis: Long): String {
        return BikramSambatConverter.formatDualDate(epochMillis)
    }

    fun formatBsDate(epochMillis: Long, includeYear: Boolean = true): String {
        return BikramSambatConverter.fromEpochMillis(epochMillis).format(includeYear)
    }

    fun formatMonthYear(epochMillis: Long): String {
        return monthYearFormat.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long): String {
        return dateTimeFormat.format(Date(epochMillis))
    }
}
