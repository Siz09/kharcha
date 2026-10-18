package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.parser.model.ValidationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {

    @Test
    fun validateAmountAcceptsPositivePaisa() {
        val field = Validator.validateAmount(150000L, raw = "1500.00")
        assertEquals(ValidationStatus.VALID, field.status)
        assertEquals(150000L, field.value)
        assertTrue(field.errorMessage == null)
    }

    @Test
    fun validateAmountRejectsZeroOrNegativePaisa() {
        val zeroField = Validator.validateAmount(0L, raw = "0.00")
        assertEquals(ValidationStatus.ERROR, zeroField.status)
        assertFalse(zeroField.errorMessage.isNullOrBlank())

        val nullField = Validator.validateAmount(null, raw = "invalid")
        assertEquals(ValidationStatus.ERROR, nullField.status)
        assertFalse(nullField.errorMessage.isNullOrBlank())
    }

    @Test
    fun validateDateAcceptsReasonablePastDate() {
        val now = System.currentTimeMillis()
        val yesterday = now - 86400000L
        val field = Validator.validateDate(yesterday, raw = "Yesterday")
        assertEquals(ValidationStatus.VALID, field.status)
        assertEquals(yesterday, field.value)
    }

    @Test
    fun validateDateRejectsFutureDate() {
        val tomorrow = System.currentTimeMillis() + 86400000L * 2
        val field = Validator.validateDate(tomorrow, raw = "Future Date")
        assertEquals(ValidationStatus.ERROR, field.status)
        assertFalse(field.errorMessage.isNullOrBlank())
    }

    @Test
    fun validateTransactionIdChecksPresenceAndFormat() {
        val validField = Validator.validateTransactionId("TXN_12345678", raw = "TXN_12345678")
        assertEquals(ValidationStatus.VALID, validField.status)
        assertEquals("TXN_12345678", validField.value)

        val emptyField = Validator.validateTransactionId("", raw = "")
        assertEquals(ValidationStatus.ERROR, emptyField.status)

        val nullField = Validator.validateTransactionId(null, raw = "")
        assertEquals(ValidationStatus.ERROR, nullField.status)
    }

    @Test
    fun validateFeeConsistencyChecksTotal() {
        // total = 10500, amount = 10000, fee = 500 -> consistent
        assertTrue(Validator.isFeeConsistent(totalPaisa = 10500L, amountPaisa = 10000L, feePaisa = 500L))
        // inconsistent
        assertFalse(Validator.isFeeConsistent(totalPaisa = 10500L, amountPaisa = 10000L, feePaisa = 100L))
    }
}
