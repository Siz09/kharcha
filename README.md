# Kharcha (खर्च)

An offline, personal Android finance tracker tailored for Nepal.

Kharcha is designed for speed, accuracy, and absolute privacy. Its core capability is on-device receipt scanning (eSewa screenshots and paper receipts) with 100% offline local storage, zero accounts, zero analytics, and no backend.

---

## Key Features (v1 Scope)

- **On-Device Receipt Scanning**: Import screenshots via Android Share Target or Photo Picker, or photograph receipts with the ML Kit Document Scanner.
- **eSewa Receipt Parser**: Robust parser pipeline (`OcrResult` → `Normalizer` → `ReceiptParser` → `Validator`) with Nepali digit normalization and character look-alike heuristics.
- **Safety Confirm Gate**: Every scanned receipt must be reviewed and confirmed on the Confirm screen before saving.
- **Duplicate Detection**: Transaction IDs are cross-checked; duplicates warn the user with "View existing" vs "Save anyway" (never auto-merged).
- **Manual Add & Edit**: Clean form for manual expense and income entries with category selector, UTC date picker, optional tags, and notes.
- **Category Management**: Default Nepali expense/income categories seeded automatically; add and rename custom categories with foreign-key delete restrictions.
- **Monthly Summary**: Month navigator with total income, total expense, net balance, and spending breakdown by category with percentage bars.
- **Backup & Restore**:
  - Encrypted, versioned JSON backup/restore (Schema v1) executing inside an atomic database transaction.
  - Human-readable CSV export.
  - Storage Access Framework (SAF) folder picker for user-controlled destination.
  - Daily scheduled auto-backup via WorkManager with automatic pruning (keeps latest 5 backups).
- **Theme Support**: Material 3 Dynamic Colors with System Default, Light, and Dark modes.

---

## Technical Stack & Architecture

- **Language**: Kotlin 2.0.21 (JVM 17 target)
- **UI Framework**: Jetpack Compose, Material 3, Single Activity architecture
- **Architecture**: MVVM, Repository pattern, Reactive StateFlow, Coroutines
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Compose
- **Database**: Room 2.6.1 with KSP (`exportSchema = true`), FTS4 for full-text search
- **Preferences**: DataStore Preferences
- **OCR & Capture**: On-device ML Kit Text Recognition, Photo Picker, ML Kit Document Scanner
- **Serialization**: kotlinx.serialization
- **Background Tasks**: WorkManager
- **Min SDK**: 26 (Android 8.0+) | **Target & Compile SDK**: 35

### Package Layout
`com.sijan.kharcha`
- `data/db/` — Room database, DAOs, entities, and migration definitions
- `data/repository/` — `LedgerRepository` implementation
- `data/backup/` — JSON v1 backup/restore service, CSV export, and `AutoBackupWorker`
- `data/preferences/` — `UserPreferencesRepository` (DataStore)
- `data/storage/` — App-private receipt image storage (`ReceiptStorage`)
- `domain/model/` — Pure Kotlin domain models (`LedgerEntry`, `Category`, `Money`, `MonthlySummary`, etc.)
- `domain/parser/` — OCR models, `Normalizer`, `Validator`, `EsewaParser`, `ReceiptProcessingPipeline`
- `di/` — Hilt dependency injection modules
- `ui/theme/` — Theme, color schemes, typography
- `ui/components/` — Shared UI components (`AmountText`, `CategoryPicker`, `FieldWithStatus`, `EmptyState`, etc.)
- `ui/navigation/` — Navigation destinations and `KharchaNavHost`
- `ui/{home, entries, scan, summary, settings}/` — Feature screens and ViewModels

---

## Non-Negotiable Invariants

1. **Money is integer paisa (`Long`)**: No floating-point (`Double` or `Float`) representation for monetary calculations across data, domain, or presentation logic.
2. **Dates are epoch milliseconds UTC (`Long`)**: Formatting is isolated to the presentation layer.
3. **100% Offline**: Zero `android.permission.INTERNET`, zero analytics SDKs, zero remote logging.
4. **No Unconfirmed Writes**: Scanned OCR entries are never saved directly to disk without explicit user confirmation.
5. **Safe Migrations**: Room schema migrations must be explicit; destructive fallback migrations are strictly banned.
6. **Integrity-Preserving Restore**: Database restore validates schema and record invariants, executing entirely inside a single Room transaction. If any record fails, the entire transaction rolls back.

---

## Building and Verification

### Prerequisites
- JDK 17
- Android SDK with API 35 compile SDK and API 26+ platform tools
- Gradle 8.13 (bundled wrapper `./gradlew`)

### Running Verification Gates
Execute all repository guards, unit tests, and lint checks:
```bash
bash scripts/check.sh
```

Or execute individual tasks:
```bash
# Run unit and Robolectric tests
./gradlew testDebugUnitTest

# Run Android Lint
./gradlew lintDebug

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Release APK
./gradlew assembleRelease
```

---

## License

Personal project — All rights reserved.
