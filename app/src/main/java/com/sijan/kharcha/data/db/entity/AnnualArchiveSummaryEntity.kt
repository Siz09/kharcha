package com.sijan.kharcha.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "annual_archive_summaries",
    primaryKeys = ["year", "calendarSystem"]
)
data class AnnualArchiveSummaryEntity(
    val year: Int,
    val calendarSystem: String, // "AD" or "BS"
    val totalIncomePaisa: Long,
    val totalExpensePaisa: Long,
    val netSavingsPaisa: Long,
    val categoryBreakdownJson: String,
    val monthlyTotalsJson: String,
    val entryCount: Int,
    val archivedAt: Long
)
