package com.sijan.kharcha.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.backup.AutoBackupScheduler
import com.sijan.kharcha.data.backup.AutoBackupWorker
import com.sijan.kharcha.data.backup.BackupService
import com.sijan.kharcha.data.backup.BackupSummary
import com.sijan.kharcha.data.backup.JsonBackupSerializer
import com.sijan.kharcha.data.backup.model.ArchiveSummary
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import com.sijan.kharcha.domain.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupService: BackupService,
    private val serializer: JsonBackupSerializer,
    private val autoBackupScheduler: AutoBackupScheduler
) : ViewModel() {

    private val isRestoreDialogOpen = MutableStateFlow(false)
    private val pendingRestoreSummary = MutableStateFlow<BackupSummary?>(null)
    private var pendingRestoreBytes: ByteArray? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.preferences,
        isRestoreDialogOpen,
        pendingRestoreSummary
    ) { prefs, isRestoreOpen, summary ->
        SettingsUiState(
            theme = prefs.theme,
            autoBackupEnabled = prefs.autoBackupEnabled,
            autoBackupFolderUri = prefs.autoBackupFolderUri,
            lastBackupTimestamp = prefs.lastBackupTimestamp,
            rolloverEnabled = prefs.rolloverEnabled,
            smsDetectionEnabled = prefs.smsDetectionEnabled,
            biometricLockEnabled = prefs.biometricLockEnabled,
            pendingRestoreSummary = summary,
            isRestoreDialogOpen = isRestoreOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setSmsDetectionEnabled(enabled: Boolean): Job = viewModelScope.launch {
        userPreferencesRepository.setSmsDetectionEnabled(enabled)
    }

    fun setBiometricLockEnabled(enabled: Boolean): Job = viewModelScope.launch {
        userPreferencesRepository.setBiometricLockEnabled(enabled)
    }

    fun setTheme(theme: AppTheme): Job = viewModelScope.launch {
        userPreferencesRepository.setTheme(theme)
    }

    fun setAutoBackupEnabled(enabled: Boolean): Job = viewModelScope.launch {
        userPreferencesRepository.setAutoBackupEnabled(enabled)
        if (enabled) {
            autoBackupScheduler.schedulePeriodicBackup()
        } else {
            autoBackupScheduler.cancelPeriodicBackup()
        }
    }

    fun setAutoBackupFolderUri(uri: String?): Job = viewModelScope.launch {
        userPreferencesRepository.setAutoBackupFolderUri(uri)
    }

    fun setRolloverEnabled(enabled: Boolean): Job = viewModelScope.launch {
        userPreferencesRepository.setRolloverEnabled(enabled)
    }

    fun backupNow(onResult: (Result<ArchiveSummary>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val prefs = userPreferencesRepository.preferences.first()
                    val treeUriStr = prefs.autoBackupFolderUri
                        ?: throw IllegalStateException("Backup folder not configured")
                    val treeUri = Uri.parse(treeUriStr)

                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val fileName = "kharcha_backup_$dateStr.kharcha"

                    val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                    val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
                    val fileDocUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        parentDocUri,
                        "application/zip",
                        fileName
                    ) ?: throw IllegalStateException("Failed to create backup file in destination folder")

                    val summary = runCatching {
                        context.contentResolver.openOutputStream(fileDocUri)?.use { output ->
                            backupService.exportArchive(output).getOrThrow()
                        } ?: throw IllegalStateException("Failed to open backup output stream")
                    }.getOrElse { error ->
                        DocumentsContract.deleteDocument(context.contentResolver, fileDocUri)
                        throw error
                    }

                    AutoBackupWorker.pruneOldBackups(context.contentResolver, treeUri)
                    userPreferencesRepository.setLastBackupTimestamp(System.currentTimeMillis())
                    summary
                }
            }
            onResult(result)
        }
    }

    suspend fun exportArchiveToStream(outputStream: OutputStream): Result<ArchiveSummary> = withContext(Dispatchers.IO) {
        backupService.exportArchive(outputStream)
    }

    suspend fun exportJsonToStream(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        backupService.exportBackup(outputStream)
    }

    suspend fun exportCsvToStream(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        backupService.exportCsv(outputStream)
    }

    fun stageRestore(bytes: ByteArray, onReady: () -> Unit, onError: () -> Unit): Job = viewModelScope.launch {
        val parseResult = withContext(Dispatchers.IO) {
            serializer.deserialize(ByteArrayInputStream(bytes))
        }

        parseResult.onSuccess { payload ->
            pendingRestoreBytes = bytes
            pendingRestoreSummary.value = BackupSummary(
                schemaVersion = payload.schemaVersion,
                exportedAt = payload.exportedAt,
                categoryCount = payload.categories.size,
                entryCount = payload.entries.size
            )
            isRestoreDialogOpen.value = true
            onReady()
        }.onFailure {
            onError()
        }
    }

    fun confirmRestore(onSuccess: (BackupSummary) -> Unit, onError: () -> Unit): Job? {
        val bytes = pendingRestoreBytes ?: run {
            onError()
            return null
        }

        return viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                backupService.restoreBackup(ByteArrayInputStream(bytes))
            }

            result.onSuccess { summary ->
                isRestoreDialogOpen.value = false
                pendingRestoreBytes = null
                pendingRestoreSummary.value = null
                onSuccess(summary)
            }.onFailure {
                isRestoreDialogOpen.value = false
                pendingRestoreBytes = null
                pendingRestoreSummary.value = null
                onError()
            }
        }
    }

    fun dismissRestoreDialog() {
        isRestoreDialogOpen.value = false
        pendingRestoreBytes = null
        pendingRestoreSummary.value = null
    }
}
