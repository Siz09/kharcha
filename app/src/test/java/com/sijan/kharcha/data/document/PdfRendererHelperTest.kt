package com.sijan.kharcha.data.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PdfRendererHelperTest {

    @Test
    fun getPageCount_returnsZeroForNonExistentOrEmptyFile() {
        val nonExistent = File("non_existent_statement.pdf")
        assertEquals(0, PdfRendererHelper.getPageCount(nonExistent))

        val tempEmpty = File.createTempFile("empty", ".pdf").apply {
            deleteOnExit()
        }
        assertEquals(0, PdfRendererHelper.getPageCount(tempEmpty))
    }

    @Test
    fun renderPage_failsGracefullyForInvalidPdfFile() {
        val tempInvalid = File.createTempFile("invalid", ".pdf").apply {
            writeText("not-a-valid-pdf")
            deleteOnExit()
        }

        val result = PdfRendererHelper.renderPage(tempInvalid, 0)
        assertFalse(result.isSuccess)
    }
}
