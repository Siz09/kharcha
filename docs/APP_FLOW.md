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

## 4. Flow: scan from a shared screenshot

1. The user opens eSewa, takes a screenshot, and shares it to Kharcha.
2. Kharcha opens directly on Confirm scan with the image loaded.
3. The app runs OCR, normalizer, parser, and validator.
4. All checks pass: fields are pre-filled, Save is enabled.
5. A check fails: the field is highlighted with a reason, and Save stays disabled until fixed.
6. Duplicate transaction ID found: a warning appears with "View existing entry" and "Save anyway".
7. User taps Save. The entry is stored with raw OCR text and the image path.

## 5. Flow: scan from the gallery or camera

1. Tap Scan.
2. Choose Photo Picker for an existing screenshot, or Camera for a paper receipt.
3. Camera opens the Document Scanner, which crops and straightens the image.
4. Continue from step 3 of the shared screenshot flow.

## 6. Flow: OCR cannot parse

1. No parser matches, or required fields are missing.
2. Show a plain message: the receipt could not be read fully.
3. Open the manual form with the image attached and any extracted fields filled in.
4. The user completes and saves. Store the raw OCR text so the receipt can be re-parsed later.

## 7. Flow: edit or delete

1. Open an entry from a list.
2. Edit: change fields, Save. `updatedAt` changes.
3. Delete: confirmation dialog. On confirm, delete the entry and its stored image.

## 8. Flow: backup

1. First run: Settings prompts the user to choose a backup folder.
2. Manual backup: tap Back up now. Write the JSON file and show the file name and time.
3. Auto-backup: a WorkManager job writes on schedule and keeps the latest N files.
4. Failure: show which step failed. State clearly that no data was changed.

## 9. Flow: restore

1. Settings, then Restore. Pick a backup file.
2. The app validates the schema version and every record.
3. Show a summary: number of categories and entries, backup date.
4. The user confirms. The app warns that restore replaces current data.
5. Restore runs in one transaction. On failure, roll back and keep existing data.

## 10. First launch

1. Seed default categories.
2. Show a short intro with three points: scan receipts, data stays on this device, set up backup.
3. Prompt for the backup folder.

## 11. Edge cases

- Share Target receives a non-image file: show a message and ignore it.
- Very large image: downscale before OCR.
- Date in the future: validation error.
- Process death during scan: restore the image URI and parsed state from saved state.
- Storage permission revoked for the backup folder: ask the user to choose the folder again.
