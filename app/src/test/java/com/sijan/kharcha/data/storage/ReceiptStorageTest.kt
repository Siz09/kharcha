package com.sijan.kharcha.data.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ReceiptStorageTest {

    private lateinit var context: Context
    private lateinit var storage: ReceiptStorage

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storage = ReceiptStorageImpl(context)
    }

    @Test
    fun saveImageSavesBytesToPrivateDirectory() = runBlocking {
        val sampleBytes = "test-image-data-receipt".toByteArray()
        val result = storage.saveImage(ByteArrayInputStream(sampleBytes))

        assertTrue(result.isSuccess)
        val path = result.getOrThrow()
        val file = File(path)

        assertTrue(file.exists())
        assertEquals(sampleBytes.size.toLong(), file.length())
        assertTrue(path.contains("receipts"))
    }

    @Test
    fun getImageUriGeneratesValidFileProviderUri() = runBlocking {
        val sampleBytes = "sample".toByteArray()
        val path = storage.saveImage(sampleBytes).getOrThrow()

        val uri = storage.getImageUri(path)
        assertNotNull(uri)
        assertEquals("content", uri.scheme)
        assertTrue(uri.authority?.contains("fileprovider") == true)
    }

    @Test
    fun deleteImageRemovesStoredFile() = runBlocking {
        val sampleBytes = "temporary".toByteArray()
        val path = storage.saveImage(sampleBytes).getOrThrow()
        val file = File(path)
        assertTrue(file.exists())

        val deleted = storage.deleteImage(path)
        assertTrue(deleted)
        assertFalse(file.exists())
    }

    @Test
    fun saveDocumentSavesPdfToPrivateDirectory() {
        runBlocking {
            val samplePdfBytes = "%PDF-1.4 mock pdf statement".toByteArray()
            val result = storage.saveDocument(samplePdfBytes, "pdf")

            assertTrue(result.isSuccess)
            val path = result.getOrThrow()
            val file = File(path)

            assertTrue(file.exists())
            assertEquals(samplePdfBytes.size.toLong(), file.length())
            assertTrue(path.contains("documents"))
            assertTrue(path.endsWith(".pdf"))

            val uri = storage.getDocumentUri(path)
            assertNotNull(uri)
            assertEquals("content", uri.scheme)
            assertTrue(uri.authority?.contains("fileprovider") == true)

            storage.deleteImage(path)
        }
    }
}
