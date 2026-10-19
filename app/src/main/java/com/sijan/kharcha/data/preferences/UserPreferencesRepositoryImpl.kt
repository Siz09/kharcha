package com.sijan.kharcha.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sijan.kharcha.domain.model.AppTheme
import com.sijan.kharcha.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
    }

    override val preferences: Flow<UserPreferences> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeName = prefs[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
            val theme = try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                AppTheme.SYSTEM
            }

            val autoBackupEnabled = prefs[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false
            val autoBackupFolderUri = prefs[PreferencesKeys.AUTO_BACKUP_FOLDER_URI]
            val lastBackupTimestamp = prefs[PreferencesKeys.LAST_BACKUP_TIMESTAMP]

            UserPreferences(
                theme = theme,
                autoBackupEnabled = autoBackupEnabled,
                autoBackupFolderUri = autoBackupFolderUri,
                lastBackupTimestamp = lastBackupTimestamp
            )
        }

    override suspend fun setTheme(theme: AppTheme) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME] = theme.name
        }
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }

    override suspend fun setAutoBackupFolderUri(uri: String?) {
        context.userDataStore.edit { prefs ->
            if (uri != null) {
                prefs[PreferencesKeys.AUTO_BACKUP_FOLDER_URI] = uri
            } else {
                prefs.remove(PreferencesKeys.AUTO_BACKUP_FOLDER_URI)
            }
        }
    }

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }
}
