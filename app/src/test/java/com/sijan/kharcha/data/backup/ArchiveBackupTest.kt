package com.sijan.kharcha.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.backup.model.ArchiveManifest
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.entity.AccountEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.AccountType
import com.sijan.kharcha.domain.model.DocumentType
import com.sijan.kharcha.domain.model.EntryType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
class ArchiveBackupTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var serializer: JsonBackupSerializer
    private lateinit var backupService: BackupServiceImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        serializer = JsonBackupSerializer()
        backupService = BackupServiceImpl(
            context,
            db,
            db.categoryDao(),
            db.ledgerDao(),
            db.accountDao(),
            db.udharDao(),
            db.savingsGoalDao(),
            db.utilityAccountDao(),
            serializer
        )

        File(context.filesDir, "receipts").deleteRecursively()
        File(context.filesDir, "documents").deleteRecursively()
    }

    @After
    fun teardown() {
        db.close()
        File(context.filesDir, "receipts").deleteRecursively()
        File(context.filesDir, "documents").deleteRecursively()
    }

    @Test
    fun exportArchiveWithNoAssetsGeneratesValidZipWithManifestAndDatabase() = runBlocking {
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Food", type = EntryType.EXPENSE))
        db.accountDao().insertAccount(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, openingBalancePaisa = 10000L)
        )
        db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 50000L,
                type = EntryType.EXPENSE,
                categoryId = catId,
                accountId = 1L,
                note = "Groceries",
                date = System.currentTimeMillis()
            )
        )

        val output = ByteArrayOutputStream()
        val result = backupService.exportArchive(output)
        assertTrue(result.isSuccess)

        val summary = result.getOrThrow()
        assertEquals(1, summary.totalEntries)
        assertEquals(0, summary.totalAssets)
        assertTrue(summary.archiveSizeBytes > 0)
        assertTrue(summary.fileName.startsWith("kharcha_backup_"))
        assertTrue(summary.fileName.endsWith(".kharcha"))

        // Unpack zip and verify entries
        val zipIn = ZipInputStream(ByteArrayInputStream(output.toByteArray()))
        val entryNames = mutableListOf<String>()
        var manifestContent: String? = null
        var databaseContent: String? = null

        var zipEntry = zipIn.nextEntry
        while (zipEntry != null) {
            entryNames.add(zipEntry.name)
            if (zipEntry.name == "manifest.json") {
                manifestContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
            } else if (zipEntry.name == "database.json") {
                databaseContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
            }
            zipEntry = zipIn.nextEntry
        }

        assertTrue(entryNames.contains("manifest.json"))
        assertTrue(entryNames.contains("database.json"))
        assertNotNull(manifestContent)
        assertNotNull(databaseContent)

        val manifestResult = serializer.deserializeManifest(manifestContent!!)
        assertTrue(manifestResult.isSuccess)
        val manifest = manifestResult.getOrThrow()
        assertEquals(1, manifest.archiveVersion)
        assertEquals(AppDatabase.SCHEMA_VERSION, manifest.schemaVersion)
        assertEquals(1, manifest.totalEntries)
        assertEquals(0, manifest.totalAssets)
        assertTrue(manifest.assets.isEmpty())
    }

    @Test
    fun exportArchiveBundlesReceiptAndDocumentAssetsWithCorrectSha256() = runBlocking {
        // Setup receipts and documents on disk
        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val documentsDir = File(context.filesDir, "documents").apply { mkdirs() }

        val receiptBytes = "dummy receipt image content bytes 12345".toByteArray(Charsets.UTF_8)
        val receiptFile = File(receiptsDir, "receipt_test_1.jpg")
        receiptFile.writeBytes(receiptBytes)

        val docBytes = "dummy pdf statement document bytes 67890".toByteArray(Charsets.UTF_8)
        val docFile = File(documentsDir, "doc_statement.pdf")
        docFile.writeBytes(docBytes)

        val expectedReceiptSha256 = MessageDigest.getInstance("SHA-256")
            .digest(receiptBytes)
            .joinToString("") { "%02x".format(it) }

        val expectedDocSha256 = MessageDigest.getInstance("SHA-256")
            .digest(docBytes)
            .joinToString("") { "%02x".format(it) }

        // Setup db entry
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Bills", type = EntryType.EXPENSE))
        db.accountDao().insertAccount(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, openingBalancePaisa = 10000L)
        )
        db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 125000L,
                type = EntryType.EXPENSE,
                categoryId = catId,
                accountId = 1L,
                note = "Electricity",
                date = System.currentTimeMillis(),
                receiptPath = receiptFile.absolutePath,
                documentType = DocumentType.IMAGE
            )
        )

        val output = ByteArrayOutputStream()
        val result = backupService.exportArchive(output)
        assertTrue(result.isSuccess)

        val summary = result.getOrThrow()
        assertEquals(1, summary.totalEntries)
        assertEquals(2, summary.totalAssets) // 1 receipt + 1 document

        // Validate zip structure and file contents
        val zipIn = ZipInputStream(ByteArrayInputStream(output.toByteArray()))
        val entryNames = mutableListOf<String>()
        var foundManifest: String? = null
        var readReceiptBytes: ByteArray? = null
        var readDocBytes: ByteArray? = null

        var entry = zipIn.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            when (entry.name) {
                "manifest.json" -> foundManifest = zipIn.bufferedReader(Charsets.UTF_8).readText()
                "assets/receipts/receipt_test_1.jpg" -> readReceiptBytes = zipIn.readBytes()
                "assets/documents/doc_statement.pdf" -> readDocBytes = zipIn.readBytes()
            }
            entry = zipIn.nextEntry
        }

        assertTrue(entryNames.contains("manifest.json"))
        assertTrue(entryNames.contains("database.json"))
        assertTrue(entryNames.contains("assets/receipts/receipt_test_1.jpg"))
        assertTrue(entryNames.contains("assets/documents/doc_statement.pdf"))

        assertNotNull(readReceiptBytes)
        assertNotNull(readDocBytes)
        assertEquals(receiptBytes.toList(), readReceiptBytes!!.toList())
        assertEquals(docBytes.toList(), readDocBytes!!.toList())

        // Verify manifest contents and hashes
        val manifest = serializer.deserializeManifest(foundManifest!!).getOrThrow()
        assertEquals(2, manifest.totalAssets)

        val receiptAsset = manifest.assets.find { it.relativePath == "assets/receipts/receipt_test_1.jpg" }
        assertNotNull(receiptAsset)
        assertEquals(expectedReceiptSha256, receiptAsset!!.sha256)
        assertEquals(receiptBytes.size.toLong(), receiptAsset.sizeBytes)

        val docAsset = manifest.assets.find { it.relativePath == "assets/documents/doc_statement.pdf" }
        assertNotNull(docAsset)
        assertEquals(expectedDocSha256, docAsset!!.sha256)
        assertEquals(docBytes.size.toLong(), docAsset.sizeBytes)
    }
}
