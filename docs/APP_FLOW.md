# Kharcha: App Flow

## 1. Screen map

| Screen | Purpose |
|--------|---------|
| Home | Month summary and recent entries. Entry points for Add and Scan |
| Entry list | Full list with search and filters |
| Entry detail / edit | View, edit, delete one entry. Shows source image if present |
| Add entry | Manual form |
| Scan source sheet | Choose Photo Picker or Camera |
| Confirm scan | Parsed fields beside the source image |
| Categories | Add and rename categories |
| Summary | Month picker with totals |
| Settings | Backup, restore, export, auto-backup, theme |

Bottom navigation: Home, Entries, Summary, Settings. A floating action button on Home and Entries opens Add or Scan.

## 2. Navigation

```
Home ─┬─ Add entry ── Home
      ├─ Scan source sheet ─ Confirm scan ── Home
      └─ Entry detail ── Edit
Entries ─ Entry detail
Summary
Settings ─ Categories
         ─ Backup / Restore
```

## 3. Flow: add entry manually

1. Tap Add.
2. Enter amount, choose type, category, date, optional note.
3. Tap Save.
4. Validation passes: entry saved, return to the previous screen, snackbar confirms.
5. Validation fails: show the error on the field, stay on the form.

## 4. Flow: scan or import from a shared file (Image, PDF, DOCX)

1. The user selects a receipt screenshot from Gallery, a bank statement PDF from Gmail/Files, or a DOCX invoice, and taps Share to Kharcha.
2. If Biometric Lock is active, the app authenticates the user via `BiometricPrompt` before opening content.
3. Kharcha opens directly on Confirm scan with the image or document staged.
4. For images: runs OCR, normalizer, parser, and validator.
5. For PDFs: generates thumbnail via `PdfRenderer` and extracts text tokens from statement layout.
6. For DOCX: parses `word/document.xml` to extract invoice amount, date, and reference.
7. All checks pass: fields are pre-filled, Save is enabled.
8. A check fails: field is highlighted with a reason, and Save stays disabled until fixed.
9. Duplicate transaction ID found: a warning appears with "View existing entry" and "Save anyway".
10. User taps Save. Entry is stored with raw OCR text, document type, and the file path in app-private storage.

## 5. Flow: scan from the gallery, camera, or file picker

1. Tap Scan.
2. Choose Photo Picker for an existing screenshot, Camera for a paper receipt, or Document Picker for a PDF/DOCX statement.
3. Camera opens the Document Scanner, which crops and straightens the image.
4. Document Picker allows selecting PDF bank statements or DOCX invoices.
5. Continue from step 4 of the shared file flow.

## 6. Flow: OCR cannot parse

1. No parser matches, or required fields are missing.
2. Show a plain message: the receipt or statement could not be read fully.
3. Open the manual form with the image or document attached and any extracted fields filled in.
4. The user completes and saves. Store the raw OCR or document text so the entry can be re-parsed later.

## 7. Flow: edit or delete

1. Open an entry from a list.
2. Edit: change fields, Save. `updatedAt` changes.
3. Delete: confirmation dialog. On confirm, delete the entry and its stored image/document.

## 8. Flow: full-asset backup (.kharcha bundle)

1. First run / Settings: User selects a backup destination folder via Storage Access Framework (SAF).
   - Can choose a local device folder or Google Drive folder (cloud provider integrated into Android SAF).
2. Configuration: User toggles Automatic Backup (Weekly, 7-day interval) or Manual-only.
3. Manual backup: User taps "Back up now".
4. App bundles `manifest.json` (with SHA-256 asset checksums), `database.json` (all Room tables), and all stored receipt images / PDF / DOCX documents into a compressed `.kharcha` ZIP archive.
5. File is written atomically to the chosen folder. Shows file name, size, and timestamp.
6. Auto-backup: WorkManager executes weekly, prunes old backups, and keeps the latest 5 archives.
7. Failure: show which step failed. State clearly that existing data and assets were untouched.

## 9. Flow: lossless restore from archive

1. Settings -> Restore. Pick a `.kharcha` archive file.
2. Staging & Pre-flight check:
   - App unzips archive into an isolated temporary cache directory (`cacheDir/restore_staging/`).
   - Reads `manifest.json`, validates Room schema version compatibility, and checks SHA-256 hashes of all bundled assets.
3. Confirmation dialog: Displays backup timestamp, schema version, category count, entry count, and asset count.
4. User confirms. The app warns that restore replaces current local data with the backup contents.
5. Execution:
   - Runs database restore in a single `db.withTransaction` block.
   - Moves verified assets from staging to `filesDir/receipts/`.
6. Safe rollback: If any step fails (corrupt archive, hash mismatch, schema error), the database transaction is rolled back, staging files are deleted, and current data remains completely untouched.

## 10. Flow: annual analytics & historical archive browsing

1. User opens Summary / Reports screen and selects "Annual Analytics".
2. Year selector allows toggling between Current Year (YTD) and any historical year.
3. Annual Summary displays:
   - Total earnings (income) vs total spending (expenses) from Year Start to Year End.
   - Live YTD tracker for current year showing pace and burn rate.
   - Category breakdowns: "Where money came from" (income sources) and "Where money went" (expense categories) with percentage distributions.
4. Historical Data Archiving:
   - Past years' data is never deleted.
   - Once a year ends, its totals are indexed in `annual_archive_summaries` for instant zero-lag review.
   - Full historical entries remain searchable and filterable at any time.

## 11. First launch

1. Seed default categories.
2. Show a short intro with three points: scan receipts, data stays on this device, set up backup.
3. Prompt for the backup folder.

## 12. Edge cases

- Share Target receives unsupported mime type: show a helpful message and ignore it.
- Large PDF statement with multiple pages: extract text and render first page thumbnail.
- Very large image: downscale before OCR.
- Date in the future: validation error.
- Process death during scan: restore the image URI and parsed state from saved state.
- Storage permission revoked for the backup folder: ask the user to choose the folder again.
