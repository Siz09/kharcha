package com.sijan.kharcha.data.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sijan.kharcha.MainActivity
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.model.BankSmsTransaction
import com.sijan.kharcha.domain.model.Money
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankSmsNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "channel_bank_sms"
        const val EXTRA_SMS_AMOUNT_PAISA = "extra_sms_amount_paisa"
        const val EXTRA_SMS_NOTE = "extra_sms_note"
        const val EXTRA_SMS_TYPE = "extra_sms_type"
        const val EXTRA_SMS_DATE = "extra_sms_date"
        const val EXTRA_SMS_TXN_ID = "extra_sms_txn_id"
        const val EXTRA_SMS_SOURCE = "extra_sms_source"
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_sms_channel_name)
            val desc = context.getString(R.string.notification_sms_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = desc
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTransactionNotification(txn: BankSmsTransaction) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SMS_AMOUNT_PAISA, txn.amountPaisa)
            putExtra(EXTRA_SMS_NOTE, txn.merchant ?: "${txn.bankName} Transaction")
            putExtra(EXTRA_SMS_TYPE, txn.type.name)
            putExtra(EXTRA_SMS_DATE, txn.dateEpochMillis)
            txn.transactionId?.let { putExtra(EXTRA_SMS_TXN_ID, it) }
            putExtra(EXTRA_SMS_SOURCE, "sms:${txn.bankName.lowercase().replace(" ", "_")}")
        }

        val notificationId = (txn.dateEpochMillis xor txn.amountPaisa).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = Money.formatPaisa(txn.amountPaisa)
        val title = context.getString(R.string.notification_sms_title, txn.bankName)
        val bodyText = context.getString(R.string.notification_sms_body, formattedAmount)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$bodyText\n${txn.merchant ?: txn.bankName}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
