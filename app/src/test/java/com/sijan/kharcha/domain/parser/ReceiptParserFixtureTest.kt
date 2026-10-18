package com.sijan.kharcha.domain.parser

import com.sijan.kharcha.domain.parser.model.OcrResult
import com.sijan.kharcha.domain.parser.model.ValidationStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

@Serializable
data class ExpectedFixture(
    val source: String,
    val expectedAmountPaisa: Long,
    val expectedDateEpochMillis: Long,
    val expectedTransactionId: String,
    val expectedMerchant: String,
    val expectedFeePaisa: Long = 0L
)

class ReceiptParserFixtureTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val parser = EsewaParser()

    @Test
    fun evaluateEsewaSample1FixtureAccuracy() {
        val rawTextStream = javaClass.classLoader?.getResourceAsStream("fixtures/esewa_sample_1.txt")
        assertNotNull("Fixture raw text must exist", rawTextStream)
        val rawText = rawTextStream!!.bufferedReader().use { it.readText() }

        val jsonStream = javaClass.classLoader?.getResourceAsStream("fixtures/esewa_sample_1.json")
        assertNotNull("Fixture json must exist", jsonStream)
        val jsonText = jsonStream!!.bufferedReader().use { it.readText() }
        val expected = json.decodeFromString<ExpectedFixture>(jsonText)

        val ocr = OcrResult(fullText = rawText)
        assertTrue(parser.canParse(ocr))

        val result = parser.parse(ocr)

        // Verify per-field accuracy
        assertEquals(expected.source, result.source)

        // Amount accuracy (100% on sample)
        assertEquals(expected.expectedAmountPaisa, result.amountPaisa.value)
        assertEquals(ValidationStatus.VALID, result.amountPaisa.status)

        // Transaction ID accuracy
        assertEquals(expected.expectedTransactionId, result.transactionId.value)
        assertEquals(ValidationStatus.VALID, result.transactionId.status)

        // Date accuracy
        assertEquals(expected.expectedDateEpochMillis, result.dateEpochMillis.value)
        assertEquals(ValidationStatus.VALID, result.dateEpochMillis.status)

        // Merchant accuracy
        assertEquals(expected.expectedMerchant, result.merchant.value)

        // Fee accuracy
        assertEquals(expected.expectedFeePaisa, result.feePaisa.value)

        assertTrue("Overall parse result must be valid", result.isValid)
    }

    @Test
    fun malformedReceiptDoesNotSilentlySucceed() {
        val corruptedText = """
            eSewa
            Failed Transaction
            Paid To: Unknown
            Amount: N/A
        """.trimIndent()

        val ocr = OcrResult(fullText = corruptedText)
        val result = parser.parse(ocr)

        // Must reject amount and mark error, never silently 0 or valid
        assertEquals(ValidationStatus.ERROR, result.amountPaisa.status)
        assertFalse(result.isValid)
    }
}
