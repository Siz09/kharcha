package com.sijan.kharcha.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sijan.kharcha.MainActivity
import com.sijan.kharcha.R
import com.sijan.kharcha.data.preferences.UserPreferencesRepository
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.Money
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

class KharchaQuickBalanceWidget : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun ledgerRepository(): LedgerRepository
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
                val ledgerRepo = entryPoint.ledgerRepository()
                val prefsRepo = entryPoint.userPreferencesRepository()

                val prefs = prefsRepo.preferences.first()
                val now = LocalDate.now()
                val monthlySummary = ledgerRepo.getMonthlySummary(now.year, now.monthValue).first()
                val netBalancePaisa = monthlySummary.balancePaisa

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_quick_balance)

                    val balanceText = if (prefs.biometricLockEnabled) {
                        context.getString(R.string.widget_locked_placeholder)
                    } else {
                        formatPaisa(netBalancePaisa)
                    }

                    views.setTextViewText(R.id.widget_balance, balanceText)

                    // Root click -> Open MainActivity
                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openAppPendingIntent = PendingIntent.getActivity(
                        context, 0, openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                    // Add Expense button click
                    val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("shortcut_action", "action_add_expense")
                    }
                    val addExpensePendingIntent = PendingIntent.getActivity(
                        context, 1, addExpenseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_add, addExpensePendingIntent)

                    // Scan button click
                    val scanIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("shortcut_action", "action_scan")
                    }
                    val scanPendingIntent = PendingIntent.getActivity(
                        context, 2, scanIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_scan, scanPendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (ignored: Exception) {
                // Ignore widget background update errors safely
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun formatPaisa(paisa: Long): String {
            val isNegative = paisa < 0
            val absPaisa = if (isNegative) -paisa else paisa
            val formatted = Money.formatPaisa(absPaisa)
            return if (isNegative) "-$formatted" else formatted
        }
    }
}
