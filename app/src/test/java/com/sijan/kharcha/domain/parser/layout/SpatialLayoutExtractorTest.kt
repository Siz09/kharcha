package com.sijan.kharcha.domain.parser.layout

import com.sijan.kharcha.domain.parser.Normalizer
import com.sijan.kharcha.domain.parser.model.BoundingBox
import com.sijan.kharcha.domain.parser.model.OcrBlock
import com.sijan.kharcha.domain.parser.model.OcrLine
import com.sijan.kharcha.domain.parser.model.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SpatialLayoutExtractorTest {

    @Test
    fun extractFieldFromTwoColumnLayoutUsingBoundingBoxes() {
        // In two-column receipts, OCR often detects all column 1 labels first, then column 2 values.
        val block1 = OcrBlock(
            text = "Total Amount\nTransaction ID\nPaid To",
            lines = listOf(
                OcrLine(text = "Total Amount", boundingBox = BoundingBox(left = 20, top = 100, right = 200, bottom = 140)),
                OcrLine(text = "Transaction ID", boundingBox = BoundingBox(left = 20, top = 160, right = 220, bottom = 200)),
                OcrLine(text = "Paid To", boundingBox = BoundingBox(left = 20, top = 220, right = 150, bottom = 260))
            )
        )
        val block2 = OcrBlock(
            text = "Rs. 4,500.00\nTXN_99881122\nBhatbhateni Supermarket",
            lines = listOf(
                OcrLine(text = "Rs. 4,500.00", boundingBox = BoundingBox(left = 300, top = 100, right = 480, bottom = 140)),
                OcrLine(text = "TXN_99881122", boundingBox = BoundingBox(left = 300, top = 160, right = 490, bottom = 200)),
                OcrLine(text = "Bhatbhateni Supermarket", boundingBox = BoundingBox(left = 300, top = 220, right = 600, bottom = 260))
            )
        )

        val ocr = OcrResult(
            fullText = "${block1.text}\n${block2.text}",
            blocks = listOf(block1, block2)
        )

        val extractor = SpatialLayoutExtractor(ocr)

        // 1. Amount should be matched to Rs. 4,500.00 on the right (same vertical band: 100..140)
        val amount = extractor.extractField(Regex("(?i).*total\\s*amount.*")) { candidate ->
            Normalizer.parsePaisa(Normalizer.toAsciiDigits(candidate))
        }
        assertNotNull(amount)
        assertEquals(450000L, amount?.first)

        // 2. Transaction ID matched to TXN_99881122 on the right (same vertical band: 160..200)
        val txn = extractor.extractField(Regex("(?i).*transaction\\s*id.*")) { candidate ->
            candidate.trim().takeIf { it.startsWith("TXN") }
        }
        assertNotNull(txn)
        assertEquals("TXN_99881122", txn?.first)

        // 3. Paid To matched to Bhatbhateni Supermarket on the right (same vertical band: 220..260)
        val merchant = extractor.extractField(Regex("(?i).*paid\\s*to.*")) { candidate ->
            candidate.trim().takeIf { it.isNotBlank() }
        }
        assertNotNull(merchant)
        assertEquals("Bhatbhateni Supermarket", merchant?.first)
    }

    @Test
    fun extractFieldFromStackedLayoutDirectlyBelow() {
        val block = OcrBlock(
            text = "Total Amount\nRs. 1,200.00",
            lines = listOf(
                OcrLine(text = "Total Amount", boundingBox = BoundingBox(left = 100, top = 50, right = 300, bottom = 90)),
                OcrLine(text = "Rs. 1,200.00", boundingBox = BoundingBox(left = 110, top = 100, right = 320, bottom = 140))
            )
        )

        val ocr = OcrResult(fullText = block.text, blocks = listOf(block))
        val extractor = SpatialLayoutExtractor(ocr)

        val amount = extractor.extractField(Regex("(?i).*total\\s*amount.*")) { candidate ->
            Normalizer.parsePaisa(Normalizer.toAsciiDigits(candidate))
        }
        assertNotNull(amount)
        assertEquals(120000L, amount?.first)
    }

    @Test
    fun fallbackToSequentialWhenBoundingBoxesMissing() {
        val plainText = """
            eSewa
            Total Amount
            Rs. 850.00
            Transaction ID
            TXN12345678
        """.trimIndent()

        val ocr = OcrResult(fullText = plainText, blocks = emptyList())
        val extractor = SpatialLayoutExtractor(ocr)

        val amount = extractor.extractField(Regex("(?i).*total\\s*amount.*")) { candidate ->
            Normalizer.parsePaisa(Normalizer.toAsciiDigits(candidate))
        }
        assertNotNull(amount)
        assertEquals(85000L, amount?.first)

        val txn = extractor.extractField(Regex("(?i).*transaction\\s*id.*")) { candidate ->
            candidate.trim().takeIf { it.startsWith("TXN") }
        }
        assertNotNull(txn)
        assertEquals("TXN12345678", txn?.first)
    }
}
