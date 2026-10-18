package com.sijan.kharcha.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NormalizerTest {

    @Test
    fun toAsciiDigitsConvertsDevanagariNumerals() {
        val devanagari = "रु ०१२३४५६७८९"
        val expected = "रु 0123456789"
        assertEquals(expected, Normalizer.toAsciiDigits(devanagari))
    }

    @Test
    fun cleanNumericStringStripsCurrencyAndCommas() {
        assertEquals("1500.50", Normalizer.cleanNumericString("Rs. 1,500.50"))
        assertEquals("250", Normalizer.cleanNumericString("NPR 250"))
        assertEquals("4000.00", Normalizer.cleanNumericString("रु. 4,000.00"))
        assertEquals("99.99", Normalizer.cleanNumericString("रू 99.99"))
    }

    @Test
    fun fixNumericLookalikesCorrectsCommonOcrGlitches() {
        assertEquals("1050", Normalizer.fixNumericLookalikes("lO5O"))
        assertEquals("100", Normalizer.fixNumericLookalikes("IOO"))
        assertEquals("550", Normalizer.fixNumericLookalikes("SS0"))
    }

    @Test
    fun parsePaisaExtractsExactPaisaLong() {
        assertEquals(150050L, Normalizer.parsePaisa("Rs. 1,500.50"))
        assertEquals(25000L, Normalizer.parsePaisa("250"))
        assertEquals(1005L, Normalizer.parsePaisa("10.05"))
        assertEquals(50L, Normalizer.parsePaisa("0.50"))
        assertEquals(123456700L, Normalizer.parsePaisa("1,234,567"))
    }

    @Test
    fun parsePaisaReturnsNullOnMalformedOrNegative() {
        assertNull(Normalizer.parsePaisa("abc"))
        assertNull(Normalizer.parsePaisa("-500.00"))
        assertNull(Normalizer.parsePaisa("12.345")) // More than 2 decimal places
        assertNull(Normalizer.parsePaisa(""))
    }
}
