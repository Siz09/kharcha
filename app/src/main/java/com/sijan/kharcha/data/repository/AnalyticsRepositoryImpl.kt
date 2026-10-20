package com.sijan.kharcha.data.repository

import com.sijan.kharcha.data.db.dao.AnnualArchiveSummaryDao
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.entity.AnnualArchiveSummaryEntity
import com.sijan.kharcha.domain.calendar.BikramSambatConverter
import com.sijan.kharcha.domain.calendar.NepaliDate
import com.sijan.kharcha.domain.model.AnnualAnalyticsSummary
import com.sijan.kharcha.domain.model.CalendarSystem
import com.sijan.kharcha.domain.model.CategoryAnnualSpend
import com.sijan.kharcha.domain.model.CategorySpendDelta
import com.sijan.kharcha.domain.model.DayOfWeekSpend
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.MonthOverMonthComparison
import com.sijan.kharcha.domain.model.MonthlyFinancialPoint
import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val categoryDao: CategoryDao,
    private val annualArchiveSummaryDao: AnnualArchiveSummaryDao
) : AnalyticsRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val kathmanduZone = ZoneId.of("Asia/Kathmandu")

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

            val topMerchants = currentExpenses
                .filter { it.note.isNotBlank() }
                .groupBy { it.note.trim() }
                .mapValues { (_, entries) -> entries.sumOf { it.amountPaisa } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)

            val dailySpends = currentExpenses
                .groupBy { entry ->
                    Instant.ofEpochMilli(entry.date).atZone(ZoneId.of("UTC")).toLocalDate()
                }
                .mapValues { (_, entries) -> entries.sumOf { it.amountPaisa } }

            val highestSpendEntry = dailySpends.maxByOrNull { it.value }
            val highestSpendDayDate = highestSpendEntry?.key?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            val highestSpendDayAmount = highestSpendEntry?.value ?: 0L

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
                highestSpendDayDate = highestSpendDayDate,
                highestSpendDayAmountPaisa = highestSpendDayAmount
            )
        }
    }

    override fun getAnnualAnalytics(year: Int, calendarSystem: CalendarSystem): Flow<AnnualAnalyticsSummary> = flow {
        // Check if cached/archived summary exists
        val archived = annualArchiveSummaryDao.getSummary(year, calendarSystem.name).first()
        if (archived != null) {
            val breakdown = try {
                json.decodeFromString<List<CategoryAnnualSpend>>(archived.categoryBreakdownJson)
            } catch (_: Exception) {
                emptyList()
            }
            val monthly = try {
                json.decodeFromString<List<MonthlyFinancialPoint>>(archived.monthlyTotalsJson)
            } catch (_: Exception) {
                emptyList()
            }
            val savingsRate = if (archived.totalIncomePaisa > 0L) {
                ((archived.netSavingsPaisa * 100L) / archived.totalIncomePaisa).toInt()
            } else 0
            val burnRate = archived.totalExpensePaisa / 365L

            emit(
                AnnualAnalyticsSummary(
                    year = year,
                    calendarSystem = calendarSystem,
                    totalIncomePaisa = archived.totalIncomePaisa,
                    totalExpensePaisa = archived.totalExpensePaisa,
                    netSavingsPaisa = archived.netSavingsPaisa,
                    savingsRatePercentage = savingsRate,
                    dailyBurnRatePaisa = burnRate,
                    entryCount = archived.entryCount,
                    categoryBreakdown = breakdown,
                    monthlyTotals = monthly,
                    isArchived = true
                )
            )
            return@flow
        }

        // Calculate live analytics
        val (yearStart, yearEnd, monthRanges) = getYearRanges(year, calendarSystem)
        val entries = ledgerDao.getEntriesFiltered(type = null, categoryId = null, startDate = yearStart, endDate = yearEnd).first()
        val categories = categoryDao.getAllCategories().first().associateBy { it.id }

        val incomeEntries = entries.filter { it.type == EntryType.INCOME }
        val expenseEntries = entries.filter { it.type == EntryType.EXPENSE }

        val totalIncome = incomeEntries.sumOf { it.amountPaisa }
        val totalExpense = expenseEntries.sumOf { it.amountPaisa }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0L) ((netSavings * 100L) / totalIncome).toInt() else 0
        val daysInYear = if (calendarSystem == CalendarSystem.BS) {
            if (year in BikramSambatConverter.START_YEAR_BS..BikramSambatConverter.END_YEAR_BS) {
                BikramSambatConverter.getDaysInYear(year)
            } else 365
        } else {
            if (LocalDate.of(year, 1, 1).isLeapYear) 366 else 365
        }
        val burnRate = if (daysInYear > 0) totalExpense / daysInYear else 0L

        // Category breakdown
        val expenseByCat = expenseEntries.groupBy { it.categoryId }
        val categoryBreakdown = expenseByCat.map { (catId, catEntries) ->
            val catTotal = catEntries.sumOf { it.amountPaisa }
            val pct = if (totalExpense > 0L) ((catTotal * 100L) / totalExpense).toInt() else 0
            CategoryAnnualSpend(
                categoryId = catId,
                categoryName = categories[catId]?.name ?: "General",
                totalExpensePaisa = catTotal,
                percentageOfTotal = pct
            )
        }.sortedByDescending { it.totalExpensePaisa }

        // Monthly breakdown
        val monthlyTotals = monthRanges.map { range ->
            val mEntries = entries.filter { it.date in range.startMillis..range.endMillis }
            val mIncome = mEntries.filter { it.type == EntryType.INCOME }.sumOf { it.amountPaisa }
            val mExpense = mEntries.filter { it.type == EntryType.EXPENSE }.sumOf { it.amountPaisa }
            MonthlyFinancialPoint(
                monthIndex = range.monthIndex,
                monthName = range.monthName,
                incomePaisa = mIncome,
                expensePaisa = mExpense,
                netPaisa = mIncome - mExpense
            )
        }

        // Prior year check
        val priorYear = year - 1
        var priorExpense: Long? = null
        var priorIncome: Long? = null
        var yoyChangePct: Int? = null

        val priorArchived = annualArchiveSummaryDao.getSummary(priorYear, calendarSystem.name).first()
        if (priorArchived != null) {
            priorExpense = priorArchived.totalExpensePaisa
            priorIncome = priorArchived.totalIncomePaisa
        } else {
            val (pStart, pEnd, _) = getYearRanges(priorYear, calendarSystem)
            val pEntries = ledgerDao.getEntriesFiltered(type = null, categoryId = null, startDate = pStart, endDate = pEnd).first()
            if (pEntries.isNotEmpty()) {
                priorExpense = pEntries.filter { it.type == EntryType.EXPENSE }.sumOf { it.amountPaisa }
                priorIncome = pEntries.filter { it.type == EntryType.INCOME }.sumOf { it.amountPaisa }
            }
        }

        if (priorExpense != null && priorExpense > 0L) {
            val diff = totalExpense - priorExpense
            yoyChangePct = ((diff * 100L) / priorExpense).toInt()
        }

        emit(
            AnnualAnalyticsSummary(
                year = year,
                calendarSystem = calendarSystem,
                totalIncomePaisa = totalIncome,
                totalExpensePaisa = totalExpense,
                netSavingsPaisa = netSavings,
                savingsRatePercentage = savingsRate,
                dailyBurnRatePaisa = burnRate,
                entryCount = entries.size,
                categoryBreakdown = categoryBreakdown,
                monthlyTotals = monthlyTotals,
                priorYearExpensePaisa = priorExpense,
                priorYearIncomePaisa = priorIncome,
                yearOverYearExpenseChangePercentage = yoyChangePct,
                isArchived = false
            )
        )
    }

    override suspend fun archiveCompletedYear(year: Int, calendarSystem: CalendarSystem): Result<Unit> = runCatching {
        val summary = getAnnualAnalytics(year, calendarSystem).first()
        val entity = AnnualArchiveSummaryEntity(
            year = year,
            calendarSystem = calendarSystem.name,
            totalIncomePaisa = summary.totalIncomePaisa,
            totalExpensePaisa = summary.totalExpensePaisa,
            netSavingsPaisa = summary.netSavingsPaisa,
            categoryBreakdownJson = json.encodeToString(summary.categoryBreakdown),
            monthlyTotalsJson = json.encodeToString(summary.monthlyTotals),
            entryCount = summary.entryCount,
            archivedAt = System.currentTimeMillis()
        )
        annualArchiveSummaryDao.insertSummary(entity)
    }

    private data class MonthRange(
        val monthIndex: Int,
        val monthName: String,
        val startMillis: Long,
        val endMillis: Long
    )

    private fun getYearRanges(year: Int, calendarSystem: CalendarSystem): Triple<Long, Long, List<MonthRange>> {
        if (calendarSystem == CalendarSystem.BS) {
            val safeYear = year.coerceIn(BikramSambatConverter.START_YEAR_BS, BikramSambatConverter.END_YEAR_BS)
            val yearStart = BikramSambatConverter.toEpochMillis(NepaliDate(safeYear, 1, 1))
            val lastMonthDays = BikramSambatConverter.getDaysInMonth(safeYear, 12)
            val yearEnd = BikramSambatConverter.toLocalDate(NepaliDate(safeYear, 12, lastMonthDays))
                .atTime(23, 59, 59, 999_000_000)
                .atZone(kathmanduZone)
                .toInstant()
                .toEpochMilli()

            val monthRanges = (1..12).map { m ->
                val daysInM = BikramSambatConverter.getDaysInMonth(safeYear, m)
                val mStart = BikramSambatConverter.toEpochMillis(NepaliDate(safeYear, m, 1))
                val mEnd = BikramSambatConverter.toLocalDate(NepaliDate(safeYear, m, daysInM))
                    .atTime(23, 59, 59, 999_000_000)
                    .atZone(kathmanduZone)
                    .toInstant()
                    .toEpochMilli()
                MonthRange(
                    monthIndex = m,
                    monthName = NepaliDate.MONTH_NAMES_EN[m - 1].take(3),
                    startMillis = mStart,
                    endMillis = mEnd
                )
            }
            return Triple(yearStart, yearEnd, monthRanges)
        } else {
            val yearStart = LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val yearEnd = LocalDate.of(year, 12, 31).atTime(23, 59, 59, 999_000_000).toInstant(ZoneOffset.UTC).toEpochMilli()

            val monthRanges = (1..12).map { m ->
                val mDate = LocalDate.of(year, m, 1)
                val mStart = mDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val mEnd = LocalDate.of(year, m, mDate.lengthOfMonth()).atTime(23, 59, 59, 999_000_000).toInstant(ZoneOffset.UTC).toEpochMilli()
                MonthRange(
                    monthIndex = m,
                    monthName = mDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                    startMillis = mStart,
                    endMillis = mEnd
                )
            }
            return Triple(yearStart, yearEnd, monthRanges)
        }
    }
}
