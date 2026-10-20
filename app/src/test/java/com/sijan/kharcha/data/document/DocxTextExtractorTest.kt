package com.sijan.kharcha.data.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class DocxTextExtractorTest {

    private fun createSampleDocxBytes(paragraphs: List<String>): ByteArray {
        val xmlBuilder = StringBuilder()
        xmlBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        xmlBuilder.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")
        for (para in paragraphs) {
            xmlBuilder.append("<w:p><w:r><w:t>$para</w:t></w:r></w:p>")
        }
        xmlBuilder.append("</w:body></w:document>")

        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zip ->
            val entry = ZipEntry("word/document.xml")
            zip.putNextEntry(entry)
            zip.write(xmlBuilder.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return byteOut.toByteArray()
    }

    @Test
    fun extractText_successfullyParsesWordDocumentXmlParagraphs() {
        val paragraphs = listOf(
            "eSewa Statement Invoice",
            "Payment to Nepal Telecom",
            "Amount: Rs. 1,500.00",
            "Reference ID: TXN_DOC_778899"
        )
        val docxBytes = createSampleDocxBytes(paragraphs)

        val result = DocxTextExtractor.extractText(ByteArrayInputStream(docxBytes))
        assertTrue(result.isSuccess)

        val content = result.getOrThrow()
        assertEquals(4, content.paragraphs.size)
        assertEquals("eSewa Statement Invoice", content.paragraphs[0])
        assertEquals("Reference ID: TXN_DOC_778899", content.paragraphs[3])
        assertTrue(content.fullText.contains("Amount: Rs. 1,500.00"))
    }

    @Test
    fun extractText_failsOnZipWithoutDocumentXml() {
        val byteOut = ByteArrayOutputStream()
        ZipOutputStream(byteOut).use { zip ->
            zip.putNextEntry(ZipEntry("other/file.txt"))
            zip.write("some text".toByteArray())
            zip.closeEntry()
        }

        val result = DocxTextExtractor.extractText(ByteArrayInputStream(byteOut.toByteArray()))
        assertFalse(result.isSuccess)
    }

    @Test
    fun extractText_failsOnCorruptData() {
        val corruptBytes = "not-a-zip-file".toByteArray()
        val result = DocxTextExtractor.extractText(ByteArrayInputStream(corruptBytes))
        assertFalse(result.isSuccess)
    }
}
