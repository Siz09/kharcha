package com.sijan.kharcha.data.backup

import androidx.room.withTransaction
import com.sijan.kharcha.data.backup.model.BackupCategory
import com.sijan.kharcha.data.backup.model.BackupEntry
import com.sijan.kharcha.data.backup.model.BackupPayload
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupServiceImpl @Inject constructor(
    private val db: AppDatabase,
    private val categoryDao: CategoryDao,
    private val ledgerDao: LedgerDao,
    private val serializer: JsonBackupSerializer
) : BackupService {

    override suspend fun exportBackup(outputStream: OutputStream): Result<Int> = runCatching {
        val categories = categoryDao.getAllCategories().first().map {
            BackupCategory(
                id = it.id,
                name = it.name,
                type = it.type
            )
        }

        val entries = ledgerDao.getAllEntries().first().map {
            BackupEntry(
                id = it.id,
                amountPaisa = it.amountPaisa,
                type = it.type,
                categoryId = it.categoryId,
                note = it.note,
                date = it.date,
                source = it.source,
                receiptPath = it.receiptPath,
                rawOcrText = it.rawOcrText,
                transactionId = it.transactionId,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val payload = BackupPayload(
            schemaVersion = JsonBackupSerializer.CURRENT_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            categories = categories,
            entries = entries
        )

        serializer.serialize(payload, outputStream).getOrThrow()
        entries.size
    }

    override suspend fun restoreBackup(inputStream: InputStream): Result<BackupSummary> = runCatching {
        // Step 1: Deserialize and validate payload before modifying database
        val payload = serializer.deserialize(inputStream).getOrThrow()

        // Step 2: Atomic transaction write
        db.withTransaction {
            ledgerDao.deleteAllEntries()
            categoryDao.deleteAllCategories()

            for (category in payload.categories) {
                categoryDao.insertCategory(
                    CategoryEntity(
                        id = category.id,
                        name = category.name,
                        type = category.type
                    )
                )
            }

            for (entry in payload.entries) {
                ledgerDao.insertEntry(
                    LedgerEntryEntity(
                        id = entry.id,
                        amountPaisa = entry.amountPaisa,
                        type = entry.type,
                        categoryId = entry.categoryId,
                        note = entry.note,
                        date = entry.date,
                        source = entry.source,
                        receiptPath = entry.receiptPath,
                        rawOcrText = entry.rawOcrText,
                        transactionId = entry.transactionId,
                        createdAt = entry.createdAt,
                        updatedAt = entry.updatedAt
                    )
                )
            }
        }

        BackupSummary(
            schemaVersion = payload.schemaVersion,
            exportedAt = payload.exportedAt,
            categoryCount = payload.categories.size,
            entryCount = payload.entries.size
        )
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
}
