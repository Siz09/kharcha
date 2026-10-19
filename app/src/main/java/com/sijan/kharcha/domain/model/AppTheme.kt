package com.sijan.kharcha.domain.model

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val lastBackupTimestamp: Long? = null,
    val rolloverEnabled: Boolean = false,
    val smsDetectionEnabled: Boolean = true
)
