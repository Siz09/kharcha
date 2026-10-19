package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BankSmsParserTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setup() {
        parser = BankSmsParser()
    }

    @Test
    fun parseNabilBankDebitSmsSuccessfully() {
        val sender = "NABIL"
        val body = "Dear Customer, your A/C ...1234 has been debited by NPR 2,500.00 on 2026-10-18 11:20:00 for POS Purchase at Bhatbhateni Supermarket. Ref: TXN998811. Avail Bal NPR 50,000.00"

        val result = parser.parse(sender, body)

        assertNotNull("Nabil debit SMS must be parsed", result)
        assertEquals("Nabil Bank", result?.bankName)
        assertEquals(250000L, result?.amountPaisa)
        assertEquals(EntryType.EXPENSE, result?.type)
        assertEquals("TXN998811", result?.transactionId)
        assertEquals("...1234", result?.accountNumberTail)
        assertEquals("Bhatbhateni Supermarket", result?.merchant)
    }

    @Test
    fun parseNicAsiaCreditSmsSuccessfully() {
        val sender = "NICASIA"
        val body = "Your A/C ...5678 is credited for NPR 15,000.00 on 18-Oct-2026 14:10:05 by Salary Deposit. Ref: SAL12345. Available Bal NPR 85,000.00"

        val result = parser.parse(sender, body)

        assertNotNull("NIC Asia credit SMS must be parsed", result)
        assertEquals("NIC Asia Bank", result?.bankName)
        assertEquals(1500000L, result?.amountPaisa)
        assertEquals(EntryType.INCOME, result?.type)
        assertEquals("SAL12345", result?.transactionId)
        assertEquals("...5678", result?.accountNumberTail)
    }

    @Test
    fun parseGlobalImeDebitSmsSuccessfully() {
        val sender = "GLOBAL"
        val body = "A/C *4321 debited by Rs. 850.00 on 18/10/2026. Remarks: Subisu Internet. Txn ID: GIBL776655."

        val result = parser.parse(sender, body)

        assertNotNull("Global IME debit SMS must be parsed", result)
        assertEquals("Global IME Bank", result?.bankName)
        assertEquals(85000L, result?.amountPaisa)
        assertEquals(EntryType.EXPENSE, result?.type)
        assertEquals("GIBL776655", result?.transactionId)
        assertEquals("...4321", result?.accountNumberTail)
        assertEquals("Subisu Internet", result?.merchant)
    }

    @Test
    fun parseNimbDebitSmsSuccessfully() {
        val sender = "NIMB"
        val body = "Dear Customer, your A/C ...9999 has been debited by NPR 3,400.00 on 2026-10-18 for QR Payment at Himalayan Java. Ref: NIMB443322."

        val result = parser.parse(sender, body)

        assertNotNull("NIMB debit SMS must be parsed", result)
        assertEquals("Nepal Investment Mega Bank", result?.bankName)
        assertEquals(340000L, result?.amountPaisa)
        assertEquals(EntryType.EXPENSE, result?.type)
        assertEquals("NIMB443322", result?.transactionId)
        assertEquals("...9999", result?.accountNumberTail)
        assertEquals("Himalayan Java", result?.merchant)
    }

    @Test
    fun otpMessagesAreRejected() {
        val sender = "NABIL"
        val body = "Your Nabil Bank one time password is 776655 for transaction of NPR 1,200.00. Do not share this OTP with anyone."

        val result = parser.parse(sender, body)
        assertNull("OTP messages must never be parsed as transactions", result)
    }

    @Test
    fun nonFinancialAndSpamMessagesAreRejected() {
        assertNull(parser.parse("AD-OFFER", "Congratulations! You are eligible for pre-approved loan of NPR 500,000."))
        assertNull(parser.parse("FRIEND", "Hey bro, let's meet at 5 PM."))
        assertNull(parser.parse("NABIL", "Your A/C ...1234 has been debited by NPR 0.00."))
        assertNull(parser.parse("", ""))
    }
}
