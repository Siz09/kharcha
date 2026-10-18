# Kharcha v1: Milestone 2 — Backup and Restore Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build JSON backup export and atomic restore service with schema versioning and round-trip verification, plus one-way CSV export.

**Architecture:** Streaming I/O with `kotlinx.serialization`. Restoration executes inside a single Room transaction to guarantee zero data loss on corruption.

**Tech Stack:** Kotlin 2.0.21, kotlinx.serialization.json, Room `withTransaction`, Coroutines, JUnit4, Robolectric.

**Spec:** `docs/DATA_LAYER_STRUCTURE.md` Section 6, `docs/TRD.md` Section 5, `docs/PRD.md` Section 6, `.agents/skills/backup-roundtrip/SKILL.md`.

## Global Constraints
- Schema version is locked to `1` for v1.
- Newer schema versions must be rejected before any database write.
- Atomic restoration: Entire restore runs inside a Room transaction. Failure leaves existing data untouched.
- Money is `Long` in paisa. No floating point in backup schema or serialization.
- Fully offline: reads and writes to user-supplied `InputStream` / `OutputStream` via Storage Access Framework.

---

### Task 1: Backup JSON Serialization and Schema Models

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/data/backup/model/BackupPayload.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/backup/model/BackupCategory.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/backup/model/BackupEntry.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/backup/JsonBackupSerializer.kt`
- Test: `app/src/test/java/com/sijan/kharcha/data/backup/JsonBackupSerializerTest.kt`

**Interfaces:**
- Produces: `JsonBackupSerializer.serialize(payload: BackupPayload, outputStream: OutputStream)`
- Produces: `JsonBackupSerializer.deserialize(inputStream: InputStream): Result<BackupPayload>`

- [ ] **Step 1: Write failing tests for JSON serialization, schema validation, and error rejection**
- [ ] **Step 2: Run test to confirm failure (RED)**
- [ ] **Step 3: Implement data models with `@Serializable` and `JsonBackupSerializer`**
- [ ] **Step 4: Run test to confirm green (GREEN)**
- [ ] **Step 5: Run `bash scripts/guard-money.sh`**
- [ ] **Step 6: Commit Task 1**

---

### Task 2: BackupService Implementation & Atomic Roundtrip

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/data/backup/BackupSummary.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/backup/BackupService.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/backup/BackupServiceImpl.kt`
- Test: `app/src/test/java/com/sijan/kharcha/data/backup/BackupRoundtripTest.kt`

**Interfaces:**
- Consumes: `AppDatabase`, `CategoryDao`, `LedgerDao`, `JsonBackupSerializer`.
- Produces: `BackupService.exportBackup(outputStream: OutputStream): Result<Int>`
- Produces: `BackupService.restoreBackup(inputStream: InputStream): Result<BackupSummary>`
- Produces: `BackupService.exportCsv(outputStream: OutputStream): Result<Int>`

- [ ] **Step 1: Write failing round-trip test (export -> wipe -> restore -> compare 1:1) and atomic rollback test**
- [ ] **Step 2: Run test to confirm failure (RED)**
- [ ] **Step 3: Implement `BackupServiceImpl` with atomic Room transaction and CSV exporter**
- [ ] **Step 4: Run round-trip tests to confirm green (GREEN)**
- [ ] **Step 5: Run `bash scripts/guard-migrations.sh`**
- [ ] **Step 6: Commit Task 2**

---

### Task 3: Hilt Wiring & Verification Gates

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/di/BackupModule.kt`

**Interfaces:**
- Produces: Hilt `@Singleton` binding for `BackupService`.

- [ ] **Step 1: Implement `BackupModule.kt`**
- [ ] **Step 2: Run `bash scripts/check.sh`**
- [ ] **Step 3: Commit Task 3 & Push to `origin main`**
