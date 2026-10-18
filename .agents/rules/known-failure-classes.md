---
trigger: always_on
---

# Known Failure Classes — Kharcha

Review touched files against every class that applies before calling work complete.

## Data & Calculations
- **Never use Double or Float for money.** Store and compute as `Long` in paisa. Floating point rounding errors cause silent money discrepancies.
- **Never save 0 or negative expense without explicit validation.** If parser cannot determine amount, mark error state and block one-tap confirm.
- **Dates must be epoch milliseconds UTC.** Do not store pre-formatted local date strings in SQLite.

## Database & Persistence
- **Room migrations must be explicit.** Never use `fallbackToDestructiveMigration()`. Every version bump requires a `Migration` and migration test.
- **Room schema export must be preserved.** `exportSchema = true` is mandatory; committed JSON schema is the source of truth.
- **Transactional multi-table writes.** Operations affecting multiple tables (e.g. backup restore, deleting an entry with linked assets) must execute inside a Room transaction.

## Safety & Offline Invariants
- **Strictly offline.** Never introduce `android.permission.INTERNET`, external analytics, crash reporters with network backends, or remote API endpoints.
- **No silent error swallowing.** Never use empty `catch {}` or silent fallback defaults. Propagate or log errors with descriptive context.
- **No scanned entry saved without Confirm screen.** Even with 100% confidence, OCR parsed data must be presented on the Confirm screen for user confirmation.
- **Duplicate transaction IDs warn, never auto-merge.** Surface existing entry to user and offer "View existing" vs "Save anyway".
