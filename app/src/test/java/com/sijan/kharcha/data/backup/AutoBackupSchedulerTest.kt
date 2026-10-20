package com.sijan.kharcha.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoBackupSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: AutoBackupScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        scheduler = AutoBackupScheduler(context)
    }

    @Test
    fun constantsMatchV3Specification() {
        assertEquals("kharcha_auto_backup", AutoBackupWorker.WORK_NAME)
        assertEquals("kharcha_daily_auto_backup", AutoBackupWorker.LEGACY_WORK_NAME)
        assertEquals(5, AutoBackupWorker.MAX_BACKUP_ARCHIVES)
    }

    @Test
    fun scheduleAndCancelWorkDoesNotThrow() {
        scheduler.schedulePeriodicBackup()
        scheduler.cancelPeriodicBackup()
    }
}
