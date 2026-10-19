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

    private fun assertFixtureMatches(fixtureName: String) {
        val rawTextStream = javaClass.classLoader?.getResourceAsStream("fixtures/$fixtureName.txt")
        assertNotNull("Fixture raw text must exist for $fixtureName", rawTextStream)
        val rawText = rawTextStream!!.bufferedReader().use { it.readText() }

        val jsonStream = javaClass.classLoader?.getResourceAsStream("fixtures/$fixtureName.json")
        assertNotNull("Fixture json must exist for $fixtureName", jsonStream)
        val jsonText = jsonStream!!.bufferedReader().use { it.readText() }
        val expected = json.decodeFromString<ExpectedFixture>(jsonText)

        val ocr = OcrResult(fullText = rawText)
        assertTrue("Parser must recognize $fixtureName", parser.canParse(ocr))

        val result = parser.parse(ocr)

        assertEquals(expected.source, result.source)
        assertEquals(expected.expectedAmountPaisa, result.amountPaisa.value)
        assertEquals(ValidationStatus.VALID, result.amountPaisa.status)
        assertEquals(expected.expectedTransactionId, result.transactionId.value)
        assertEquals(ValidationStatus.VALID, result.transactionId.status)
        assertEquals(expected.expectedDateEpochMillis, result.dateEpochMillis.value)
        assertEquals(ValidationStatus.VALID, result.dateEpochMillis.status)
        assertEquals(expected.expectedMerchant, result.merchant.value)
        assertEquals(expected.expectedFeePaisa, result.feePaisa.value)
        assertTrue("Overall parse result must be valid for $fixtureName", result.isValid)
    }

    @Test
    fun evaluateEsewaSample1FixtureAccuracy() {
        assertFixtureMatches("esewa_sample_1")
    }

    @Test
    fun evaluateEsewaSample2FixtureAccuracy() {
        assertFixtureMatches("esewa_sample_2")
    }

    @Test
    fun evaluateEsewaSample3FixtureAccuracy() {
        assertFixtureMatches("esewa_sample_3")
    }

    @Test
    fun evaluateEsewaSample4FixtureAccuracy() {
        assertFixtureMatches("esewa_sample_4")
    }

    @Test
    fun evaluateEsewaSample5FixtureAccuracy() {
        assertFixtureMatches("esewa_sample_5")
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
