package com.sijan.kharcha.data.repository

import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.domain.model.CategorySpendDelta
import com.sijan.kharcha.domain.model.DayOfWeekSpend
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.MonthOverMonthComparison
import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val ledgerDao: LedgerDao,
    private val categoryDao: CategoryDao
) : AnalyticsRepository {

    override fun getMonthlyAnalytics(year: Int, month: Int): Flow<SpendingAnalyticsSummary> {
        val currentStart = LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val currentEnd = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1L

        val prevMonthDate = LocalDate.of(year, month, 1).minusMonths(1)
        val prevStart = prevMonthDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val prevEnd = currentStart - 1L

        return combine(
            ledgerDao.getEntriesFiltered(type = EntryType.EXPENSE, categoryId = null, startDate = currentStart, endDate = currentEnd),
            ledgerDao.getEntriesFiltered(type = EntryType.EXPENSE, categoryId = null, startDate = prevStart, endDate = prevEnd),
            categoryDao.getAllCategories()
        ) { currentExpenses, prevExpenses, categories ->
            val currentTotal = currentExpenses.sumOf { it.amountPaisa }
            val prevTotal = prevExpenses.sumOf { it.amountPaisa }
            val netDifference = currentTotal - prevTotal

            val percentageChange = if (prevTotal > 0L) {
                ((netDifference * 100L) / prevTotal).toInt()
            } else if (currentTotal > 0L) {
                100
            } else 0

            val categoryMap = categories.associateBy { it.id }

            // Category deltas
            val currentByCat = currentExpenses.groupBy { it.categoryId }
            val prevByCat = prevExpenses.groupBy { it.categoryId }
            val allCatIds = (currentByCat.keys + prevByCat.keys).distinct()

            val categoryDeltas = allCatIds.map { catId ->
                val currentCatTotal = currentByCat[catId]?.sumOf { it.amountPaisa } ?: 0L
                val prevCatTotal = prevByCat[catId]?.sumOf { it.amountPaisa } ?: 0L
                val catDiff = currentCatTotal - prevCatTotal
                val catPct = if (prevCatTotal > 0L) {
                    ((catDiff * 100L) / prevCatTotal).toInt()
                } else if (currentCatTotal > 0L) {
                    100
                } else 0

                CategorySpendDelta(
                    categoryId = catId,
                    categoryName = categoryMap[catId]?.name ?: "General",
                    currentMonthPaisa = currentCatTotal,
                    previousMonthPaisa = prevCatTotal,
                    differencePaisa = catDiff,
                    percentageChange = catPct
                )
            }.sortedByDescending { it.currentMonthPaisa }

            // Day of week spends
            val dayOfWeekMap = DayOfWeek.values().associateWith { mutableListOf<Long>() }
            currentExpenses.forEach { entry ->
                val day = Instant.ofEpochMilli(entry.date).atZone(ZoneId.of("UTC")).dayOfWeek
                dayOfWeekMap[day]?.add(entry.amountPaisa)
            }

            val dayOfWeekSpends = DayOfWeek.values().map { day ->
                val amounts = dayOfWeekMap[day] ?: emptyList()
                DayOfWeekSpend(
                    dayOfWeek = day,
                    totalExpensePaisa = amounts.sum(),
                    transactionCount = amounts.size
                )
            }

            // Top merchants/notes
            val topMerchants = currentExpenses
                .filter { it.note.isNotBlank() }
                .groupBy { it.note.trim() }
                .mapValues { (_, entries) -> entries.sumOf { it.amountPaisa } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)

            // Highest spending day
            val byDay = currentExpenses.groupBy { entry ->
                Instant.ofEpochMilli(entry.date).atZone(ZoneId.of("UTC")).toLocalDate()
            }.mapValues { (_, entries) -> entries.sumOf { it.amountPaisa } }

            val peakDay = byDay.maxByOrNull { it.value }
            val highestDateEpoch = peakDay?.key?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            val highestAmount = peakDay?.value ?: 0L

            SpendingAnalyticsSummary(
                year = year,
                month = month,
                momComparison = MonthOverMonthComparison(
                    currentMonthTotalExpensePaisa = currentTotal,
                    previousMonthTotalExpensePaisa = prevTotal,
                    netDifferencePaisa = netDifference,
                    percentageChange = percentageChange,
                    categoryDeltas = categoryDeltas
                ),
                dayOfWeekSpends = dayOfWeekSpends,
                topMerchants = topMerchants,
                highestSpendDayDate = highestDateEpoch,
                highestSpendDayAmountPaisa = highestAmount
            )
        }
    }
}
