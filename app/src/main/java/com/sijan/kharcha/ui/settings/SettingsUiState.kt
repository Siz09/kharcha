package com.sijan.kharcha.ui.settings

import com.sijan.kharcha.data.backup.BackupSummary
import com.sijan.kharcha.domain.model.AppTheme

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val lastBackupTimestamp: Long? = null,
    val pendingRestoreSummary: BackupSummary? = null,
    val isRestoreDialogOpen: Boolean = false,
    val rolloverEnabled: Boolean = false,
    val smsDetectionEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val appVersion: String = "1.0.0",
    val isLoading: Boolean = false
)
