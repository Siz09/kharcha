package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun getMonthlyAnalytics(year: Int, month: Int): Flow<SpendingAnalyticsSummary>
}
