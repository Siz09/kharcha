package com.sijan.kharcha.data.bills

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sijan.kharcha.R
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.Money
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone

@HiltWorker
class BillReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: LedgerRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val bills = repository.getAllBills().first()
            if (bills.isEmpty()) return Result.success()

            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)

            val dueBills = bills.filter { bill ->
                bill.isAutoReminder &&
                    !bill.isPaidForMonth(currentYear, currentMonth) &&
                    (bill.dueDayOfMonth - currentDay) in 0..2
            }

            if (dueBills.isNotEmpty()) {
                ensureNotificationChannel()
                dueBills.forEachIndexed { index, bill ->
                    showNotification(index + 1000, bill.name, bill.amountPaisa)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.bills_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(notificationId: Int, billName: String, amountPaisa: Long) {
        val title = appContext.getString(R.string.bills_notification_title)
        val formattedAmount = Money.formatPaisa(amountPaisa)
        val content = appContext.getString(R.string.bills_notification_content, billName, formattedAmount)

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (ignored: SecurityException) {
            // Notification permission might not be granted on Android 13+
        }
    }

    companion object {
        const val WORK_NAME = "kharcha_daily_bill_reminder"
        const val CHANNEL_ID = "kharcha_bill_reminders"
    }
}
