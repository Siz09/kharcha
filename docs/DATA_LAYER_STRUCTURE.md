# Kharcha: Data Layer Structure

Kharcha has no server. This document replaces a "backend structure" doc and covers everything below the UI: database, repository, parsing, backup, and file storage.

## 1. Package layout

```
data/
  db/
    AppDatabase.kt
    dao/
    entity/
    migration/
  repository/
  backup/
  storage/
domain/
  model/
  parser/
    ReceiptParser.kt
    EsewaParser.kt
    Normalizer.kt
    Validator.kt
di/
```

## 2. Entities (v1, database version 1)

`Category`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | primary key, auto-generated |
| name | String | unique per type |
| type | EntryType | INCOME or EXPENSE |

`LedgerEntry`

| Field | Type | Notes |
|-------|------|-------|
| id | Long | primary key, auto-generated |
| amountPaisa | Long | always positive |
| type | EntryType | INCOME or EXPENSE |
| categoryId | Long | foreign key to Category, RESTRICT on delete |
| note | String | default empty |
| date | Long | epoch millis, indexed |
| source | String | "manual" or "receipt" |
| receiptPath | String? | app-private image path |
| rawOcrText | String? | stored for re-parse and audit |
| transactionId | String? | indexed, used for duplicate checks |
| createdAt, updatedAt | Long | epoch millis |

The table is named `LedgerEntry`, not `Transaction`, to avoid a clash with the Room `@Transaction` annotation.

FTS: an FTS table over `note` for search. Add a merchant field to the entry and the FTS table if the parser extracts one.

Planned for v1.1: `Budget`, `RecurringRule`, `AuditLog`.

## 3. DAOs

- `LedgerDao`: insert, update, delete, observe list with filters, observe monthly totals by type, find by transaction ID, search through FTS.
- `CategoryDao`: insert, rename, observe all, count entries per category.

All reads return `Flow`. All writes are `suspend`.

## 4. Repository

`LedgerRepository` is the only class the ViewModels talk to. Responsibilities:

- CRUD for entries and categories.
- Duplicate lookup by transaction ID before a scanned entry is saved.
- Monthly summary queries.
- Coordinating image storage with entry deletion. Deleting an entry also deletes its stored image.

## 5. Parser layer

```kotlin
interface ReceiptParser {
    fun canParse(text: OcrResult): Boolean
    fun parse(text: OcrResult): ParseResult
}
```

- `OcrResult` holds the full text plus line and block bounding boxes.
- `ParseResult` holds the extracted fields, a per-field status, and the raw text.
- `Normalizer` runs before every parser. `Validator` runs after.
- Parsers are registered in Hilt. The first parser whose `canParse` returns true is used. If none match, the app falls back to manual entry with the image attached.

Parser rules for eSewa: TBD, pending redacted samples.

## 6. Backup format

JSON file with this top-level shape:

```json
{
  "schemaVersion": 1,
  "exportedAt": 0,
  "categories": [],
  "entries": []
}
```

- Restore reads `schemaVersion`, rejects newer versions than the app supports, and migrates older ones in code.
- Restore runs in one database transaction. Any invalid record aborts the whole restore.
- Receipt images are not inside the JSON in v1. Decide in v1.1 whether to bundle them in a zip.
- CSV export is one-way and carries human-readable columns only.

## 7. File storage

- Receipt images are stored in app-private storage.
- File names use a generated ID, never user input.
- Sharing outside the app goes through FileProvider only.

## 8. Migrations

- `exportSchema = true`. Commit the schema JSON files to Git.
- Every version bump gets an explicit `Migration` and a migration test.
- Destructive migration is forbidden.

## 9. Settings (DataStore)

Backup folder URI, auto-backup interval, number of backups to keep, theme choice, default category per type.
