package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.parser.model.OcrBlock
import com.sijan.kharcha.domain.parser.model.OcrLine
import com.sijan.kharcha.domain.parser.model.OcrResult
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EsewaParserTest {

    private lateinit var parser: EsewaParser

    @Before
    fun setup() {
        parser = EsewaParser()
    }

    @Test
    fun canParseIdentifiesEsewaKeyword() {
        val ocr = OcrResult(
            fullText = "eSewa Payment Successful\nTransaction ID: 12345678\nAmount: Rs. 500"
        )
        assertTrue(parser.canParse(ocr))
    }

    @Test
    fun canParseRejectsUnrelatedReceipt() {
        val ocr = OcrResult(
            fullText = "Grocery Store Tax Invoice\nItem: Milk Rs. 50\nThank you"
        )
        assertFalse(parser.canParse(ocr))
    }

    @Test
    fun parseExtractsValidEsewaFields() {
        val text = """
            eSewa
            Payment Successful
            Transaction ID
            TXN9988776655
            Paid To
            Bhatbhateni Supermarket
            Total Amount
            Rs. 1,500.50
            Date & Time
            2026-10-18 14:30:00
        """.trimIndent()

        val ocr = OcrResult(fullText = text)
        val result = parser.parse(ocr)

        assertEquals("esewa", result.source)
        assertEquals(150050L, result.amountPaisa.value)
        assertEquals(ValidationStatus.VALID, result.amountPaisa.status)

        assertEquals("TXN9988776655", result.transactionId.value)
        assertEquals(ValidationStatus.VALID, result.transactionId.status)

        assertEquals("Bhatbhateni Supermarket", result.merchant.value)
        assertNotNull(result.dateEpochMillis.value)
        assertEquals(ValidationStatus.VALID, result.dateEpochMillis.status)
        assertTrue(result.isValid)
    }

    @Test
    fun parseHandlesMissingFieldsWithValidationErrors() {
        val malformedText = "eSewa\nPayment Pending\nAmount: invalid"
        val ocr = OcrResult(fullText = malformedText)
        val result = parser.parse(ocr)

        assertEquals(ValidationStatus.ERROR, result.amountPaisa.status)
        assertEquals(ValidationStatus.ERROR, result.transactionId.status)
        assertFalse(result.isValid)
    }
}
