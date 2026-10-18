# Kharcha Project Context & Rules

Offline personal Android finance tracker for Nepal. Its core capability is on-device receipt scanning (eSewa screenshots and paper receipts) with 100% offline local storage, zero accounts, and no backend.

## Locked Technology Stack
- Language: Kotlin 2.0.21 (JVM 17 target)
- UI: Jetpack Compose, Material 3, Single Activity architecture
- Architecture: MVVM, Repository pattern, StateFlow, Coroutines
- DI: Hilt
- Navigation: Navigation Compose
- Database: Room with KSP (`exportSchema = true`), FTS for search
- Settings: DataStore Preferences
- OCR & Capture: On-device ML Kit Text Recognition, Photo Picker, ML Kit Document Scanner
- Serialization: kotlinx.serialization
- Storage & Backup: Storage Access Framework (SAF), JSON schema v1
- Background Tasks: WorkManager
- Build System: Gradle 8.13 with Kotlin DSL and Version Catalog (`gradle/libs.versions.toml`)
- Min SDK: 26 (Android 8.0+) | Target & Compile SDK: 35

## Package Layout
Root package: `com.sijan.kharcha`
- `data/db/` -> `AppDatabase.kt`, `dao/`, `entity/`, `migration/`
- `data/repository/` -> Repository implementations
- `data/backup/` -> JSON backup & restore service
- `data/storage/` -> App-private receipt image storage
- `domain/model/` -> Pure Kotlin domain models
- `domain/parser/` -> `ReceiptParser.kt`, `EsewaParser.kt`, `Normalizer.kt`, `Validator.kt`
- `di/` -> Hilt dependency injection modules
- `ui/theme/` -> Theme, Colors, Typography
- `ui/components/` -> Shared composables (`AmountText`, `ConfirmScanCard`, etc.)
- `ui/navigation/` -> NavHost and destination routes
- `ui/{home, entries, scan, summary, settings}/` -> Feature screens and ViewModels

## Build, Test & Lint Commands
- Build Debug APK: `./gradlew assembleDebug`
- Run Unit Tests: `./gradlew testDebugUnitTest`
- Run Lint: `./gradlew lintDebug`
- Check All Gates: `bash scripts/check.sh`

## Scope & Feature Freeze
The v1 scope in `docs/PRD.md` is strictly frozen.
Do not add features, background sync, or unrequested abstractions.
All new ideas, v1.1 concepts, or future suggestions must be appended to `docs/IDEAS.md`.

## Non-Negotiable Rules
1. Money is `Long` in paisa. No `Double` or `Float` for money anywhere in data, domain, or presentation calculations.
2. Dates are stored as epoch milliseconds UTC (`Long`). Formatting happens only in presentation.
3. No destructive Room migrations (`fallbackToDestructiveMigration` is forbidden). Every schema bump requires an explicit `Migration` and migration test. `exportSchema = true` is mandatory; schema JSON files are committed to Git.
4. No `android.permission.INTERNET`. Zero network SDKs, zero analytics, zero external logging.
5. No scanned entry is saved without user confirmation on the Confirm screen.
6. Duplicate transaction IDs warn the user with "View existing" and "Save anyway"; they are never merged automatically.
7. Raw OCR text and source image path must be stored with every scanned entry.
8. UI never calls DAOs directly. All writes and reads flow through `LedgerRepository`.
9. No hard-coded user-facing strings in UI code. All strings reside in `strings.xml`.
10. No secrets, keystores (`*.jks`, `*.keystore`), or `local.properties` in Git.
11. Parser rules are TBD until real receipt samples exist. Do not invent eSewa field names. Mark unknowns with `TODO(parser-spec)`.

## Git Rules
- Small, focused commits following Conventional Commits (`feat:`, `fix:`, `test:`, `chore:`).
- Stage specific files, never broad `git add .`.
- Never force push (`--force`) and never rewrite history.
- Push after each completed unit of work.

## Definition of Done
A task is done only when:
1. `./gradlew assembleDebug` succeeds.
2. All unit tests pass (`./gradlew testDebugUnitTest`).
3. Lint checks pass (`./gradlew lintDebug`).
4. All repository guards pass (`bash scripts/check.sh`).
5. Related documentation is updated if behavior changed.

## Documentation References
- Product Requirements: `docs/PRD.md`
- Technical Requirements: `docs/TRD.md`
- Data Layer Architecture: `docs/DATA_LAYER_STRUCTURE.md`
- Screen & App Flows: `docs/APP_FLOW.md`
- Frontend & Compose Guidelines: `docs/FRONTEND_GUIDELINES.md`
