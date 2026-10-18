package com.sijan.kharcha.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `formatPaisa formats zero correctly`() {
        assertEquals("Rs. 0.00", Money.formatPaisa(0L))
    }

    @Test
    fun `formatPaisa formats small paisa amount`() {
        assertEquals("Rs. 0.50", Money.formatPaisa(50L))
        assertEquals("Rs. 0.05", Money.formatPaisa(5L))
    }

    @Test
    fun `formatPaisa formats rupees with paisa`() {
        assertEquals("Rs. 150.50", Money.formatPaisa(15050L))
        assertEquals("Rs. 1,000.00", Money.formatPaisa(100000L))
    }

    @Test
    fun `parsePaisa parses valid decimal string`() {
        assertEquals(15050L, Money.parsePaisa("150.50"))
        assertEquals(10000L, Money.parsePaisa("100"))
        assertEquals(5L, Money.parsePaisa("0.05"))
        assertEquals(50L, Money.parsePaisa("0.5"))
    }

    @Test
    fun `parsePaisa ignores Rs prefix and commas`() {
        assertEquals(100000L, Money.parsePaisa("Rs. 1,000.00"))
        assertEquals(100000L, Money.parsePaisa("Rs 1000"))
    }

    @Test
    fun `parsePaisa returns null on invalid or negative amounts`() {
        assertNull(Money.parsePaisa(""))
        assertNull(Money.parsePaisa("abc"))
        assertNull(Money.parsePaisa("-50"))
        assertNull(Money.parsePaisa("10.123")) // More than 2 decimals
    }
}
