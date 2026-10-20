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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun restoreArchiveRoundtripRestoresTablesAndAssetsLosslessly() = runBlocking {
        // 1. Setup seed data
        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val receiptBytes = "sample receipt image payload for restore".toByteArray(Charsets.UTF_8)
        val receiptFile = File(receiptsDir, "receipt_restore.jpg")
        receiptFile.writeBytes(receiptBytes)

        val catId = db.categoryDao().insertCategory(CategoryEntity(id = 10L, name = "Shopping", type = EntryType.EXPENSE))
        db.accountDao().insertAccount(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, openingBalancePaisa = 50000L)
        )
        db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                id = 100L,
                amountPaisa = 320000L,
                type = EntryType.EXPENSE,
                categoryId = catId,
                accountId = 1L,
                note = "Clothes",
                date = 1718000000000L,
                receiptPath = receiptFile.absolutePath,
                documentType = DocumentType.IMAGE
            )
        )
        db.udharDao().insertUdhar(
            com.sijan.kharcha.data.db.entity.UdharEntity(
                id = 200L,
                personName = "Ramesh",
                phoneNumber = "9841000000",
                type = "GIVEN",
                amountPaisa = 100000L,
                settledPaisa = 0L,
                dueDate = null,
                notes = "Loan",
                status = "PENDING",
                createdAt = 1718000000000L,
                updatedAt = 1718000000000L
            )
        )

        // 2. Export archive
        val archiveStream = ByteArrayOutputStream()
        val exportResult = backupService.exportArchive(archiveStream)
        assertTrue(exportResult.isSuccess)
        val exportedArchiveBytes = archiveStream.toByteArray()

        // 3. Wipe current database and local files
        db.ledgerDao().deleteAllEntries()
        db.categoryDao().deleteAllCategories()
        db.accountDao().deleteAllAccounts()
        db.udharDao().deleteAllUdhars()
        receiptFile.delete()

        // 4. Phase 1: Pre-flight Stage restore
        val stageResult = backupService.stageArchiveRestore(ByteArrayInputStream(exportedArchiveBytes))
        assertTrue(stageResult.isSuccess)
        val stageSummary = stageResult.getOrThrow()
        assertEquals(1, stageSummary.entryCount)
        assertEquals(1, stageSummary.categoryCount)
        assertEquals(1, stageSummary.assetCount)

        // Verify staging directory exists before confirmation
        val stagingDir = File(context.cacheDir, "restore_staging")
        assertTrue(stagingDir.exists())

        // 5. Phase 2 & 3: Confirm restore
        val confirmResult = backupService.confirmArchiveRestore()
        assertTrue(confirmResult.isSuccess)

        // Staging directory must be cleaned up
        assertFalse(stagingDir.exists())

        // 6. Verify database records restored
        val restoredCategories = db.categoryDao().getAllCategories().first()
        assertEquals(1, restoredCategories.size)
        assertEquals("Shopping", restoredCategories[0].name)

        val restoredEntries = db.ledgerDao().getAllEntries().first()
        assertEquals(1, restoredEntries.size)
        assertEquals(320000L, restoredEntries[0].amountPaisa)
        assertEquals("Clothes", restoredEntries[0].note)

        val restoredUdhars = db.udharDao().getAllUdhars().first()
        assertEquals(1, restoredUdhars.size)
        assertEquals("Ramesh", restoredUdhars[0].personName)

        // Verify asset was copied to filesDir/receipts
        val restoredReceiptFile = File(context.filesDir, "receipts/receipt_restore.jpg")
        assertTrue(restoredReceiptFile.exists())
        assertEquals(receiptBytes.toList(), restoredReceiptFile.readBytes().toList())
    }

    @Test
    fun stageArchiveRestoreFailsOnTamperedAssetChecksumAndWipesStaging() = runBlocking {
        // Setup seed data and asset
        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        val receiptFile = File(receiptsDir, "receipt_tamper.jpg")
        receiptFile.writeBytes("authentic bytes".toByteArray(Charsets.UTF_8))

        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Health", type = EntryType.EXPENSE))
        db.accountDao().insertAccount(
            AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, openingBalancePaisa = 50000L)
        )
        db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 5000L,
                type = EntryType.EXPENSE,
                categoryId = catId,
                accountId = 1L,
                note = "Medicine",
                date = System.currentTimeMillis(),
                receiptPath = receiptFile.absolutePath,
                documentType = DocumentType.IMAGE
            )
        )

        // Export authentic archive
        val validOut = ByteArrayOutputStream()
        backupService.exportArchive(validOut).getOrThrow()
        val validBytes = validOut.toByteArray()

        // Create tampered zip archive where asset content was modified after manifest creation
        val tamperedOut = ByteArrayOutputStream()
        val zipIn = ZipInputStream(ByteArrayInputStream(validBytes))
        val zipOut = java.util.zip.ZipOutputStream(tamperedOut)

        var entry = zipIn.nextEntry
        while (entry != null) {
            zipOut.putNextEntry(java.util.zip.ZipEntry(entry.name))
            if (entry.name == "assets/receipts/receipt_tamper.jpg") {
                // Tamper bytes with different content but same size
                zipOut.write("tampered bytes!".toByteArray(Charsets.UTF_8))
            } else {
                zipIn.copyTo(zipOut)
            }
            zipOut.closeEntry()
            entry = zipIn.nextEntry
        }
        zipOut.finish()

        // Stage restore must fail with checksum mismatch
        val stageResult = backupService.stageArchiveRestore(ByteArrayInputStream(tamperedOut.toByteArray()))
        org.junit.Assert.assertFalse(stageResult.isSuccess)
        val errorMsg = stageResult.exceptionOrNull()?.message ?: ""
        assertTrue(errorMsg.contains("checksum") || errorMsg.contains("mismatch"))

        // Staging directory must be wiped
        val stagingDir = File(context.cacheDir, "restore_staging")
        assertFalse(stagingDir.exists())

        // Existing DB data must remain untouched
        val categories = db.categoryDao().getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Health", categories[0].name)
    }

    @Test
    fun stageArchiveRestoreFailsOnNewerSchemaVersion() = runBlocking {
        // Construct archive with manifest schemaVersion = 99
        val manifest = ArchiveManifest(
            archiveVersion = 1,
            schemaVersion = 99,
            createdAt = System.currentTimeMillis(),
            appVersion = "99.0.0",
            totalEntries = 0,
            totalAccounts = 0,
            totalCategories = 0,
            totalAssets = 0,
            assets = emptyList()
        )
        val manifestBytes = serializer.serializeManifest(manifest).toByteArray(Charsets.UTF_8)
        val dbBytes = serializer.serializeToBytes(
            com.sijan.kharcha.data.backup.model.BackupPayload(
                schemaVersion = 1,
                exportedAt = System.currentTimeMillis(),
                categories = emptyList(),
                entries = emptyList()
            )
        ).getOrThrow()

        val zipOutStream = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(zipOutStream).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            zip.write(manifestBytes)
            zip.closeEntry()

            zip.putNextEntry(java.util.zip.ZipEntry("database.json"))
            zip.write(dbBytes)
            zip.closeEntry()
        }

        val stageResult = backupService.stageArchiveRestore(ByteArrayInputStream(zipOutStream.toByteArray()))
        org.junit.Assert.assertFalse(stageResult.isSuccess)
        val errorMsg = stageResult.exceptionOrNull()?.message ?: ""
        assertTrue(errorMsg.contains("Unsupported backup schema version"))

        val stagingDir = File(context.cacheDir, "restore_staging")
        assertFalse(stagingDir.exists())
    }
}
