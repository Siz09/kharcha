package com.sijan.kharcha.domain.insights

import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.InsightType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.model.SpendingInsight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

object SpendingInsightsEngine {

    private const val MIN_SPIKE_AMOUNT_PAISA = 100000L // Min Rs. 1,000 to trigger spike insight
    private const val DUPLICATE_TIME_WINDOW_MILLIS = 3600000L // 1 hour

    fun generateInsights(
        allEntries: List<LedgerEntry>,
        categories: Map<Long, Category>,
        currentYear: Int,
        currentMonth: Int
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()

        // 1. Detect Category Spikes vs 3-month trailing average
        insights.addAll(detectCategorySpikes(allEntries, categories, currentYear, currentMonth))

        // 2. Detect Potential Duplicate Transactions (recent entries)
        insights.addAll(detectPotentialDuplicates(allEntries))

        return insights
    }

    private fun detectCategorySpikes(
        entries: List<LedgerEntry>,
        categories: Map<Long, Category>,
        currentYear: Int,
        currentMonth: Int
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()

        val currentStart = LocalDate.of(currentYear, currentMonth, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val currentEnd = LocalDate.of(currentYear, currentMonth, 1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1L

        val trailingStart = LocalDate.of(currentYear, currentMonth, 1).minusMonths(3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val trailingEnd = currentStart - 1L

        val currentExpenses = entries.filter { it.type == EntryType.EXPENSE && it.date in currentStart..currentEnd }
        val trailingExpenses = entries.filter { it.type == EntryType.EXPENSE && it.date in trailingStart..trailingEnd }

        val currentByCat = currentExpenses.groupBy { it.categoryId }
        val trailingByCat = trailingExpenses.groupBy { it.categoryId }

        currentByCat.forEach { (catId, catEntries) ->
            val currentCatTotal = catEntries.sumOf { it.amountPaisa }
            val trailingCatTotal = trailingByCat[catId]?.sumOf { it.amountPaisa } ?: 0L
            val threeMonthAvg = trailingCatTotal / 3L

            if (threeMonthAvg > 0L && currentCatTotal >= (threeMonthAvg * 2L) && currentCatTotal >= MIN_SPIKE_AMOUNT_PAISA) {
                val catName = categories[catId]?.name ?: "Category"
                val pctSpike = (((currentCatTotal - threeMonthAvg) * 100L) / threeMonthAvg).toInt()
                insights.add(
                    SpendingInsight(
                        id = "spike_$catId",
                        type = InsightType.CATEGORY_SPIKE,
                        title = "Unusual Spending Spike: $catName",
                        message = "$catName spending (${Money.formatPaisa(currentCatTotal)}) is $pctSpike% higher than your 3-month monthly average (${Money.formatPaisa(threeMonthAvg)}).",
                        categoryId = catId,
                        amountPaisa = currentCatTotal,
                        relatedEntryIds = catEntries.map { it.id }
                    )
                )
            }
        }
        return insights
    }

    private fun detectPotentialDuplicates(entries: List<LedgerEntry>): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        val recentExpenses = entries
            .filter { it.type == EntryType.EXPENSE }
            .sortedByDescending { it.date }
            .take(50)

        val processedIds = mutableSetOf<Long>()

        for (i in 0 until recentExpenses.size) {
            val a = recentExpenses[i]
            if (processedIds.contains(a.id)) continue

            for (j in (i + 1) until recentExpenses.size) {
                val b = recentExpenses[j]
                if (processedIds.contains(b.id)) continue

                if (a.amountPaisa == b.amountPaisa &&
                    a.accountId == b.accountId &&
                    abs(a.date - b.date) <= DUPLICATE_TIME_WINDOW_MILLIS
                ) {
                    processedIds.add(a.id)
                    processedIds.add(b.id)
                    insights.add(
                        SpendingInsight(
                            id = "dup_${a.id}_${b.id}",
                            type = InsightType.POTENTIAL_DUPLICATE,
                            title = "Possible Duplicate Charge",
                            message = "Two identical charges of ${Money.formatPaisa(a.amountPaisa)} were recorded within 1 hour.",
                            amountPaisa = a.amountPaisa,
                            relatedEntryIds = listOf(a.id, b.id)
                        )
                    )
                    break
                }
            }
        }
        return insights
    }
}
