package com.sijan.kharcha.domain.insights

import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.InsightType
import com.sijan.kharcha.domain.model.LedgerEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class SpendingInsightsEngineTest {

    @Test
    fun generateInsights_detectsCategorySpendingSpikes() {
        val categories = mapOf(
            1L to Category(id = 1L, name = "Dining", type = EntryType.EXPENSE)
        )

        // Trailing 3 months: Rs. 1,000 / month (100,000 paisa). Total = 300,000 paisa. 3-mo avg = 100,000.
        val m1Date = LocalDate.of(2026, 7, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val m2Date = LocalDate.of(2026, 8, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val m3Date = LocalDate.of(2026, 9, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // Current month (October 2026): Rs. 2,500 (250,000 paisa) -> 2.5x spike!
        val currentMonthDate = LocalDate.of(2026, 10, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val entries = listOf(
            LedgerEntry(id = 1L, amountPaisa = 100000L, type = EntryType.EXPENSE, categoryId = 1L, date = m1Date),
            LedgerEntry(id = 2L, amountPaisa = 100000L, type = EntryType.EXPENSE, categoryId = 1L, date = m2Date),
            LedgerEntry(id = 3L, amountPaisa = 100000L, type = EntryType.EXPENSE, categoryId = 1L, date = m3Date),
            LedgerEntry(id = 4L, amountPaisa = 250000L, type = EntryType.EXPENSE, categoryId = 1L, date = currentMonthDate)
        )

        val insights = SpendingInsightsEngine.generateInsights(
            allEntries = entries,
            categories = categories,
            currentYear = 2026,
            currentMonth = 10
        )

        val spike = insights.find { it.type == InsightType.CATEGORY_SPIKE }
        assertTrue(spike != null)
        assertEquals(1L, spike!!.categoryId)
        assertEquals(250000L, spike.amountPaisa)
        assertTrue(spike.title.contains("Dining"))
    }

    @Test
    fun generateInsights_detectsPotentialDuplicateCharges() {
        val baseDate = 1761800000000L
        val tenMinutesLater = baseDate + (10 * 60 * 1000L)

        val entries = listOf(
            LedgerEntry(id = 10L, amountPaisa = 150000L, type = EntryType.EXPENSE, categoryId = 1L, accountId = 1L, date = baseDate, note = "Bhatbhateni"),
            LedgerEntry(id = 11L, amountPaisa = 150000L, type = EntryType.EXPENSE, categoryId = 1L, accountId = 1L, date = tenMinutesLater, note = "Bhatbhateni")
        )

        val insights = SpendingInsightsEngine.generateInsights(
            allEntries = entries,
            categories = emptyMap(),
            currentYear = 2026,
            currentMonth = 10
        )

        val duplicate = insights.find { it.type == InsightType.POTENTIAL_DUPLICATE }
        assertTrue(duplicate != null)
        assertEquals(150000L, duplicate!!.amountPaisa)
        assertEquals(listOf(11L, 10L), duplicate.relatedEntryIds)
    }
}
