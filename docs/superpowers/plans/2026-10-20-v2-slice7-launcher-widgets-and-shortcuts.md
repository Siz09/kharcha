# Kharcha v2 Slice 7: Launcher Widgets & App Shortcuts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Android App Shortcuts ("Scan Receipt", "Add Expense", "Add Income") and an Impeccable home screen Quick Balance Widget with one-tap entry actions, reactive updates, and Biometric Lock privacy protection.

**Architecture:** Static shortcuts in `res/xml/shortcuts.xml` launch `MainActivity` with intent action extras that `KharchaNavHost` routes directly to the requested destination (Scan, Add Expense, Add Income). An `AppWidgetProvider` (`KharchaQuickBalanceWidget`) displays the user's monthly net balance in Paisa (or `Rs. ••••••` if Biometric Lock is active) with quick-action PendingIntents.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose, Android AppWidgetProvider, RemoteViews, Storage / Repository, Hilt.

**Spec:** [docs/superpowers/specs/2026-10-20-v2-slice7-launcher-widgets-and-shortcuts-design.md](file:///D:/vscode/kharcha/docs/superpowers/specs/2026-10-20-v2-slice7-launcher-widgets-and-shortcuts-design.md)

## Global Constraints

- 100% offline: `android.permission.INTERNET` forbidden.
- Zero floating point for money: all balances calculated as `Long` in paisa.
- All strings in `res/values/strings.xml` (no hard-coded strings in UI or XML).
- Follow Impeccable design: high-contrast typography, clear hierarchy, 48dp minimum touch targets on widget action buttons.
- Biometric Privacy: if `biometricLockEnabled == true`, widget obscures balance with `Rs. ••••••`.
- Always prefix commands with `rtk`.

---

### Task 1: String Resources & Drawable Vectors for Shortcuts & Widget

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_shortcut_scan.xml`
- Create: `app/src/main/res/drawable/ic_shortcut_expense.xml`
- Create: `app/src/main/res/drawable/ic_shortcut_income.xml`
- Create: `app/src/main/res/drawable/bg_widget.xml`
- Create: `app/src/main/res/drawable/bg_widget_button.xml`

**Interfaces:**
- Produces: Vector icons and localized labels for shortcuts and widget.

- [ ] **Step 1: Add strings to strings.xml**

Add to `app/src/main/res/values/strings.xml`:
```xml
    <!-- Shortcuts & Widget -->
    <string name="shortcut_scan_short">Scan Receipt</string>
    <string name="shortcut_scan_long">Scan eSewa or Paper Receipt</string>
    <string name="shortcut_add_expense_short">Add Expense</string>
    <string name="shortcut_add_expense_long">Record New Expense Entry</string>
    <string name="shortcut_add_income_short">Add Income</string>
    <string name="shortcut_add_income_long">Record New Income Entry</string>
    <string name="widget_title">Kharcha Quick Balance</string>
    <string name="widget_description">Glance current balance and launch quick entry</string>
    <string name="widget_tap_to_view">Tap to open Kharcha</string>
    <string name="widget_locked_placeholder">Rs. ••••••</string>
    <string name="widget_this_month">This Month Net</string>
```

- [ ] **Step 2: Create vector drawables and background shapes**

Create `app/src/main/res/drawable/ic_shortcut_scan.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#1B5E20"
        android:pathData="M19,3H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3zM19,19H5V5h14V19zM12,6c-3.31,0 -6,2.69 -6,6s2.69,6 6,6 6,-2.69 6,-6 -2.69,-6 -6,-6zM12,16c-2.21,0 -4,-1.79 -4,-4s1.79,-4 4,-4 4,1.79 4,4 -1.79,4 -4,4z"/>
</vector>
```

Create `app/src/main/res/drawable/ic_shortcut_expense.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#B71C1C"
        android:pathData="M19,13H5v-2h14V13z"/>
</vector>
```

Create `app/src/main/res/drawable/ic_shortcut_income.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#1B5E20"
        android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6V13z"/>
</vector>
```

Create `app/src/main/res/drawable/bg_widget.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <corners android:radius="16dp" />
    <solid android:color="#F8FAF7" />
    <stroke android:width="1dp" android:color="#E0E6DD" />
</shape>
```

Create `app/src/main/res/drawable/bg_widget_button.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#E8EFE5" />
    <size android:width="44dp" android:height="44dp" />
</shape>
```

- [ ] **Step 3: Run guard checks**

Run: `node scripts/guard.mjs`  
Expected: PASS

- [ ] **Step 4: Commit**

```bash
rtk git add app/src/main/res/values/strings.xml app/src/main/res/drawable/
rtk git commit -m "chore: add string resources and drawables for shortcuts and widget"
```

---

### Task 2: Static App Shortcuts XML & Manifest Integration

**Files:**
- Create: `app/src/main/res/xml/shortcuts.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: Launcher long-press shortcuts routing to `MainActivity` with action extras:
  - `shortcut_action = "action_scan"`
  - `shortcut_action = "action_add_expense"`
  - `shortcut_action = "action_add_income"`

- [ ] **Step 1: Create shortcuts.xml**

Create `app/src/main/res/xml/shortcuts.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="shortcut_scan"
        android:enabled="true"
        android:icon="@drawable/ic_shortcut_scan"
        android:shortLabel="@string/shortcut_scan_short"
        android:longLabel="@string/shortcut_scan_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.sijan.kharcha"
            android:targetClass="com.sijan.kharcha.MainActivity">
            <extra android:name="shortcut_action" android:value="action_scan" />
        </intent>
    </shortcut>
    <shortcut
        android:shortcutId="shortcut_expense"
        android:enabled="true"
        android:icon="@drawable/ic_shortcut_expense"
        android:shortLabel="@string/shortcut_add_expense_short"
        android:longLabel="@string/shortcut_add_expense_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.sijan.kharcha"
            android:targetClass="com.sijan.kharcha.MainActivity">
            <extra android:name="shortcut_action" android:value="action_add_expense" />
        </intent>
    </shortcut>
    <shortcut
        android:shortcutId="shortcut_income"
        android:enabled="true"
        android:icon="@drawable/ic_shortcut_income"
        android:shortLabel="@string/shortcut_add_income_short"
        android:longLabel="@string/shortcut_add_income_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.sijan.kharcha"
            android:targetClass="com.sijan.kharcha.MainActivity">
            <extra android:name="shortcut_action" android:value="action_add_income" />
        </intent>
    </shortcut>
</shortcuts>
```

- [ ] **Step 2: Add shortcuts metadata to AndroidManifest.xml**

In `app/src/main/AndroidManifest.xml`, inside `<activity android:name=".MainActivity">`:
```xml
        <meta-data
            android:name="android.app.shortcuts"
            android:resource="@xml/shortcuts" />
```

- [ ] **Step 3: Run guard-manifest to verify no network permissions or schema leaks**

Run: `node scripts/guard.mjs`  
Expected: PASS

- [ ] **Step 4: Commit**

```bash
rtk git add app/src/main/res/xml/shortcuts.xml app/src/main/AndroidManifest.xml
rtk git commit -m "feat: declare launcher app shortcuts in shortcuts.xml and manifest"
```

---

### Task 3: Home Screen Widget Layout & Provider XML

**Files:**
- Create: `app/src/main/res/layout/widget_quick_balance.xml`
- Create: `app/src/main/res/xml/widget_quick_balance_info.xml`

**Interfaces:**
- Produces: RemoteViews layout and AppWidgetProviderInfo configuration.

- [ ] **Step 1: Create widget_quick_balance_info.xml**

Create `app/src/main/res/xml/widget_quick_balance_info.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="80dp"
    android:targetCellWidth="3"
    android:targetCellHeight="2"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_quick_balance"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:description="@string/widget_description" />
```

- [ ] **Step 2: Create widget_quick_balance.xml RemoteViews layout**

Create `app/src/main/res/layout/widget_quick_balance.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_widget"
    android:orientation="horizontal"
    android:padding="16dp"
    android:gravity="center_vertical">

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/widget_label"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/widget_this_month"
            android:textColor="#556052"
            android:textSize="12sp"
            android:fontFamily="sans-serif-medium" />

        <TextView
            android:id="@+id/widget_balance"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Rs. 0.00"
            android:textColor="#182017"
            android:textSize="20sp"
            android:textStyle="bold"
            android:fontFamily="sans-serif"
            android:layout_marginTop="2dp" />

        <TextView
            android:id="@+id/widget_subtitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/widget_tap_to_view"
            android:textColor="#768472"
            android:textSize="11sp"
            android:layout_marginTop="2dp" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <ImageButton
            android:id="@+id/widget_btn_add"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="@drawable/bg_widget_button"
            android:src="@drawable/ic_shortcut_expense"
            android:contentDescription="@string/shortcut_add_expense_short"
            android:scaleType="centerInside" />

        <Space
            android:layout_width="8dp"
            android:layout_height="wrap_content" />

        <ImageButton
            android:id="@+id/widget_btn_scan"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="@drawable/bg_widget_button"
            android:src="@drawable/ic_shortcut_scan"
            android:contentDescription="@string/shortcut_scan_short"
            android:scaleType="centerInside" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: Commit**

```bash
rtk git add app/src/main/res/layout/widget_quick_balance.xml app/src/main/res/xml/widget_quick_balance_info.xml
rtk git commit -m "feat: create widget RemoteViews layout and appwidget-provider configuration"
```

---

### Task 4: AppWidgetProvider Implementation & Manifest Registration

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/ui/widget/KharchaQuickBalanceWidget.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/widget/KharchaWidgetUpdater.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/sijan/kharcha/ui/widget/KharchaQuickBalanceWidgetTest.kt`

**Interfaces:**
- Produces: `KharchaQuickBalanceWidget : AppWidgetProvider`, `KharchaWidgetUpdater.updateAllWidgets(context: Context)`

- [ ] **Step 1: Write unit test for widget formatting**

Create `app/src/test/java/com/sijan/kharcha/ui/widget/KharchaQuickBalanceWidgetTest.kt`:
```kotlin
package com.sijan.kharcha.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class KharchaQuickBalanceWidgetTest {

    @Test
    fun formatWidgetBalance_formatsPaisaCorrectly() {
        assertEquals("Rs. 1,500.00", KharchaQuickBalanceWidget.formatPaisa(150000L))
        assertEquals("Rs. 0.00", KharchaQuickBalanceWidget.formatPaisa(0L))
        assertEquals("-Rs. 450.50", KharchaQuickBalanceWidget.formatPaisa(-45050L))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.ui.widget.KharchaQuickBalanceWidgetTest"`  
Expected: FAIL (unresolved class)

- [ ] **Step 3: Implement KharchaQuickBalanceWidget and KharchaWidgetUpdater**

Create `app/src/main/java/com/sijan/kharcha/ui/widget/KharchaQuickBalanceWidget.kt`:
```kotlin
package com.sijan.kharcha.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
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
                val startOfMonth = now.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val endOfMonth = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

                val monthlySummary = ledgerRepo.getMonthlySummary(startOfMonth, endOfMonth).first()
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
```

Create `app/src/main/java/com/sijan/kharcha/ui/widget/KharchaWidgetUpdater.kt`:
```kotlin
package com.sijan.kharcha.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object KharchaWidgetUpdater {
    fun updateAllWidgets(context: Context) {
        val intent = Intent(context, KharchaQuickBalanceWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, KharchaQuickBalanceWidget::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
```

- [ ] **Step 4: Register Widget Receiver in AndroidManifest.xml**

Add inside `<application>` in `app/src/main/AndroidManifest.xml`:
```xml
        <receiver
            android:name=".ui.widget.KharchaQuickBalanceWidget"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_quick_balance_info" />
        </receiver>
```

- [ ] **Step 5: Run unit test to verify it passes**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.ui.widget.KharchaQuickBalanceWidgetTest"`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/ui/widget/ app/src/test/java/com/sijan/kharcha/ui/widget/ app/src/main/AndroidManifest.xml
rtk git commit -m "feat: implement KharchaQuickBalanceWidget and register in manifest"
```

---

### Task 5: MainActivity Shortcut Navigation Routing & Auto-Update Trigger

**Files:**
- Modify: `app/src/main/java/com/sijan/kharcha/MainActivity.kt`
- Modify: `app/src/main/java/com/sijan/kharcha/ui/navigation/KharchaNavHost.kt`
- Modify: `app/src/main/java/com/sijan/kharcha/data/repository/LedgerRepositoryImpl.kt`

**Interfaces:**
- Consumes: `intent.getStringExtra("shortcut_action")`
- Produces: Navigation to `Screen.Scan` or `Screen.AddEditEntry(type)` on shortcut trigger; widget broadcast update on entry mutation.

- [ ] **Step 1: Update MainActivity to pass shortcut action to KharchaApp**

In `MainActivity.kt`:
Extract `val shortcutAction = intent?.getStringExtra("shortcut_action")` and pass to `KharchaApp(..., initialShortcutAction = shortcutAction)`.
Also support `onNewIntent(intent: Intent?)` to update intent when app is already running.

- [ ] **Step 2: Update KharchaApp & KharchaNavHost for shortcut routing**

In `KharchaApp.kt` & `KharchaNavHost.kt`:
When `initialShortcutAction` is:
- `"action_scan"` -> `navController.navigate(Screen.Scan.route)`
- `"action_add_expense"` -> `navController.navigate(Screen.AddEditEntry.createRoute(type = "EXPENSE"))`
- `"action_add_income"` -> `navController.navigate(Screen.AddEditEntry.createRoute(type = "INCOME"))`

- [ ] **Step 3: Call KharchaWidgetUpdater.updateAllWidgets in LedgerRepositoryImpl on writes**

In `LedgerRepositoryImpl.kt`:
Inject `@ApplicationContext private val context: Context` and call `KharchaWidgetUpdater.updateAllWidgets(context)` on `insertEntry`, `updateEntry`, `deleteEntry`, and `transfer`.

- [ ] **Step 4: Verify compilation & run unit tests**

Run: `rtk gradlew testDebugUnitTest`  
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/MainActivity.kt app/src/main/java/com/sijan/kharcha/ui/KharchaApp.kt app/src/main/java/com/sijan/kharcha/ui/navigation/ app/src/main/java/com/sijan/kharcha/data/repository/LedgerRepositoryImpl.kt
rtk git commit -m "feat: route launcher shortcut intents to destinations and update widgets on ledger writes"
```

---

### Task 6: Full Verification Gates Run

- [ ] **Step 1: Run all unit tests**
Run: `rtk gradlew testDebugUnitTest`

- [ ] **Step 2: Run Android Lint**
Run: `rtk gradlew lintDebug`

- [ ] **Step 3: Run repository verification guards**
Run: `node scripts/guard.mjs`

- [ ] **Step 4: Build debug APK**
Run: `rtk gradlew assembleDebug`
