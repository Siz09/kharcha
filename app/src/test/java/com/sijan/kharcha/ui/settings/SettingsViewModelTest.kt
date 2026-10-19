package com.sijan.kharcha.ui.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.backup.AutoBackupScheduler
import com.sijan.kharcha.data.backup.BackupServiceImpl
import com.sijan.kharcha.data.backup.JsonBackupSerializer
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import com.sijan.kharcha.data.preferences.UserPreferencesRepositoryImpl
import com.sijan.kharcha.domain.model.AppTheme
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var preferencesRepo: UserPreferencesRepository
    private lateinit var serializer: JsonBackupSerializer
    private lateinit var scheduler: AutoBackupScheduler
    private lateinit var backupService: BackupServiceImpl
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesRepo = UserPreferencesRepositoryImpl(context)
        serializer = JsonBackupSerializer()
        scheduler = AutoBackupScheduler(context)
        backupService = BackupServiceImpl(db, db.categoryDao(), db.ledgerDao(), serializer)
        viewModel = SettingsViewModel(
            context = context,
            userPreferencesRepository = preferencesRepo,
            backupService = backupService,
            serializer = serializer,
            autoBackupScheduler = scheduler
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun themeSelectionUpdatesPreference() = runTest(testDispatcher) {
        viewModel.setTheme(AppTheme.DARK)
        val state = viewModel.uiState.first { it.theme == AppTheme.DARK }
        assertEquals(AppTheme.DARK, state.theme)

        viewModel.setTheme(AppTheme.LIGHT)
        val lightState = viewModel.uiState.first { it.theme == AppTheme.LIGHT }
        assertEquals(AppTheme.LIGHT, lightState.theme)
    }

    @Test
    fun autoBackupPreferencesUpdate() = runTest(testDispatcher) {
        val testFolderUri = "content://com.android.externalstorage.documents/tree/primary%3ABackups"
        viewModel.setAutoBackupFolderUri(testFolderUri)

        val stateWithFolder = viewModel.uiState.first { it.autoBackupFolderUri == testFolderUri }
        assertEquals(testFolderUri, stateWithFolder.autoBackupFolderUri)

        viewModel.setAutoBackupEnabled(true)
        val stateWithEnabled = viewModel.uiState.first { it.autoBackupEnabled }
        assertTrue(stateWithEnabled.autoBackupEnabled)

        viewModel.setAutoBackupEnabled(false)
        val stateWithDisabled = viewModel.uiState.first { !it.autoBackupEnabled }
        assertFalse(stateWithDisabled.autoBackupEnabled)
    }

    @Test
    fun restoreStagingValidatesAndConfirms() = runTest(testDispatcher) {
        // Seed some data and export
        val catId = db.categoryDao().insertCategory(
            com.sijan.kharcha.data.db.entity.CategoryEntity(name = "Groceries", type = EntryType.EXPENSE)
        )
        db.ledgerDao().insertEntry(
            com.sijan.kharcha.data.db.entity.LedgerEntryEntity(
                amountPaisa = 12000L,
                type = EntryType.EXPENSE,
                categoryId = catId,
                date = System.currentTimeMillis()
            )
        )

        val output = ByteArrayOutputStream()
        backupService.exportBackup(output).getOrThrow()
        val backupBytes = output.toByteArray()

        // Clear db
        db.ledgerDao().deleteAllEntries()
        db.categoryDao().deleteAllCategories()

        var stagedReady = false
        var stageError = false
        val stageJob = viewModel.stageRestore(
            bytes = backupBytes,
            onReady = { stagedReady = true },
            onError = { stageError = true }
        )
        stageJob.join()
        testScheduler.advanceUntilIdle()

        assertTrue(stagedReady)
        assertFalse(stageError)

        val stageState = viewModel.uiState.first { it.isRestoreDialogOpen }
        assertTrue(stageState.isRestoreDialogOpen)
        assertNotNull(stageState.pendingRestoreSummary)
        assertEquals(1, stageState.pendingRestoreSummary?.categoryCount)
        assertEquals(1, stageState.pendingRestoreSummary?.entryCount)

        var restoreConfirmed = false
        var restoreError = false
        val confirmJob = viewModel.confirmRestore(
            onSuccess = { restoreConfirmed = true },
            onError = { restoreError = true }
        )
        confirmJob?.join()
        testScheduler.advanceUntilIdle()

        assertTrue(restoreConfirmed)
        assertFalse(restoreError)

        // Verify db restored
        val restoredCategories = db.categoryDao().getAllCategories().first()
        val restoredEntries = db.ledgerDao().getAllEntries().first()
        assertEquals(1, restoredCategories.size)
        assertEquals(1, restoredEntries.size)
        assertEquals("Groceries", restoredCategories[0].name)
    }

    @Test
    fun restoreCorruptDataFails() = runTest(testDispatcher) {
        val corruptBytes = "{ \"bad_json\": true }".toByteArray(Charsets.UTF_8)

        var stageError = false
        val job = viewModel.stageRestore(
            bytes = corruptBytes,
            onReady = {},
            onError = { stageError = true }
        )
        job.join()
        testScheduler.advanceUntilIdle()

        assertTrue(stageError)
        val state = viewModel.uiState.first()
        assertFalse(state.isRestoreDialogOpen)
    }
}
