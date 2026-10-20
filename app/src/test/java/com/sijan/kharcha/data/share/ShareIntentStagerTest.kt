package com.sijan.kharcha.data.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class ShareIntentStagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun determineDocumentType_identifiesImageMimeTypesAndExtensions() {
        assertEquals(SharedDocumentType.IMAGE, ShareIntentStager.determineDocumentType("image/png"))
        assertEquals(SharedDocumentType.IMAGE, ShareIntentStager.determineDocumentType("image/jpeg"))
        assertEquals(SharedDocumentType.IMAGE, ShareIntentStager.determineDocumentType("image/webp"))
        assertEquals(SharedDocumentType.IMAGE, ShareIntentStager.determineDocumentType(null, "receipt.jpg"))
        assertEquals(SharedDocumentType.IMAGE, ShareIntentStager.determineDocumentType(null, "bill.png"))
    }

    @Test
    fun determineDocumentType_identifiesPdfMimeTypeAndExtension() {
        assertEquals(SharedDocumentType.PDF, ShareIntentStager.determineDocumentType("application/pdf"))
        assertEquals(SharedDocumentType.PDF, ShareIntentStager.determineDocumentType(null, "bank_statement.pdf"))
    }

    @Test
    fun determineDocumentType_identifiesDocxMimeTypeAndExtension() {
        assertEquals(
            SharedDocumentType.DOCX,
            ShareIntentStager.determineDocumentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        )
        assertEquals(SharedDocumentType.DOCX, ShareIntentStager.determineDocumentType(null, "invoice.docx"))
    }

    @Test
    fun determineDocumentType_identifiesDocMimeTypeAndExtension() {
        assertEquals(SharedDocumentType.DOC, ShareIntentStager.determineDocumentType("application/msword"))
        assertEquals(SharedDocumentType.DOC, ShareIntentStager.determineDocumentType(null, "invoice.doc"))
    }

    @Test
    fun determineDocumentType_returnsUnsupportedForUnrelatedTypes() {
        assertEquals(SharedDocumentType.UNSUPPORTED, ShareIntentStager.determineDocumentType("text/plain"))
        assertEquals(SharedDocumentType.UNSUPPORTED, ShareIntentStager.determineDocumentType("application/zip"))
        assertEquals(SharedDocumentType.UNSUPPORTED, ShareIntentStager.determineDocumentType(null, "movie.mp4"))
        assertEquals(SharedDocumentType.UNSUPPORTED, ShareIntentStager.determineDocumentType(null, null))
    }

    @Test
    fun stageFromIntent_nullOrUnrelatedActionReturnsNull() {
        assertNull(ShareIntentStager.stageFromIntent(context, null))

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://dummy/test.png")
            type = "image/png"
        }
        assertNull(ShareIntentStager.stageFromIntent(context, viewIntent))
    }

    @Test
    fun stageFromIntent_unsupportedMimeTypeReturnsNull() {
        val testFile = File(context.cacheDir, "temp.zip").apply {
            writeBytes("zip-content".toByteArray())
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(testFile))
        }

        assertNull(ShareIntentStager.stageFromIntent(context, intent))
        testFile.delete()
    }

    @Test
    fun stageFromIntent_stagesImageFromSendAction() {
        val rawSource = File(context.cacheDir, "source_receipt.jpg").apply {
            writeBytes("mock-image-bytes".toByteArray())
        }
        val sourceUri = Uri.fromFile(rawSource)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, sourceUri)
        }

        val payload = ShareIntentStager.stageFromIntent(context, intent)
        assertNotNull(payload)
        assertEquals(SharedDocumentType.IMAGE, payload!!.documentType)
        assertEquals("image/jpeg", payload.mimeType)

        val stagedFile = File(payload.localFilePath)
        assertTrue(stagedFile.exists())
        assertEquals("mock-image-bytes", stagedFile.readText())
        assertTrue(payload.localFilePath.contains("shared_staging"))

        rawSource.delete()
        stagedFile.delete()
    }

    @Test
    fun stageFromIntent_stagesPdfFromSendAction() {
        val rawSource = File(context.cacheDir, "statement.pdf").apply {
            writeBytes("mock-pdf-statement-bytes".toByteArray())
        }
        val sourceUri = Uri.fromFile(rawSource)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, sourceUri)
        }

        val payload = ShareIntentStager.stageFromIntent(context, intent)
        assertNotNull(payload)
        assertEquals(SharedDocumentType.PDF, payload!!.documentType)
        assertEquals("application/pdf", payload.mimeType)

        val stagedFile = File(payload.localFilePath)
        assertTrue(stagedFile.exists())
        assertEquals("mock-pdf-statement-bytes", stagedFile.readText())

        rawSource.delete()
        stagedFile.delete()
    }

    @Test
    fun stageFromIntent_stagesDocxFromSendMultipleAction() {
        val rawSource1 = File(context.cacheDir, "doc1.docx").apply {
            writeBytes("docx-data-1".toByteArray())
        }
        val rawSource2 = File(context.cacheDir, "doc2.docx").apply {
            writeBytes("docx-data-2".toByteArray())
        }

        val uris = arrayListOf(Uri.fromFile(rawSource1), Uri.fromFile(rawSource2))
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }

        val payload = ShareIntentStager.stageFromIntent(context, intent)
        assertNotNull(payload)
        assertEquals(SharedDocumentType.DOCX, payload!!.documentType)

        val stagedFile = File(payload.localFilePath)
        assertTrue(stagedFile.exists())
        assertEquals("docx-data-1", stagedFile.readText())

        rawSource1.delete()
        rawSource2.delete()
        stagedFile.delete()
    }

    @Test
    fun stageFromIntent_supportsClipDataFallback() {
        val rawSource = File(context.cacheDir, "clip.png").apply {
            writeBytes("clip-bytes".toByteArray())
        }
        val uri = Uri.fromFile(rawSource)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            clipData = ClipData.newRawUri("image", uri)
        }

        val payload = ShareIntentStager.stageFromIntent(context, intent)
        assertNotNull(payload)
        assertEquals(SharedDocumentType.IMAGE, payload!!.documentType)

        val stagedFile = File(payload.localFilePath)
        assertTrue(stagedFile.exists())

        rawSource.delete()
        stagedFile.delete()
    }

    @Test
    fun cleanStagingDirectory_prunesOldStagedFiles() {
        val stagingDir = File(context.cacheDir, "shared_staging").apply {
            if (!exists()) mkdirs()
        }
        val oldFile = File(stagingDir, "shared_old.jpg").apply {
            writeText("old")
            setLastModified(System.currentTimeMillis() - 7200_000L) // 2 hours old
        }
        val newFile = File(stagingDir, "shared_new.jpg").apply {
            writeText("new")
            setLastModified(System.currentTimeMillis())
        }

        ShareIntentStager.cleanStagingDirectory(context)

        assertFalse(oldFile.exists())
        assertTrue(newFile.exists())

        newFile.delete()
    }
}
