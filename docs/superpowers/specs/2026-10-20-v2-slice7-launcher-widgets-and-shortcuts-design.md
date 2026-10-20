# Kharcha v2 Slice 7: Launcher Widgets & App Shortcuts Design Spec

**Date**: 2026-10-20  
**Status**: APPROVED  
**Author**: Sijan & Antigravity  
**Target Milestone**: v2.0 (Slice 7)

---

## 1. Overview & Goals

Quick capture is a primary goal of Kharcha: reducing time to record an expense to under 10 seconds. Launcher shortcuts and a home screen widget enable users to start adding an entry or scanning a receipt immediately from their Android launcher without navigating through the app manually.

### Goals
1. **Android App Shortcuts**: Static and dynamic launcher shortcuts for high-frequency actions:
   - "Scan Receipt": opens directly to Photo Picker / Document Scanner flow.
   - "Add Expense": opens `AddEditEntryScreen` prefilled with `EntryType.EXPENSE`.
   - "Add Income": opens `AddEditEntryScreen` prefilled with `EntryType.INCOME`.
2. **Glanceable Quick-Balance Widget**: Android home screen widget (`KharchaQuickBalanceWidget`) displaying:
   - Total Net Balance or Current Month Net Balance in Paisa (`Rs. #,##,###`).
   - Quick "Add Expense" (+) and "Scan Receipt" (Camera/Scan) shortcut buttons.
   - Privacy masking toggle: if Biometric Lock is enabled or if user taps privacy mask, balance shows as `Rs. ••••••`.
3. **Reactive Updates**: Widget updates automatically via `AppWidgetManager` broadcast whenever a transaction or account transfer is created, updated, or deleted.
4. **Security Alignment**: If Biometric Lock (Slice 6) is active, launching an action from a shortcut or widget presents the biometric unlock prompt before exposing transaction forms.

### Non-Goals
- Full ledger list browsing inside the launcher widget (widgets are for glanceable summary and quick launch, not complex multi-screen list interaction).
- Network syncing (strictly 100% offline).

---

## 2. Architecture & Data Flow

### 2.1 App Shortcuts (`res/xml/shortcuts.xml`)
- Declared in `AndroidManifest.xml` under `MainActivity`:
```xml
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="shortcut_scan_receipt"
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
        android:shortcutId="shortcut_add_expense"
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
        android:shortcutId="shortcut_add_income"
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

### 2.2 Home Screen Widget (`ui/widget/KharchaQuickBalanceWidget.kt`)
- Extends `AppWidgetProvider`.
- Layout XML (`res/layout/widget_quick_balance.xml`):
  - Rounded tinted background card (matching Material 3 surface palette).
  - App icon & title "Kharcha".
  - Balance amount text view with Nepali currency format (`Rs. 24,500.00`).
  - Monthly status / burn rate caption.
  - Action button 1: Quick Add (`ic_add`).
  - Action button 2: Quick Scan (`ic_scan`).
- Data Retrieval:
  - Uses `EntryPointAccessors` from Hilt to get `LedgerRepository` and `UserPreferencesRepository`.
  - Runs in `goAsync()` coroutine scope on `Dispatchers.IO`.
  - Queries current month summary `(income - expense)`.
  - If `biometricLockEnabled` is true, displays `Rs. ••••••` with a small lock icon.
- Widget Update Trigger:
  - `KharchaWidgetUpdater.updateAllWidgets(context)` called from `LedgerRepositoryImpl` on write operations.

### 2.3 Navigation Integration (`MainActivity.kt` & `KharchaNavHost.kt`)
- `MainActivity` reads `intent.getStringExtra("shortcut_action")`:
  - `"action_scan"` -> navigates to `Screen.Scan.route`.
  - `"action_add_expense"` -> navigates to `Screen.AddEditEntry.createRoute(type = "EXPENSE")`.
  - `"action_add_income"` -> navigates to `Screen.AddEditEntry.createRoute(type = "INCOME")`.
- If Biometric Lock is active, `LockScreenOverlay` intercepts before destination renders, unlocking smoothly upon authentication.

---

## 3. Impeccable Design & Animotion Guidelines

- **Widget Aesthetics**:
  - Consistent padding (16dp content padding, 16dp rounded corner radius).
  - High contrast readability: crisp dark charcoal / tinted slate background in dark mode, light stone surface in light mode.
  - Touch targets on (+) and [Scan] buttons: 48dp minimum click areas via `RemoteViews.setOnClickPendingIntent`.
- **Iconography**:
  - Clean vector assets with standard stroke weight (`2dp`).
  - Accessible content descriptions (`@string/shortcut_scan_short`, etc.).

---

## 4. String Resources (`res/values/strings.xml`)

- `shortcut_scan_short`: "Scan Receipt"
- `shortcut_scan_long`: "Scan eSewa or Paper Receipt"
- `shortcut_add_expense_short`: "Add Expense"
- `shortcut_add_expense_long`: "Record New Expense Entry"
- `shortcut_add_income_short`: "Add Income"
- `shortcut_add_income_long`: "Record New Income Entry"
- `widget_title`: "Kharcha Quick Balance"
- `widget_description`: "Glance current balance and launch quick entry"
- `widget_tap_to_view`: "Tap to open Kharcha"
- `widget_locked_placeholder`: "Rs. ••••••"

---

## 5. Testing & Verification Plan

1. **Unit Tests**:
   - `IntentActionTest`: verify intent extras correctly parse and route to expected destination.
   - `WidgetFormattingTest`: verify paisa balance properly formatted without Double/Float inaccuracies.
2. **Build & Quality Gates**:
   - `rtk gradlew testDebugUnitTest`
   - `rtk gradlew lintDebug`
   - `bash scripts/check.sh`
   - `rtk gradlew assembleDebug`
