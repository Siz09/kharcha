package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.AnnualAnalyticsSummary
import com.sijan.kharcha.domain.model.CalendarSystem
import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun getMonthlyAnalytics(year: Int, month: Int): Flow<SpendingAnalyticsSummary>
    fun getAnnualAnalytics(year: Int, calendarSystem: CalendarSystem): Flow<AnnualAnalyticsSummary>
    suspend fun archiveCompletedYear(year: Int, calendarSystem: CalendarSystem): Result<Unit>
}
