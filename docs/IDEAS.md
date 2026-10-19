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

---

## v3 Roadmap: Intelligence, Reconciliation & Data Trust

Positioning: v3 transforms Kharcha from an expense tracker into a reliable, self-auditing financial ledger. It answers three questions:
1. Is my recorded financial data accurate?
2. Where is my money actually going?
3. What should I pay attention to next?

### Critical Architectural Prerequisite (Implement in v2)
- **Account Entity vs Payment Source String**: In v2, model an explicit `AccountEntity` (`id`, `name`, `type: CASH | WALLET | BANK`, `institutionName`, `openingBalancePaisa`, `isActive`) and link `LedgerEntry.accountId`.
- **Why**: Internal transfers (e.g. Bank ATM -> Cash, or Bank -> eSewa wallet top-up) and reconciliation require account instances with balances, not just loose string tags. Designing this in v2 prevents destructive schema migrations in v3.

### 1. Core v3 Pillars (High Priority)
- **Account Reconciliation**:
  - Compare Kharcha's estimated account balance with user's actual bank/eSewa/cash balance.
  - Set opening balances and record verified actual balances as of specific dates.
  - Show discrepancies and pinpoint missing or duplicate entries.
- **Offline Statement Import & Matching**:
  - Import user-provided local CSV statements from Nepali banks and digital wallets.
  - Reusable offline column mapping profiles.
  - Interactive preview: match against existing ledger entries by date, amount, and reference ID.
  - Highlight unmatched statement lines and unmatched ledger entries; user confirms import.
- **Split Transactions**:
  - Divide one total payment among multiple categories (e.g. Rs. 3,000 mart bill: Rs. 1,800 Grocery + Rs. 700 Household + Rs. 500 Personal).
  - Validation invariant: `sum(splits.amountPaisa) == parent.amountPaisa` exactly.
  - Single receipt image retained; category summaries compute splits without double-counting parent.
- **Explainable Spending Insights**:
  - 100% deterministic local math (e.g. "Transport spending +28% vs last month", "Average weekend spending Rs. 1,200 higher").
  - Transparent comparison baselines with zero opaque AI scoring.
- **Personal Categorization Rules**:
  - User-configurable text matching rules (e.g. merchant contains "WorldLink" -> Internet, note contains "fuel" -> Transport).
  - Explicit rule priority, user editable/removable.
  - Applied as suggestions during entry creation; never silently mutates existing ledger entries.
- **Encrypted & Verifiable Backups**:
  - Passphrase-protected backup archives using AES-GCM with PBKDF2/Argon2 key derivation.
  - SHA-256 integrity check and dry-run verification mode (verifies restore without overwriting local DB).
  - Clear offline warning: lost passphrase cannot be recovered (no cloud server).

### 2. Nepal-Specific Planning & Additional Candidates
- **Festival & Occasion Planning**:
  - Seasonal budgeting for Dashain, Tihar, weddings, home travel, and school admissions.
  - Dedicated expense allocation bucket with planned vs actual spending comparison.
- **Internal Account Transfers**:
  - Move funds between accounts (e.g. Bank to Cash or Bank to eSewa) without distorting income/expense totals.
- **Data Quality Center**:
  - Dedicated health screen auditing uncategorized transactions, missing receipt files, potential duplicates, and unusual spikes.
- **Nepali Language UI**:
  - Complete Devanagari translation of all interface strings with runtime language toggle.
  - English/Nepali language switcher with layout verification.
- **Privacy Mode**:
  - One-tap masking of monetary balances and Android recent-apps preview obscuring (`FLAG_SECURE`).

### 3. Excluded Anti-Patterns (Preserving Invariants)
- No cloud sync or mandatory user accounts (strictly 100% offline).
- No direct bank scraping or network integrations.
- No black-box AI financial advisors.
- No automatic silent saves from statements or SMS without user confirmation.
- No multi-currency accounting.
- No gamified health scores or dashboard chart clutter.

### 4. Release Sequencing
- **v3.0 — Accuracy & Reconciliation**: Account model, internal transfers, CSV statement import, reconciliation, data quality center.
- **v3.1 — Smarter Everyday Tracking**: Personal categorization rules, split transactions, explainable insights, anomaly alerts.
- **v3.2 — Privacy & Advanced Planning**: Encrypted backups, festival planning, Nepali localization, privacy mode.

