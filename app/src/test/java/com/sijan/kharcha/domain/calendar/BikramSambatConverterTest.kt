package com.sijan.kharcha.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BikramSambatConverterTest {

    @Test
    fun anchorDate_matches1975Baisakh1() {
        val dateAd = LocalDate.of(1918, 4, 13)
        val bsDate = BikramSambatConverter.fromLocalDate(dateAd)
        assertEquals(NepaliDate(1975, 1, 1), bsDate)
        assertEquals(dateAd, BikramSambatConverter.toLocalDate(bsDate))
    }

    @Test
    fun roundTripConversion_preservesDatesAcrossDecades() {
        val testDates = listOf(
            LocalDate.of(1950, 1, 1),
            LocalDate.of(1980, 6, 15),
            LocalDate.of(2000, 12, 31),
            LocalDate.of(2015, 4, 25),
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2026, 10, 19),
            LocalDate.of(2035, 8, 20)
        )

        for (adDate in testDates) {
            val bsDate = BikramSambatConverter.fromLocalDate(adDate)
            val convertedBack = BikramSambatConverter.toLocalDate(bsDate)
            assertEquals("Round-trip failed for $adDate", adDate, convertedBack)
        }
    }

    @Test
    fun knownDate2026_convertsTo2083Kartik2() {
        val adDate = LocalDate.of(2026, 10, 19)
        val bsDate = BikramSambatConverter.fromLocalDate(adDate)
        assertEquals(2083, bsDate.year)
        assertEquals(7, bsDate.month) // Kartik
        assertEquals("Kartik", bsDate.monthName)
        assertEquals(2, bsDate.day)
    }

    @Test
    fun formatDualDate_producesExpectedFormat() {
        val epochMillis = BikramSambatConverter.toEpochMillis(NepaliDate(2083, 7, 2))
        val dualDate = BikramSambatConverter.formatDualDate(epochMillis)
        assertTrue(dualDate.contains("2026"))
        assertTrue(dualDate.contains("Kartik 2, 2083 BS"))
    }

    @Test
    fun getDaysInMonth_respectsBounds() {
        val days = BikramSambatConverter.getDaysInMonth(2083, 1)
        assertEquals(31, days)
        val chaitraDays = BikramSambatConverter.getDaysInMonth(2083, 12)
        assertEquals(30, chaitraDays)
    }
}
