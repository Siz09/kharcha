# Kharcha: Technical Requirements Document

Status: tech stack locked. Parser specification pending real receipt samples.

## 1. Stack

| Area | Choice |
|------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, single Activity |
| Architecture | MVVM, Repository pattern, StateFlow, Coroutines and Flow |
| DI | Hilt |
| Navigation | Navigation Compose |
| Database | Room with KSP, `exportSchema = true`, FTS for search |
| Settings | DataStore |
| OCR | ML Kit text recognition, on-device |
| Capture | Share Target, Photo Picker, ML Kit Document Scanner, FileProvider |
| Serialization | kotlinx.serialization |
| Backup storage | Storage Access Framework |
| Background | WorkManager |
| Build | Gradle Kotlin DSL, version catalog, min SDK 26 |
| Testing | JUnit, Room migration tests, parser fixture tests |
| Source control | Git, private GitHub repo, push daily |

Pin library versions from the latest stable releases at project creation. Record them in the version catalog.

## 2. Architecture

Three layers.

- UI: Compose screens and ViewModels. ViewModels expose immutable UI state through `StateFlow`.
- Domain: parser interface, validators, normalizers. Pure Kotlin where possible, so it is easy to unit test.
- Data: Room database, DAOs, Repository, backup service, file storage.

The UI never touches DAOs directly. All writes go through the Repository.

## 3. Data integrity rules

1. Money is a `Long` in paisa. No `Double`, no `Float`.
2. Dates are epoch milliseconds in UTC. Display formatting happens in the UI layer.
3. Foreign keys are on. A category with entries cannot be deleted.
4. Migrations are explicit and tested. Destructive migration is forbidden.
5. Every scanned entry stores raw OCR text and the source image path.
6. No scanned entry is saved without user confirmation.
7. Duplicate transaction IDs raise a warning. They are never merged automatically.
8. Edits update an `updatedAt` field. An audit log table follows in v1.1.

## 4. Receipt pipeline

1. Input: screenshot via Share Target or Photo Picker, or camera via Document Scanner.
2. Preprocess: only for noisy camera images (grayscale, contrast, upscale). Skip for clean screenshots.
3. OCR: ML Kit text recognition. Keep bounding boxes, not only flat text. Add the Devanagari recognizer only if sample receipts need it.
4. Normalize: Devanagari digits to ASCII, strip `Rs.`, commas, and extra spaces. Fix look-alike characters (O/0, l/1, S/5) in numeric fields only.
5. Parse: a `ReceiptParser` interface with one class per source. eSewa first.
6. Validate: amount greater than zero with at most 2 decimals, date valid and not in the future, transaction ID matches the known pattern, amount plus fee equals total when both exist.
7. Review: always show the confirm screen. Failed checks highlight the field and block one-tap confirm.
8. Duplicate check by transaction ID.
9. Save the entry together with raw OCR text and the image path.

Parser rules: TBD. Fill in after receiving redacted sample receipts.

## 5. Backup and restore

- Format: JSON containing a schema version, categories, entries, and settings. CSV export is separate and one-way.
- Location: a folder chosen by the user through the Storage Access Framework.
- Restore: validate the schema version and every record before writing. Run inside one database transaction. On any failure, roll back and leave existing data untouched.
- Auto-backup: a periodic WorkManager job writes to the chosen folder and keeps the most recent N files.

## 6. Security and privacy

- Fully offline. No network permission.
- Receipt images live in app-private storage, shared only through FileProvider.
- No analytics and no third-party SDKs beyond ML Kit.
- Biometric lock is planned for v2.

## 7. Testing

- Parser fixtures: real receipt text plus expected fields. Run on every commit.
- Every user correction in daily use becomes a new fixture.
- Room migration tests for each schema version, using the exported schema JSON files.
- Backup round-trip test: export, wipe, restore, compare.
- Unit tests for money formatting, validators, and the Bikram Sambat table (v1.1).

Accuracy targets on the fixture corpus: 99% per field for screenshots, 95% for camera captures. These are goals to measure against, not guarantees.

## 8. Non-functional requirements

- Cold start under 2 seconds on a mid-range phone.
- Scan to confirm screen under 3 seconds for a screenshot.
- Works fully offline.
- Supports Android 8.0 and newer.

## 9. Open items

1. Verify min SDK 26 against real target phones.
2. Decide on the Devanagari recognizer after testing samples.
3. Write the parser specification from 5 or more redacted eSewa receipts.

## 10. Flexible ORM & Layout-Aware Extraction Architecture (v2)

### 10.1 Flexible Room Schema Evolution
To support diverse payment channels and statement sources without frequent schema churn:
- `paymentSource`: String enum (`CASH`, `ESEWA`, `KHALTI`, `BANK_NABIL`, `BANK_NIC_ASIA`, etc.) defaulting to `CASH`.
- `sourceTemplateId`: String identifying the layout format used (e.g., `esewa_slip_v2`, `khalti_wallet_v1`, `fonepay_qr_v1`, `nabil_sms_v1`).
- `extraMetadataJson`: Nullable JSON string for auxiliary non-indexed fields (e.g., PAN/VAT number, counterparty bank, trace reference).
- Preserves all invariants: `amountPaisa` remains strictly `Long`, dates remain UTC epoch milliseconds.

### 10.2 Layout-Aware Template Extraction Engine
Instead of rigid global regexes:
- **Anchor Detection**: Scans OCR blocks for unique anchor signatures (e.g. `["Transaction Code", "eSewa ID"]`, `["Khalti", "Txn ID"]`, `["Fonepay", "Trace ID"]`, `["A/C debited", "NABIL"]`).
- **Spatial Extraction**: Uses ML Kit bounding boxes (`Rect`) to extract values relative to anchor positions (e.g. amount token directly below `"Total NPR"`).
- **Template Registry**: Local definition registry of known templates with anchor coordinates and field matching heuristics.
- **On-Device Layout Memory**: When a user corrects or validates an unparsed field on the Confirm screen, the engine records the token offset relative to the nearest anchor to refine confidence for subsequent receipts matching that `sourceTemplateId`.
- **On-Device SMS Parser**: Strictly local `BroadcastReceiver` matching bank debit SMS templates with zero internet permission, prompting user to confirm adding the transaction.

## 11. v3 Technical Architecture: Retention, Full-Asset Backups, Documents & Annual Analytics

### 11.1 APK Upgrade Data Retention & Signing Invariants
- **Package Stability**: Immutable `applicationId = "com.sijan.kharcha"` across all releases.
- **Key Signing Continuity**: Release APK builds signed with a dedicated release keystore (`kharcha-release.jks`) configured via environment variables or local credentials. Signature continuity ensures Android OS does not reject APK updates with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, avoiding uninstalls that would wipe app data.
- **Zero-Destructive Migration Invariant**: `fallbackToDestructiveMigration()` is strictly forbidden. Schema bumps require explicit `Migration(N, N+1)` implementations with verified migration unit tests and committed schema JSON files.
- **App-Private Data Storage**: Database (`app_database.db`), preferences DataStore (`user_preferences.preferences_pb`), receipt images, and document attachments are stored in `context.filesDir` / app-private storage, which Android preserves across APK upgrades.

### 11.2 System Share Sheet Target Expansion
- **Manifest Intent Filter**: Expanded `<intent-filter>` under `MainActivity` for `ACTION_SEND` and `ACTION_SEND_MULTIPLE`:
  - `image/*` (PNG, JPEG, WEBP)
  - `application/pdf`
  - `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (DOCX)
  - `application/msword` (DOC)
- **Direct Launch Flow**: When shared from Gallery, Files, Gmail, WhatsApp, or Google Drive, Kharcha stages the incoming URI in app-private temporary cache, authenticates via `BiometricPrompt` if biometric lock is active, and routes immediately to `ConfirmScanScreen` or document attachment flow without requiring pre-launch.

### 11.3 Document Attachments & Offline Statement Engine (PDF & DOCX)
- **Local PDF Rendering**: Uses Android's native `android.graphics.pdf.PdfRenderer` to render pages to Compose `Bitmap` thumbnails on-device with zero network and zero external dependencies.
- **DOCX Text Extraction**: Lightweight streaming `ZipInputStream` parser reading `word/document.xml` using `XmlPullParser` to extract statement dates, transaction amounts, and reference IDs without heavy Apache POI dependencies.
- **Schema & Entity Integration**:
  - `receiptPath`: String path pointing to local file in `context.filesDir/receipts/`.
  - `documentType`: Enum (`IMAGE`, `PDF`, `DOCX`) stored in entity metadata for format-specific viewing and handling.

### 11.4 Full-Asset Backup Archive (`.kharcha` ZIP Bundle) & Weekly Auto-Backup
- **Archive Format (`.kharcha`)**: Standard ZIP container containing:
  - `manifest.json`: Versioning, export timestamp, file counts, and SHA-256 checksums of every bundled asset.
  - `database.json`: Full serialized snapshot of all Room tables (Accounts, Categories, Ledger Entries, Bills, Recurring, Udhar, Savings Goals, Utility Accounts, Audit Logs).
  - `assets/`: App-private receipt images, PDF statements, and DOCX invoices.
- **Storage Access Framework (SAF) & Cloud Destination**:
  - User selects folder via `ACTION_OPEN_DOCUMENT_TREE` and app acquires persistable permission via `contentResolver.takePersistableUriPermission`.
  - Works with local phone storage, SD cards, or Google Drive (synced by the Android OS cloud document provider with zero internet permissions in Kharcha).
- **Weekly Auto-Backup WorkManager**:
  - `PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)` with battery and storage constraints.
  - User can toggle between Automatic (Weekly) and Manual-only.
  - Automatically prunes older backups in the selected directory, maintaining the latest 5 archives.

### 11.5 Lossless, Atomic Multi-Table & Asset Restore
- **Phase 1: Pre-flight Integrity & Checksum Verification**:
  - Unpacks `.kharcha` archive to a temporary staging folder (`cacheDir/restore_staging/`).
  - Reads `manifest.json` and verifies SHA-256 hashes of all bundled assets against the manifest.
  - Confirms schema version compatibility against current `AppDatabase` version.
- **Phase 2: Atomic Database Transaction**:
  - Runs inside `db.withTransaction { ... }`.
  - Clears active tables and inserts validated rows in dependency order (Categories, Accounts -> Entries -> Udhar -> Goals -> Utility Accounts).
- **Phase 3: Atomic Asset Swap**:
  - Moves verified asset files from staging to `filesDir/receipts/`.
  - Cleans up staging cache.
- **Rollback Guarantee**: On any checksum failure, schema violation, or IO error during Phase 1–2, the transaction aborts, staging files are wiped, and existing database records and assets remain untouched.

### 11.6 Annual Financial Analytics & Historical Archiving Engine
- **Year-to-Date (YTD) & Full-Year Calculation**:
  - Aggregates income, expenses, and net savings from Year Start (Jan 1 AD or Baisakh 1 BS) to Year End (Dec 31 AD or Chaitra end BS).
  - Calculates live Year-to-Date (YTD) metrics comparing current date against the same period in previous years.
  - Category breakdown matrices for income sources and expense destinations with percentage distribution.
- **Historical Partitioning & Archiving**:
  - Data Retention Invariant: Historical transactions are permanent and never purged.
  - SQLite Pre-computed Summary Table (`annual_archive_summaries`): Stores aggregated yearly statistics (total income, total expense, category totals) once a calendar/BS year ends.
  - Enables instant millisecond loading of past years' financial summaries without scanning multi-year transaction tables, while maintaining drill-down access to raw historical entries.
