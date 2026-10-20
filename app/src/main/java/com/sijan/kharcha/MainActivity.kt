package com.sijan.kharcha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import com.sijan.kharcha.data.security.BiometricAuthManager
import com.sijan.kharcha.data.security.BiometricAuthResult
import com.sijan.kharcha.domain.model.AppTheme
import com.sijan.kharcha.domain.model.UserPreferences
import com.sijan.kharcha.ui.KharchaApp
import com.sijan.kharcha.ui.security.LockScreenOverlay
import com.sijan.kharcha.ui.theme.KharchaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private var isAppUnlocked by mutableStateOf(false)
    private var authErrorMessage by mutableStateOf<String?>(null)
    private var isBiometricLockConfigured by mutableStateOf(false)
    private var shortcutAction by mutableStateOf<String?>(null)
    private var sharedPayload by mutableStateOf<com.sijan.kharcha.data.share.StagedSharePayload?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        shortcutAction = intent.getStringExtra("shortcut_action")

        val payload = com.sijan.kharcha.data.share.ShareIntentStager.stageFromIntent(this, intent)
        if (payload != null) {
            sharedPayload = payload
        } else if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val docType = com.sijan.kharcha.data.share.ShareIntentStager.determineDocumentType(intent.type)
            if (docType == com.sijan.kharcha.data.share.SharedDocumentType.UNSUPPORTED) {
                android.widget.Toast.makeText(this, R.string.share_unsupported_type, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            if (isBiometricLockConfigured) {
                isAppUnlocked = false
                authErrorMessage = null
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            if (isBiometricLockConfigured && !isAppUnlocked) {
                requestUnlock()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)

        handleIncomingIntent(intent)
        com.sijan.kharcha.data.share.ShareIntentStager.cleanStagingDirectory(this)

        val initialSmsTransaction = if (intent?.hasExtra(com.sijan.kharcha.data.sms.BankSmsNotifier.EXTRA_SMS_AMOUNT_PAISA) == true) {
            val amount = intent.getLongExtra(com.sijan.kharcha.data.sms.BankSmsNotifier.EXTRA_SMS_AMOUNT_PAISA, 0L)
            if (amount > 0L) {
                com.sijan.kharcha.domain.model.SmsPrefillData(
                    amountPaisa = amount,
                    note = intent.getStringExtra(com.sijan.kharcha.data.sms.BankSmsNotifier.EXTRA_SMS_NOTE).orEmpty(),
                    type = intent.getStringExtra(com.sijan.kharcha.data.sms.BankSmsNotifier.EXTRA_SMS_TYPE) ?: "EXPENSE",
                    date = intent.getLongExtra(com.sijan.kharcha.data.sms.BankSmsNotifier.EXTRA_SMS_DATE, System.currentTimeMillis()),
                    transactionId = intent.getStringExtra(com.sijan.kharcha.data.sms.BankSmsNotifier.EXTRA_SMS_TXN_ID),
                    source = intent.getStringExtra(com.sijan.kharcha.data.sms.BankSmsNotifier.EXTRA_SMS_SOURCE) ?: "sms:bank"
                )
            } else null
        } else null

        setContent {
            val prefs by preferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = UserPreferences()
            )

            isBiometricLockConfigured = prefs.biometricLockEnabled

            LaunchedEffect(prefs.biometricLockEnabled) {
                if (prefs.biometricLockEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    if (!isAppUnlocked) {
                        requestUnlock()
                    }
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val darkTheme = when (prefs.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            KharchaTheme(darkTheme = darkTheme) {
                if (prefs.biometricLockEnabled && !isAppUnlocked) {
                    LockScreenOverlay(
                        onUnlockClick = { requestUnlock() },
                        errorMessage = authErrorMessage
                    )
                } else {
                    KharchaApp(
                        initialImageUri = sharedPayload?.stagedUri,
                        initialSharedPayload = sharedPayload,
                        initialSmsPrefill = initialSmsTransaction,
                        initialShortcutAction = shortcutAction
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
    }

    private fun requestUnlock() {
        biometricAuthManager.prompt(
            activity = this,
            title = getString(R.string.app_lock_prompt_title),
            subtitle = getString(R.string.app_lock_prompt_subtitle)
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    isAppUnlocked = true
                    authErrorMessage = null
                }
                is BiometricAuthResult.Failure -> {
                    authErrorMessage = getString(R.string.app_lock_auth_failed)
                }
                is BiometricAuthResult.Cancelled -> {
                    // Kept locked; user can tap unlock button to retry
                }
            }
        }
    }
}
