package com.sijan.kharcha.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.sijan.kharcha.data.backup.model.ArchiveAsset
import com.sijan.kharcha.data.backup.model.ArchiveManifest
import com.sijan.kharcha.data.backup.model.ArchiveSummary
import com.sijan.kharcha.data.backup.model.BackupAccount
import com.sijan.kharcha.data.backup.model.BackupCategory
import com.sijan.kharcha.data.backup.model.BackupEntry
import com.sijan.kharcha.data.backup.model.BackupPayload
import com.sijan.kharcha.data.backup.model.BackupSavingsContribution
import com.sijan.kharcha.data.backup.model.BackupSavingsGoal
import com.sijan.kharcha.data.backup.model.BackupUdhar
import com.sijan.kharcha.data.backup.model.BackupUdharSettlement
import com.sijan.kharcha.data.backup.model.BackupUtilityAccount
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.dao.AccountDao
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.dao.SavingsGoalDao
import com.sijan.kharcha.data.db.dao.UdharDao
import com.sijan.kharcha.data.db.dao.UtilityAccountDao
import com.sijan.kharcha.data.db.entity.AccountEntity
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.data.db.entity.SavingsContributionEntity
import com.sijan.kharcha.data.db.entity.SavingsGoalEntity
import com.sijan.kharcha.data.db.entity.UdharEntity
import com.sijan.kharcha.data.db.entity.UdharSettlementEntity
import com.sijan.kharcha.data.db.entity.UtilityAccountEntity
import com.sijan.kharcha.domain.model.AccountType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val categoryDao: CategoryDao,
    private val ledgerDao: LedgerDao,
    private val accountDao: AccountDao,
    private val udharDao: UdharDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val utilityAccountDao: UtilityAccountDao,
    private val serializer: JsonBackupSerializer
) : BackupService {

    private suspend fun buildBackupPayload(): BackupPayload {
        val categories = categoryDao.getAllCategories().first().map {
            BackupCategory(
                id = it.id,
                name = it.name,
                type = it.type
            )
        }

        val accounts = accountDao.getAllAccounts().first().map {
            BackupAccount(
                id = it.id,
                name = it.name,
                type = it.type,
                institutionName = it.institutionName,
                accountNumberLast4 = it.accountNumberLast4,
                openingBalancePaisa = it.openingBalancePaisa,
                isActive = it.isActive,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val entries = ledgerDao.getAllEntries().first().map {
            BackupEntry(
                id = it.id,
                amountPaisa = it.amountPaisa,
                type = it.type,
                categoryId = it.categoryId,
                accountId = it.accountId,
                toAccountId = it.toAccountId,
                note = it.note,
                date = it.date,
                source = it.source,
                receiptPath = it.receiptPath,
                rawOcrText = it.rawOcrText,
                transactionId = it.transactionId,
                isTaxDeductible = it.isTaxDeductible,
                taxCategory = it.taxCategory,
                documentType = it.documentType,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val udhars = udharDao.getAllUdhars().first().map {
            BackupUdhar(
                id = it.id,
                personName = it.personName,
                phoneNumber = it.phoneNumber,
                type = it.toDomain().type,
                amountPaisa = it.amountPaisa,
                settledPaisa = it.settledPaisa,
                dueDate = it.dueDate,
                notes = it.notes,
                status = it.toDomain().status,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val settlements = udharDao.getAllSettlements().first().map {
            BackupUdharSettlement(
                id = it.id,
                udharId = it.udharId,
                amountPaisa = it.amountPaisa,
                dateEpochMillis = it.dateEpochMillis,
                notes = it.notes,
                createdAt = it.createdAt
            )
        }

        val goals = savingsGoalDao.getAllGoals().first().map {
            BackupSavingsGoal(
                id = it.id,
                name = it.name,
                targetAmountPaisa = it.targetAmountPaisa,
                savedAmountPaisa = it.savedAmountPaisa,
                targetDateEpochMillis = it.targetDateEpochMillis,
                notes = it.notes,
                isCompleted = it.isCompleted,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val contributions = savingsGoalDao.getAllContributions().first().map {
            BackupSavingsContribution(
                id = it.id,
                goalId = it.goalId,
                amountPaisa = it.amountPaisa,
                dateEpochMillis = it.dateEpochMillis,
                notes = it.notes,
                createdAt = it.createdAt
            )
        }

        val utilityAccounts = utilityAccountDao.getAllUtilityAccounts().first().map {
            BackupUtilityAccount(
                id = it.id,
                utilityType = it.utilityType,
                providerName = it.providerName,
                consumerId = it.consumerId,
                counterOrBranch = it.counterOrBranch,
                accountHolderName = it.accountHolderName,
                notes = it.notes,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        return BackupPayload(
            schemaVersion = JsonBackupSerializer.CURRENT_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            categories = categories,
            entries = entries,
            accounts = accounts,
            udhars = udhars,
            udharSettlements = settlements,
            savingsGoals = goals,
            savingsContributions = contributions,
            utilityAccounts = utilityAccounts
        )
    }

    override suspend fun exportBackup(outputStream: OutputStream): Result<Int> = runCatching {
        val payload = buildBackupPayload()
        serializer.serialize(payload, outputStream).getOrThrow()
        payload.entries.size
    }

    override suspend fun exportArchive(
        outputStream: OutputStream,
        passphrase: CharArray?
    ): Result<ArchiveSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildBackupPayload()
            val databaseJsonBytes = serializer.serializeToBytes(payload).getOrThrow()

            val receiptsDir = File(context.filesDir, "receipts")
            val documentsDir = File(context.filesDir, "documents")

            val assetList = mutableListOf<Pair<String, File>>()
            if (receiptsDir.exists() && receiptsDir.isDirectory) {
                receiptsDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                    assetList.add("assets/receipts/${file.name}" to file)
                }
            }
            if (documentsDir.exists() && documentsDir.isDirectory) {
                documentsDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                    assetList.add("assets/documents/${file.name}" to file)
                }
            }

            val archiveAssets = assetList.map { (relPath, file) ->
                ArchiveAsset(
                    relativePath = relPath,
                    sha256 = calculateSha256(file),
                    sizeBytes = file.length()
                )
            }

            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) {
                "1.0.0"
            }

            val manifest = ArchiveManifest(
                archiveVersion = 1,
                schemaVersion = AppDatabase.SCHEMA_VERSION,
                createdAt = payload.exportedAt,
                appVersion = appVersion,
                deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim(),
                totalEntries = payload.entries.size,
                totalAccounts = payload.accounts.size,
                totalCategories = payload.categories.size,
                totalAssets = archiveAssets.size,
                assets = archiveAssets
            )

            val manifestJsonBytes = serializer.serializeManifest(manifest).toByteArray(Charsets.UTF_8)
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(manifest.createdAt))
            val defaultFileName = "kharcha_backup_$dateStr.kharcha"

            val tempZipFile = if (passphrase != null && passphrase.isNotEmpty()) {
                File(context.cacheDir, "export_temp_${System.currentTimeMillis()}.zip")
            } else null

            val targetZipStream = tempZipFile?.outputStream() ?: outputStream
            val countingOut = CountingOutputStream(targetZipStream)

            ZipOutputStream(countingOut.buffered()).use { zipOut ->
                // 1. manifest.json
                val manifestEntry = ZipEntry("manifest.json")
                zipOut.putNextEntry(manifestEntry)
                zipOut.write(manifestJsonBytes)
                zipOut.closeEntry()

                // 2. database.json
                val dbEntry = ZipEntry("database.json")
                zipOut.putNextEntry(dbEntry)
                zipOut.write(databaseJsonBytes)
                zipOut.closeEntry()

                // 3. assets
                for ((relPath, file) in assetList) {
                    val assetEntry = ZipEntry(relPath)
                    zipOut.putNextEntry(assetEntry)
                    file.inputStream().buffered().use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
                zipOut.finish()
                zipOut.flush()
            }

            var finalSize = countingOut.bytesWritten

            if (tempZipFile != null && passphrase != null && passphrase.isNotEmpty()) {
                try {
                    val encCountingOut = CountingOutputStream(outputStream)
                    tempZipFile.inputStream().buffered().use { zipIn ->
                        BackupCrypto.encrypt(zipIn, encCountingOut, passphrase)
                    }
                    finalSize = encCountingOut.bytesWritten
                } finally {
                    tempZipFile.delete()
                }
            }

            ArchiveSummary(
                fileName = defaultFileName,
                archiveVersion = manifest.archiveVersion,
                schemaVersion = manifest.schemaVersion,
                createdAt = manifest.createdAt,
                totalEntries = manifest.totalEntries,
                totalAssets = manifest.totalAssets,
                archiveSizeBytes = finalSize
            )
        }
    }

    private suspend fun applyPayloadToDatabase(
        payload: BackupPayload,
        remappedAssetPaths: Map<String, String> = emptyMap()
    ) {
        db.withTransaction {
            ledgerDao.deleteAllEntries()
            categoryDao.deleteAllCategories()
            accountDao.deleteAllAccounts()
            udharDao.deleteAllSettlements()
            udharDao.deleteAllUdhars()
            savingsGoalDao.deleteAllContributions()
            savingsGoalDao.deleteAllGoals()
            utilityAccountDao.deleteAllUtilityAccounts()

            for (category in payload.categories) {
                categoryDao.insertCategory(
                    CategoryEntity(
                        id = category.id,
                        name = category.name,
                        type = category.type
                    )
                )
            }

            if (payload.accounts.isNotEmpty()) {
                for (account in payload.accounts) {
                    accountDao.insertAccount(
                        AccountEntity(
                            id = account.id,
                            name = account.name,
                            type = account.type,
                            institutionName = account.institutionName,
                            accountNumberLast4 = account.accountNumberLast4,
                            openingBalancePaisa = account.openingBalancePaisa,
                            isActive = account.isActive,
                            createdAt = account.createdAt,
                            updatedAt = account.updatedAt
                        )
                    )
                }
            } else {
                val now = System.currentTimeMillis()
                accountDao.insertAccount(
                    AccountEntity(
                        id = 1L,
                        name = "Cash in Hand",
                        type = AccountType.CASH,
                        openingBalancePaisa = 0L,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            for (entry in payload.entries) {
                val finalReceiptPath = if (entry.receiptPath != null) {
                    val fileName = File(entry.receiptPath).name
                    remappedAssetPaths[fileName] ?: entry.receiptPath
                } else null

                ledgerDao.insertEntry(
                    LedgerEntryEntity(
                        id = entry.id,
                        amountPaisa = entry.amountPaisa,
                        type = entry.type,
                        categoryId = entry.categoryId,
                        accountId = entry.accountId,
                        toAccountId = entry.toAccountId,
                        note = entry.note,
                        date = entry.date,
                        source = entry.source,
                        receiptPath = finalReceiptPath,
                        rawOcrText = entry.rawOcrText,
                        transactionId = entry.transactionId,
                        isTaxDeductible = entry.isTaxDeductible,
                        taxCategory = entry.taxCategory,
                        documentType = entry.documentType,
                        createdAt = entry.createdAt,
                        updatedAt = entry.updatedAt
                    )
                )
            }

            for (udhar in payload.udhars) {
                udharDao.insertUdhar(
                    UdharEntity(
                        id = udhar.id,
                        personName = udhar.personName,
                        phoneNumber = udhar.phoneNumber,
                        type = udhar.type.name,
                        amountPaisa = udhar.amountPaisa,
                        settledPaisa = udhar.settledPaisa,
                        dueDate = udhar.dueDate,
                        notes = udhar.notes,
                        status = udhar.status.name,
                        createdAt = udhar.createdAt,
                        updatedAt = udhar.updatedAt
                    )
                )
            }

            for (settlement in payload.udharSettlements) {
                udharDao.insertSettlement(
                    UdharSettlementEntity(
                        id = settlement.id,
                        udharId = settlement.udharId,
                        amountPaisa = settlement.amountPaisa,
                        dateEpochMillis = settlement.dateEpochMillis,
                        notes = settlement.notes,
                        createdAt = settlement.createdAt
                    )
                )
            }

            for (goal in payload.savingsGoals) {
                savingsGoalDao.insertGoal(
                    SavingsGoalEntity(
                        id = goal.id,
                        name = goal.name,
                        targetAmountPaisa = goal.targetAmountPaisa,
                        savedAmountPaisa = goal.savedAmountPaisa,
                        targetDateEpochMillis = goal.targetDateEpochMillis,
                        notes = goal.notes,
                        isCompleted = goal.isCompleted,
                        createdAt = goal.createdAt,
                        updatedAt = goal.updatedAt
                    )
                )
            }

            for (contribution in payload.savingsContributions) {
                savingsGoalDao.insertContribution(
                    SavingsContributionEntity(
                        id = contribution.id,
                        goalId = contribution.goalId,
                        amountPaisa = contribution.amountPaisa,
                        dateEpochMillis = contribution.dateEpochMillis,
                        notes = contribution.notes,
                        createdAt = contribution.createdAt
                    )
                )
            }

            for (ua in payload.utilityAccounts) {
                utilityAccountDao.insertUtilityAccount(
                    UtilityAccountEntity(
                        id = ua.id,
                        utilityType = ua.utilityType,
                        providerName = ua.providerName,
                        consumerId = ua.consumerId,
                        counterOrBranch = ua.counterOrBranch,
                        accountHolderName = ua.accountHolderName,
                        notes = ua.notes,
                        createdAt = ua.createdAt,
                        updatedAt = ua.updatedAt
                    )
                )
            }
        }
    }

    override suspend fun restoreBackup(inputStream: InputStream): Result<BackupSummary> = runCatching {
        // Step 1: Deserialize and validate payload before modifying database
        val payload = serializer.deserialize(inputStream).getOrThrow()

        // Step 2: Atomic transaction write
        applyPayloadToDatabase(payload)

        BackupSummary(
            schemaVersion = payload.schemaVersion,
            exportedAt = payload.exportedAt,
            categoryCount = payload.categories.size,
            entryCount = payload.entries.size,
            assetCount = 0
        )
    }

    override suspend fun stageArchiveRestore(
        inputStream: InputStream,
        passphrase: CharArray?
    ): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val stagingDir = File(context.cacheDir, "restore_staging")
            if (stagingDir.exists()) {
                stagingDir.deleteRecursively()
            }
            stagingDir.mkdirs()

            val rawInputFile = File(stagingDir, "raw_incoming.bin")
            rawInputFile.outputStream().buffered().use { out ->
                inputStream.copyTo(out)
            }

            // Check if encrypted
            val headerBytes = ByteArray(16)
            rawInputFile.inputStream().use { it.read(headerBytes) }
            val isEncrypted = BackupCrypto.isEncryptedArchive(headerBytes)

            val zipFileToExtract = if (isEncrypted) {
                if (passphrase == null || passphrase.isEmpty()) {
                    stagingDir.deleteRecursively()
                    throw IllegalArgumentException("Archive is password-protected. Passphrase required.")
                }
                val decryptedZip = File(stagingDir, "decrypted.zip")
                rawInputFile.inputStream().buffered().use { encIn ->
                    decryptedZip.outputStream().buffered().use { decOut ->
                        BackupCrypto.decrypt(encIn, decOut, passphrase)
                    }
                }
                rawInputFile.delete()
                decryptedZip
            } else {
                rawInputFile
            }

            val canonicalStagingPath = stagingDir.canonicalPath

            // 1. Unpack archive to staging directory with Zip Slip protection
            ZipInputStream(zipFileToExtract.inputStream().buffered()).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val targetFile = File(stagingDir, entry.name)
                    if (!targetFile.canonicalPath.startsWith(canonicalStagingPath)) {
                        throw SecurityException("Zip Slip detected: invalid archive entry ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        targetFile.outputStream().buffered().use { out ->
                            zipIn.copyTo(out)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            zipFileToExtract.delete()

            // 2. Validate manifest.json
            val manifestFile = File(stagingDir, "manifest.json")
            if (!manifestFile.exists()) {
                stagingDir.deleteRecursively()
                throw IllegalArgumentException("Invalid archive: manifest.json not found")
            }

            val manifest = serializer.deserializeManifest(manifestFile.readText(Charsets.UTF_8)).getOrElse { error ->
                stagingDir.deleteRecursively()
                throw IllegalArgumentException("Malformed manifest.json: ${error.message}", error)
            }

            // 3. Confirm schema version compatibility
            if (manifest.schemaVersion > AppDatabase.SCHEMA_VERSION) {
                stagingDir.deleteRecursively()
                throw IllegalArgumentException(
                    "Unsupported backup schema version: ${manifest.schemaVersion}. Current database schema is ${AppDatabase.SCHEMA_VERSION}."
                )
            }

            // 4. Validate database.json
            val dbFile = File(stagingDir, "database.json")
            if (!dbFile.exists()) {
                stagingDir.deleteRecursively()
                throw IllegalArgumentException("Invalid archive: database.json not found")
            }

            val payload = dbFile.inputStream().use { stream ->
                serializer.deserialize(stream).getOrElse { error ->
                    stagingDir.deleteRecursively()
                    throw IllegalArgumentException("Malformed database.json: ${error.message}", error)
                }
            }

            // 5. Checksum verification of all assets
            for (asset in manifest.assets) {
                val assetFile = File(stagingDir, asset.relativePath)
                if (!assetFile.exists() || !assetFile.isFile) {
                    stagingDir.deleteRecursively()
                    throw IllegalStateException("Missing asset file: ${asset.relativePath}")
                }
                if (assetFile.length() != asset.sizeBytes) {
                    stagingDir.deleteRecursively()
                    throw IllegalStateException(
                        "Asset size mismatch for ${asset.relativePath}: expected ${asset.sizeBytes}, found ${assetFile.length()}"
                    )
                }
                val sha256 = calculateSha256(assetFile)
                if (!sha256.equals(asset.sha256, ignoreCase = true)) {
                    stagingDir.deleteRecursively()
                    throw IllegalStateException(
                        "Asset checksum verification failed for ${asset.relativePath}: expected ${asset.sha256}, calculated $sha256"
                    )
                }
            }

            BackupSummary(
                schemaVersion = manifest.schemaVersion,
                exportedAt = manifest.createdAt,
                categoryCount = payload.categories.size,
                entryCount = payload.entries.size,
                assetCount = manifest.totalAssets
            )
        }
    }

    override suspend fun confirmArchiveRestore(): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val stagingDir = File(context.cacheDir, "restore_staging")
            if (!stagingDir.exists()) {
                throw IllegalStateException("No staged restore directory found")
            }

            val manifestFile = File(stagingDir, "manifest.json")
            val dbFile = File(stagingDir, "database.json")
            if (!manifestFile.exists() || !dbFile.exists()) {
                stagingDir.deleteRecursively()
                throw IllegalStateException("Staged restore files missing")
            }

            val manifest = serializer.deserializeManifest(manifestFile.readText(Charsets.UTF_8)).getOrThrow()
            val payload = dbFile.inputStream().use { serializer.deserialize(it).getOrThrow() }

            val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
            val documentsDir = File(context.filesDir, "documents").apply { mkdirs() }

            // Build path remapping for restored assets
            val remappedPaths = mutableMapOf<String, String>()
            val stagedReceiptsDir = File(stagingDir, "assets/receipts")
            if (stagedReceiptsDir.exists()) {
                stagedReceiptsDir.listFiles()?.filter { it.isFile }?.forEach { srcFile ->
                    val destFile = File(receiptsDir, srcFile.name)
                    remappedPaths[srcFile.name] = destFile.absolutePath
                }
            }
            val stagedDocsDir = File(stagingDir, "assets/documents")
            if (stagedDocsDir.exists()) {
                stagedDocsDir.listFiles()?.filter { it.isFile }?.forEach { srcFile ->
                    val destFile = File(documentsDir, srcFile.name)
                    remappedPaths[srcFile.name] = destFile.absolutePath
                }
            }

            // Phase 2: Atomic database transaction
            applyPayloadToDatabase(payload, remappedPaths)

            // Phase 3: Move verified assets from staging to app-private storage
            if (stagedReceiptsDir.exists()) {
                stagedReceiptsDir.listFiles()?.filter { it.isFile }?.forEach { srcFile ->
                    val destFile = File(receiptsDir, srcFile.name)
                    srcFile.copyTo(destFile, overwrite = true)
                }
            }
            if (stagedDocsDir.exists()) {
                stagedDocsDir.listFiles()?.filter { it.isFile }?.forEach { srcFile ->
                    val destFile = File(documentsDir, srcFile.name)
                    srcFile.copyTo(destFile, overwrite = true)
                }
            }

            // Clean up staging directory
            stagingDir.deleteRecursively()

            BackupSummary(
                schemaVersion = manifest.schemaVersion,
                exportedAt = manifest.createdAt,
                categoryCount = payload.categories.size,
                entryCount = payload.entries.size,
                assetCount = manifest.totalAssets
            )
        }
    }

    override suspend fun clearRestoreStaging() = withContext(Dispatchers.IO) {
        File(context.cacheDir, "restore_staging").deleteRecursively()
        Unit
    }

    override suspend fun exportCsv(outputStream: OutputStream): Result<Int> = runCatching {
        val categoriesMap = categoryDao.getAllCategories().first().associateBy({ it.id }, { it.name })
        val entries = ledgerDao.getAllEntries().first()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("Date(UTC),Type,Category,Amount(Rs),Note,Source,TransactionId\n")

            for (entry in entries) {
                val dateStr = dateFormat.format(Date(entry.date))
                val categoryName = categoriesMap[entry.categoryId] ?: "Unknown"
                val rupees = entry.amountPaisa / 100
                val paisaRem = entry.amountPaisa % 100
                val amountStr = "$rupees.${String.format(Locale.US, "%02d", paisaRem)}"
                val escapedNote = "\"${entry.note.replace("\"", "\"\"")}\""
                val txnId = entry.transactionId ?: ""

                writer.write("$dateStr,${entry.type},$categoryName,$amountStr,$escapedNote,${entry.source},$txnId\n")
            }
            writer.flush()
        }

        entries.size
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var bytesWritten: Long = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            bytesWritten++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            bytesWritten += len
        }
    }
}
