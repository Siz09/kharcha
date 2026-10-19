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
