package com.sijan.kharcha.data.preferences

import com.sijan.kharcha.domain.model.AppTheme
import com.sijan.kharcha.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setTheme(theme: AppTheme)
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setAutoBackupFolderUri(uri: String?)
    suspend fun setLastBackupTimestamp(timestamp: Long)
    suspend fun setRolloverEnabled(enabled: Boolean)
    suspend fun setSmsDetectionEnabled(enabled: Boolean)
    suspend fun setBiometricLockEnabled(enabled: Boolean)
}
