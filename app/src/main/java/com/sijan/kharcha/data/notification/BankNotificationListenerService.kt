package com.sijan.kharcha.data.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import com.sijan.kharcha.data.sms.BankSmsNotifier
import com.sijan.kharcha.domain.parser.BankSmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var bankSmsParser: BankSmsParser

    @Inject
    lateinit var bankSmsNotifier: BankSmsNotifier

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val sbnNonNull = sbn ?: return

        // Ignore notifications posted by Kharcha itself
        if (sbnNonNull.packageName == packageName) return

        val notification = sbnNonNull.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val combinedBody = listOf(text, bigText, subText)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")

        if (combinedBody.isBlank() && title.isBlank()) return

        serviceScope.launch {
            val prefs = userPreferencesRepository.preferences.first()
            if (!prefs.smsDetectionEnabled) return@launch

            // Fast keyword filter
            val fullContent = "$title $combinedBody"
            val hasKeywords = fullContent.contains("debited", ignoreCase = true) ||
                    fullContent.contains("credited", ignoreCase = true) ||
                    fullContent.contains("NPR", ignoreCase = true) ||
                    fullContent.contains("Rs.", ignoreCase = true) ||
                    fullContent.contains("रु", ignoreCase = true) ||
                    fullContent.contains("paid", ignoreCase = true) ||
                    fullContent.contains("spent", ignoreCase = true) ||
                    fullContent.contains("transferred", ignoreCase = true)

            if (!hasKeywords) return@launch

            val parsed = bankSmsParser.parse(
                sender = title.ifBlank { sbnNonNull.packageName },
                messageBody = combinedBody.ifBlank { title }
            ) ?: return@launch

            bankSmsNotifier.showTransactionNotification(parsed)
        }
    }
}
