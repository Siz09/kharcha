package com.sijan.kharcha.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import com.sijan.kharcha.domain.parser.BankSmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankSmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var bankSmsParser: BankSmsParser

    @Inject
    lateinit var bankSmsNotifier: BankSmsNotifier

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages.firstOrNull()?.displayOriginatingAddress.orEmpty()
        val fullBody = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }

        if (sender.isBlank() || fullBody.isBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = userPreferencesRepository.preferences.first()
                if (prefs.smsDetectionEnabled) {
                    val txn = bankSmsParser.parse(sender, fullBody)
                    if (txn != null) {
                        bankSmsNotifier.showTransactionNotification(txn)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
