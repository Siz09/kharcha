---
name: backup-roundtrip
description: Use this skill when testing or implementing JSON backup and restore, verifying schema compatibility, or ensuring backup data integrity.
---

# Backup & Restore Round-Trip Verification Workflow

Follow this procedure to verify that backup creation and database restoration preserve 100% data fidelity.

## Requirements
- Format: JSON with `schemaVersion: 1`, `exportedAt`, `categories`, and `entries`.
- Atomic restoration: Entire restore runs inside a single database transaction.
- Any validation failure aborts the restore and preserves existing database state untouched.

## Step-by-Step Verification Procedure

1. **Populate Known Test State**:
   - Seed database with a known set of categories and entries (mix of manual and receipt entries with raw OCR text, tags, and transaction IDs).
   - Record exact count and hash/snapshot of database records.

2. **Execute Export**:
   - Invoke `BackupService.exportBackup(outputStream)`.
   - Validate JSON output against schema version 1.
   - Assert all categories and ledger entries are correctly serialized.

3. **Simulate Disaster (Wipe)**:
   - Clear all tables in test database instance.
   - Assert that tables are empty.

4. **Execute Restore**:
   - Invoke `BackupService.restoreBackup(inputStream)`.
   - Assert restore completes without error.

5. **Compare and Assert Fidelity**:
   - Query all restored categories and entries.
   - Assert 1:1 match with initial state:
     - All IDs, paisa amounts, timestamps, transaction IDs, notes match exactly.
     - Foreign key relationships intact.
     - Zero data loss or silent field modification.

6. **Test Invariant Violations**:
   - Test invalid JSON / unknown future `schemaVersion`.
   - Verify transaction aborts and existing records remain untouched.
