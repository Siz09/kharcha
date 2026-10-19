# Kharcha: Future Ideas & Deferred Features

This file holds features and ideas deferred from v1 scope. The v1 scope in `docs/PRD.md` is strictly frozen.
All features below adhere to Kharcha invariants: 100% offline (no `INTERNET` permission), Long paisa math, SAF local storage.

---

## Proposed UI & Flow Enhancements

### 1. Home Screen Header & Top Summary
- **App Branding**: Display "Kharcha" title and current Nepali/Gregorian date header at top.
- **Header Status Strip**: Positioned directly below app name:
  - Active month quick glance.
  - Quick burn rate indicator (e.g. "Avg Rs. 420/day").
  - Unbacked-up entries indicator or auto-backup health.

### 2. Monthly Saving Index (Savings Rate)
- Formula: `((Income - Expense) / Income) * 100%`.
- Visual badge/progress meter:
  - > 30%: Healthy (Green)
  - 10% - 30%: Moderate (Blue)
  - 0% - 10%: Slim margin (Yellow)
  - < 0%: Net deficit / Over-spending (Red)
- Monthly savings score tracking over 6-month trailing window.

### 3. Rollover / Carryover Balance
- **Rollover Toggle**: Configurable setting: "Roll surplus into next month".
- `Opening Balance` carried from previous month's closing net balance.
- Displayed on Monthly Summary Card: `Opening + Income - Expense = Net Available`.

---

## v1.1 Candidates (Core Personal Tracker Additions)

- **Bikram Sambat (BS) Calendar**: Dual BS/AD date conversion, display, and Nepali calendar date picker.
- **Cash Flow Calendar View**:
  - Monthly calendar grid with color-coded day markers (Green for income, Red for expense).
  - Tap date to view/filter day's ledger entries.
- **Bills & Subscriptions Tracker**:
  - Offline recurring bill manager (ISP, NEA electricity, Khanepani water, rent, streaming, insurance).
  - Due date alerts (via local WorkManager notifications, 0 network).
  - Mark bill as "Paid" creates corresponding ledger expense entry in one tap.
- **Per-category budgets**: Monthly budget limits per category with threshold warnings.
- **Dedicated `AuditLog` table**: Track entity edits and deletions.
- **Expanded fixture corpus**: Continuous test corpus from user scans.

---

## v2 Candidates (Advanced Intelligence & Ecosystem)

- **Spending Reports & Deep Analytics**:
  - Category breakdown donut/bar visualizer.
  - Month-over-month expenditure comparison.
  - Top merchant and highest spending days heatmaps.
  - Exportable formatted PDF monthly statement.
- **Khalti & Bank Parsers**: Receipt recognition for Khalti and Nepali commercial bank / Fonepay screenshots.
- **Udhar (Credit/Debt) Ledger**:
  - Track money lent to / borrowed from friends and family.
  - Contact picker integration (local address book).
  - Partial settlements and balance history.
- **Cash Wallet vs Digital Account Tags**:
  - Categorize payment source: `Cash in Wallet`, `eSewa`, `Bank / Card`.
  - Reconcile physical cash on hand vs digital balance.
- **On-Device Bank SMS Expense Detection**:
  - Optional `RECEIVE_SMS` permission (strictly on-device parser, zero internet).
  - Recognizes Nepali bank debit SMS (Nabil, Global IME, NIC Asia, etc.) and prompts: "Add Rs. 1,500 expense?".
- **Savings Goals Tracker**:
  - Goal buckets (e.g. "Dashain Trip", "Gadget", "Emergency Fund") with target amount and progress bar.
- **Home Widget & App Shortcuts**: Instant quick-add entry and scan from Android launcher.
- **Biometric Security Lock**: Fingerprint and Face Unlock via `BiometricPrompt`.
