package com.sijan.kharcha.data.bills

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleDailyBillCheck() {
        val workRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BillReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelDailyBillCheck() {
        WorkManager.getInstance(context).cancelUniqueWork(BillReminderWorker.WORK_NAME)
    }
}
