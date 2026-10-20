# Kharcha: Product Requirements Document

Status: v1 scope locked. Parser rules pending real receipt samples.

## 1. Overview

Kharcha is a personal, offline Android finance tracker for Nepal. Its core feature is receipt scanning: the user shares an eSewa screenshot or photographs a paper receipt, the app reads it, and the user confirms one clean entry. All data stays on the device.

## 2. Problem

Manual expense entry is slow, so people stop tracking. Receipt apps built for other markets do not understand eSewa receipts, Nepali date conventions, or local habits such as udhar (credit).

## 3. Goals

1. Capture an expense in under 10 seconds from a screenshot.
2. Never store a wrong amount silently. Accuracy beats speed.
3. Never lose data. Backup is a first-class feature.
4. Work fully offline with no account.

## 4. Non-goals

- Cloud sync, accounts, or any backend.
- SMS parsing.
- Multi-currency.
- Bank integrations.

## 5. User

Primary user: the developer, as a daily-use tool. Secondary: friends and family on Android 8.0+ (min SDK 26). Secondary use: public portfolio project, so the README, tests, and screenshots matter.

## 6. v1 requirements

| # | Feature | Acceptance criteria |
|---|---------|---------------------|
| 1 | Add, edit, delete entry | Amount, type, category, date, note. Delete asks for confirmation. |
| 2 | Categories | Seeded defaults on first run. User can add and rename. A category in use cannot be deleted. |
| 3 | List, search, filter | Full-text search on note and merchant. Filter by date range, category, type. |
| 4 | Monthly summary | Income, expense, balance for the selected month. |
| 5 | Receipt scan | Share Target and Photo Picker for screenshots. Document Scanner for camera. eSewa parser. Validation gate. Confirm screen on every scan. Duplicate warning by transaction ID. |
| 6 | Raw data retention | Raw OCR text and source image path stored with each scanned entry. |
| 7 | Backup and restore | JSON backup with schema version to a user-picked folder. Restore validates before writing. CSV export. |
| 8 | Auto-backup | Scheduled backup through WorkManager to the chosen folder. Built last in v1. Moves to v1.1 if v1 slips. |

## 7. v1.1 (Active)

- [x] **Home Header & Status Strip**: App title branding with live date, daily burn rate indicator, and backup status.
- [x] **Monthly Saving Index**: Savings rate percentage `((Income - Expense) / Income) * 100%`, visual meter gauge, deficit explanation, and health status rating (`Healthy`, `Moderate`, `Slim`, `Deficit`).
- [x] **Rollover / Carryover Balance**: Option (`rollover_enabled` preference) to roll previous month's surplus/deficit into opening balance of the next month.
- [x] **Cash Flow Calendar**: Monthly calendar grid with color-coded day markers (income/expense) and date filter drill-down (`CashFlowCalendarScreen`).
- [x] **Bills & Subscriptions Tracker**: Offline recurring bill manager (ISP, NEA electricity, Khanepani, rent) with local WorkManager due-date reminders, one-tap payment recording, and Room schema v2 migration (`BillsScreen`).
- [x] **Bikram Sambat (BS) Dates**: Dual BS/AD date conversion table (1975–2100 BS), dual date display on Home and Entry views, and Nepali calendar date picker dialog.
- [x] **Per-Category Budgets**: Monthly budget limits per category with visual progress, remaining/exceeded indicators, and set/clear budget dialog in `SummaryScreen`.
- [x] **Recurring Entries**: Scheduled automated templates and database tables (`RecurringEntryEntity`) for recurring payments in data layer.
- [x] **Audit Log Table**: Dedicated local `AuditLog` table tracking entity edits and deletions automatically on repository writes.
- [x] **Fixture Corpus Expansion**: 5 eSewa receipt fixtures (P2P transfer, NEA electricity, mobile topup, ISP payment, retail store) with 100% field accuracy verified in unit tests.
- [x] **Expense Breakdown Donut Chart**: Monthly category expense breakdown with smooth custom Canvas Donut/Pie Chart and legend on Home screen.

## 8. v2 (Complete)

- [x] **Flexible ORM & Layout-Aware Extraction Engine**: Spatial anchor-matching engine to parse and remember varied statement/receipt layouts for digital wallets (eSewa, Khalti) and bank slips (Fonepay, commercial banks).
- [x] **Payment Source & Account Architecture**: Model explicit `AccountEntity` (Cash, eSewa, Khalti, Bank accounts) to track balances accurately and prepare for v3 reconciliation.
- [x] **On-Device Bank SMS Expense Detection**: Optional local `RECEIVE_SMS` receiver (zero internet) parsing debit SMS from Nepali banks and prompting user confirmation.
- [x] **Spending Reports & Deep Analytics**: Category breakdown charts, month-over-month trend analysis, day-of-week spend patterns, and printable offline PDF monthly statements.
- [x] **Udhar (Credit/Debt) Ledger**: Track borrowed and lent money with contact linking, due dates, and partial settlement logs.
- [x] **Savings Goals Tracker**: Goal buckets (e.g. "Dashain Trip", "Emergency Fund") with target amount and progress visualizer.
- [x] **Utility Account Number Vault**: Offline storage for NEA customer IDs, Khanepani meter numbers, and ISP account IDs with quick clipboard copy.
- [x] **Tax Deductible Tagging**: Tag expenses qualifying for Nepali tax rebates (insurance, medical, retirement) with statutory ceiling meters.
- [x] **Launcher Widgets & Shortcuts**: Home screen quick-add widget and Android App Shortcuts.
- [x] **Biometric Security Lock**: Fingerprint and Face Unlock via Android `BiometricPrompt`.

## 9. v3

The v3 roadmap prioritizes zero data loss on updates, full document and asset backup/restore, direct system-wide sharing, deep annual analytics with historical archiving, and account reconciliation.

### Slices & Execution Order

1. **APK Upgrade Data Retention & Signing Invariant**:
   - Explicit guarantee: Updating the app APK never wipes, resets, or corrupts user data.
   - Fixed `applicationId` (`com.sijan.kharcha`).
   - Persistent signing configuration for release builds with documented key management to prevent signature mismatch (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
   - Zero destructive Room migrations (`fallbackToDestructiveMigration` forbidden; explicit migrations and schema exports required).
   - App-private storage retention (`context.filesDir`) preserved by Android OS across package upgrades.

2. **System Share Sheet Integration (Direct Ingestion from Gallery & Files)**:
   - System-wide `ACTION_SEND` intent filter expansion for:
     - Images: `image/*` (PNG, JPG, WEBP screenshots and photos from Gallery or camera).
     - PDFs: `application/pdf` (official bank statements, eSewa receipts, invoices from email/files).
     - Documents: `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (DOCX) and `application/msword` (DOC).
   - Direct launch into `ConfirmScanScreen` or entry creation with staged URI, without requiring the user to open Kharcha beforehand.
   - If Biometric Lock is enabled, authenticate immediately on launch before revealing shared document contents.

3. **Document Attachments & Offline Statement Viewer (PDF & DOCX Support)**:
   - Secure app-private storage for PDF and DOCX attachments alongside receipt images.
   - On-device native PDF page preview using Android's built-in `PdfRenderer` (zero internet, zero external web view dependencies).
   - DOCX metadata and text extraction via lightweight on-device XML reader (`java.util.zip.ZipInputStream` parsing `word/document.xml`).
   - Multi-format document linking in `LedgerEntryEntity` (`receiptPath`, `documentType`).

4. **Full-Asset Backup Archive (`.kharcha` ZIP Bundle) & Weekly Auto-Backup**:
   - Comprehensive compressed `.kharcha` ZIP archive containing:
     - `manifest.json`: Schema version, archive version, creation timestamp, device info, SHA-256 hash list of all files.
     - `database.json`: Complete serialized dump of all Room tables (Accounts, Categories, Ledger Entries, Bills, Recurring, Udhar, Savings Goals, Utility Accounts, Audit Logs).
     - `assets/`: App-private receipt images, PDF statements, and DOCX invoices.
   - Target destination via Android Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`):
     - Local device or SD card storage.
     - Google Drive cloud directory (synced transparently by Android OS document provider with zero `android.permission.INTERNET`).
   - Weekly auto-backup option via WorkManager (`PeriodicWorkRequestBuilder` with 7-day interval) and user choice (Automatic Weekly vs Manual Only).
   - Automatic retention management keeping the latest 5 backup archives.

5. **Lossless, Atomic, Multi-Table & Asset Restore**:
   - Pre-flight validation: verifies archive integrity, manifest checksums, and Room schema version compatibility before touching database or storage.
   - Atomic database restore: executes inside a single `db.withTransaction` block; rolls back completely on any deserialization or foreign-key error.
   - Atomic asset unpacking: unpacks receipt images and document attachments to staging and commits only after full validation.
   - Zero data corruption guarantee: if restore fails at any point, existing user data and assets remain 100% intact.

6. **Annual Financial Analytics, YTD Summaries & Historical Archiving**:
   - Year-to-Date (YTD) and Full-Year Analytics:
     - Aggregates total earnings (income) and total spending (expenses) from the start of the year (Jan 1 / Baisakh 1 BS) to end of the year (Dec 31 / Chaitra end BS).
     - Live YTD summary reflecting financial status up to the current moment.
     - Detailed category breakdown visualizing where money came from (income categories) and where money went (expense categories).
   - Historical Data Retention & Archiving:
     - Prior years' data is **never** deleted.
     - Archived and compacted historical partitions (`annual_archive_summaries`) pre-calculate annual totals and category distributions for instant loading.
     - Historical records remain fully accessible, filterable, and searchable on-demand.

7. **Account Reconciliation, Split Transactions & Financial Planning**:
   - Account Reconciliation: Set opening balance, reconcile Kharcha records against actual bank/wallet/cash balances, and investigate discrepancies.
   - Split Transactions: Divide a single receipt or bill across multiple categories with strict validation (`sum(splits) == parent`).
   - Offline CSV Statement Import & Matching: Local CSV statement import with customizable column mappings and duplicate detection.
   - Festival & Occasion Planning: Tailored seasonal budgeting for Dashain, Tihar, travel home, and school admissions.

## 10. Data accuracy requirements

- Money is stored as an integer number of paisa. No floating point.
- No scanned entry is saved without the user confirming it.
- A failed validation check blocks one-tap confirm and highlights the field.
- Duplicate transaction IDs warn the user. They are never merged automatically.

## 11. Success metrics

- Screenshot scan field accuracy: 99% target on the fixture corpus.
- Camera scan field accuracy: 95% target.
- Zero silent wrong amounts in daily use.
- Restore from backup works on a clean install, checked by an automated test.
- Time from share to saved entry: under 10 seconds on a clean parse.

## 12. Risks

| Risk | Mitigation |
|------|------------|
| eSewa changes receipt layout | One parser class per format, fixture tests on every commit, raw OCR stored for re-parse |
| Scope creep | v1 list is frozen. New ideas go to v1.1 or v2 |
| Data loss again | Backup ships before OCR. Git from day one |
| Wrong Bikram Sambat dates | Own lookup table with unit tests against known dates |

## 13. Open questions

- Exact eSewa receipt fields and layout (needs 5 redacted text samples).
- Whether receipts contain Devanagari text.
- Final package name (placeholder: `com.sijan.kharcha`).
