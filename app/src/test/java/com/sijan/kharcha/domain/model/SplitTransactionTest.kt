package com.sijan.kharcha.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SplitTransactionTest {

    @Test
    fun splitTransaction_matchingAmounts_instantiatesSuccessfully() {
        val entry = LedgerEntry(
            id = 1L,
            amountPaisa = 100000L, // Rs. 1000
            type = EntryType.EXPENSE,
            categoryId = 1L,
            date = 1761800000000L
        )

        val splits = listOf(
            SplitItem(categoryId = 1L, amountPaisa = 60000L, note = "Vegetables"),
            SplitItem(categoryId = 2L, amountPaisa = 40000L, note = "Dairy")
        )

        val splitTxn = SplitTransaction(entry, splits)
        assertEquals(2, splitTxn.splits.size)
        assertEquals(100000L, splitTxn.splits.sumOf { it.amountPaisa })
    }

    @Test
    fun splitTransaction_mismatchingAmounts_throwsIllegalArgumentException() {
        val entry = LedgerEntry(
            id = 1L,
            amountPaisa = 100000L, // Rs. 1000
            type = EntryType.EXPENSE,
            categoryId = 1L,
            date = 1761800000000L
        )

        val invalidSplits = listOf(
            SplitItem(categoryId = 1L, amountPaisa = 60000L),
            SplitItem(categoryId = 2L, amountPaisa = 30000L) // Sum = 90,000, mismatch!
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            SplitTransaction(entry, invalidSplits)
        }
        assert(exception.message!!.contains("must strictly equal entry total"))
    }
}
