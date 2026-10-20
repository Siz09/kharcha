package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvStatementParserTest {

    @Test
    fun parse_standardCsv_extractsValidRowsCorrectly() {
        val csvData = """
            Date,Amount,Type,Category,Note,TransactionId
            2026-10-15,1500.50,EXPENSE,Food,Grocery Store,TXN1001
            2026-10-16,5000,INCOME,Salary,Monthly Salary,TXN1002
            2026-10-17,200.25,EXPENSE,Travel,"Bus fare, taxi",TXN1003
        """.trimIndent()

        val preview = CsvStatementParser.parse(ByteArrayInputStream(csvData.toByteArray()))

        assertEquals(3, preview.totalRows)
        assertEquals(3, preview.validRows.size)
        assertEquals(0, preview.duplicateRows.size)
        assertEquals(0, preview.errorRows.size)

        val first = preview.validRows[0]
        assertEquals(150050L, first.amountPaisa)
        assertEquals(EntryType.EXPENSE, first.type)
        assertEquals("Food", first.categoryName)
        assertEquals("Grocery Store", first.note)
        assertEquals("TXN1001", first.transactionId)

        val third = preview.validRows[2]
        assertEquals(20025L, third.amountPaisa)
        assertEquals("Bus fare, taxi", third.note) // Handles comma inside quotes
    }

    @Test
    fun parse_detectsDuplicateTransactionIds() {
        val csvData = """
            Date,Amount,Type,Category,Note,TransactionId
            2026-10-15,1500,EXPENSE,Food,Lunch,TXN_EXISTING
            2026-10-16,2500,EXPENSE,Shopping,Clothes,TXN_NEW
        """.trimIndent()

        val preview = CsvStatementParser.parse(
            ByteArrayInputStream(csvData.toByteArray()),
            existingTransactionIds = setOf("TXN_EXISTING")
        )

        assertEquals(2, preview.totalRows)
        assertEquals(1, preview.validRows.size)
        assertEquals(1, preview.duplicateRows.size)
        assertEquals(0, preview.errorRows.size)

        assertEquals("TXN_EXISTING", preview.duplicateRows[0].transactionId)
        assertTrue(preview.duplicateRows[0].isDuplicate)
        assertEquals("TXN_NEW", preview.validRows[0].transactionId)
        assertFalse(preview.validRows[0].isDuplicate)
    }

    @Test
    fun parse_handlesInvalidRowsGracefullyWithoutCrashing() {
        val csvData = """
            Date,Amount,Type,Category,Note,TransactionId
            INVALID_DATE,1500,EXPENSE,Food,Lunch,TXN1
            2026-10-15,NOT_AN_AMOUNT,EXPENSE,Food,Lunch,TXN2
            2026-10-15,1000,EXPENSE,Food,Valid,TXN3
        """.trimIndent()

        val preview = CsvStatementParser.parse(ByteArrayInputStream(csvData.toByteArray()))

        assertEquals(3, preview.totalRows)
        assertEquals(1, preview.validRows.size)
        assertEquals(2, preview.errorRows.size)
        assertEquals(100000L, preview.validRows[0].amountPaisa)
    }
}
