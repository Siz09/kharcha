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

## 7. v1.1

- **Home Header & Status Strip**: App title branding with live date (Gregorian + BS), daily burn rate indicator, and backup status.
- **Monthly Saving Index**: Savings rate percentage `((Income - Expense) / Income) * 100%` with health status indicator.
- **Rollover / Carryover Balance**: Option to roll previous month's surplus/deficit into opening balance of the next month.
- **Cash Flow Calendar**: Monthly calendar grid with color-coded day markers (income/expense) and date filter drill-down.
- **Bills & Subscriptions Tracker**: Offline recurring bill manager (ISP, NEA electricity, Khanepani, rent) with local WorkManager due-date reminders and one-tap payment recording.
- **Bikram Sambat (BS) Dates**: Dual BS/AD date conversion, display, and Nepali calendar date picker.
- **Per-Category Budgets**: Monthly budget limits per category with local threshold warning notifications.
- **Recurring Entries**: Scheduled automated templates for regular income or recurring expenses.
- **Audit Log Table**: Dedicated local `AuditLog` table tracking entity edits and deletions.
- **Fixture Corpus Expansion**: Growing parser test suite derived from user corrections.

## 8. v2

- **Flexible ORM & Layout-Aware Extraction Engine**: Spatial anchor-matching engine to parse and remember varied statement/receipt layouts for digital wallets (eSewa, Khalti) and bank slips (Fonepay, commercial banks).
- **Payment Source Separation**: Categorize transactions by payment channel (`Cash in Wallet`, `eSewa`, `Khalti`, `Bank Account`) to track physical vs digital balances.
- **On-Device Bank SMS Expense Detection**: Optional local `RECEIVE_SMS` receiver (zero internet) parsing debit SMS from Nepali banks and prompting user confirmation.
- **Spending Reports & Deep Analytics**: Category breakdown charts, month-over-month trend analysis, day-of-week spend patterns, and printable offline PDF monthly statements.
- **Udhar (Credit/Debt) Ledger**: Track borrowed and lent money with contact linking, due dates, and partial settlement logs.
- **Savings Goals Tracker**: Goal buckets (e.g. "Dashain Trip", "Emergency Fund") with target amount and progress visualizer.
- **Utility Account Number Vault**: Offline storage for NEA customer IDs, Khanepani meter numbers, and ISP account IDs.
- **Tax Deductible Tagging**: Tag expenses qualifying for Nepali tax rebates (insurance, medical, education).
- **Launcher Widgets & Shortcuts**: Home screen quick-add widget and Android App Shortcuts.
- **Biometric Security Lock**: Fingerprint and Face Unlock via Android `BiometricPrompt`.

## 9. Data accuracy requirements

- Money is stored as an integer number of paisa. No floating point.
- No scanned entry is saved without the user confirming it.
- A failed validation check blocks one-tap confirm and highlights the field.
- Duplicate transaction IDs warn the user. They are never merged automatically.

## 10. Success metrics

- Screenshot scan field accuracy: 99% target on the fixture corpus.
- Camera scan field accuracy: 95% target.
- Zero silent wrong amounts in daily use.
- Restore from backup works on a clean install, checked by an automated test.
- Time from share to saved entry: under 10 seconds on a clean parse.

## 11. Risks

| Risk | Mitigation |
|------|------------|
| eSewa changes receipt layout | One parser class per format, fixture tests on every commit, raw OCR stored for re-parse |
| Scope creep | v1 list is frozen. New ideas go to v1.1 or v2 |
| Data loss again | Backup ships before OCR. Git from day one |
| Wrong Bikram Sambat dates | Own lookup table with unit tests against known dates |

## 12. Open questions

- Exact eSewa receipt fields and layout (needs 5 redacted text samples).
- Whether receipts contain Devanagari text.
- Final package name (placeholder: `com.sijan.kharcha`).
