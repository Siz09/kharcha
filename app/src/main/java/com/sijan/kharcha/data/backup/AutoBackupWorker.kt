package com.sijan.kharcha.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupService: BackupService,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = userPreferencesRepository.preferences.first()
        if (!prefs.autoBackupEnabled) {
            return Result.success()
        }

        val treeUriStr = prefs.autoBackupFolderUri ?: return Result.failure()
        val treeUri = Uri.parse(treeUriStr)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "kharcha_backup_$dateStr.kharcha"

        return try {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            val fileDocUri = DocumentsContract.createDocument(
                appContext.contentResolver,
                parentDocUri,
                "application/zip",
                fileName
            ) ?: return Result.failure()

            val writeResult = runCatching {
                appContext.contentResolver.openOutputStream(fileDocUri)?.use { output ->
                    backupService.exportArchive(output).getOrThrow()
                } ?: throw IllegalStateException("Could not open output stream")
            }

            if (writeResult.isFailure) {
                DocumentsContract.deleteDocument(appContext.contentResolver, fileDocUri)
                return Result.retry()
            }

            pruneOldBackups(appContext.contentResolver, treeUri, MAX_BACKUP_ARCHIVES)
            userPreferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "kharcha_auto_backup"
        const val LEGACY_WORK_NAME = "kharcha_daily_auto_backup"
        const val MAX_BACKUP_ARCHIVES = 5

        fun pruneOldBackups(
            contentResolver: ContentResolver,
            treeUri: Uri,
            maxBackups: Int = MAX_BACKUP_ARCHIVES
        ) {
            try {
                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )

                val backups = mutableListOf<Triple<String, String, Long>>()

                contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val modifiedIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIdx)
                        val name = cursor.getString(nameIdx) ?: ""
                        val modified = cursor.getLong(modifiedIdx)

                        if (name.startsWith("kharcha_backup_") && (name.endsWith(".kharcha") || name.endsWith(".json"))) {
                            backups.add(Triple(id, name, modified))
                        }
                    }
                }

                if (backups.size > maxBackups) {
                    val toDelete = backups.sortedByDescending { it.third }.drop(maxBackups)
                    for ((id, _, _) in toDelete) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        DocumentsContract.deleteDocument(contentResolver, docUri)
                    }
                }
            } catch (ignored: Exception) {
                // Best effort prune
            }
        }
    }
}
