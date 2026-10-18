# Kharcha v1: Milestone 1 — Data Layer Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Room persistence layer, domain models, paisa money utilities, and `LedgerRepository` for offline transaction storage.

**Architecture:** MVVM clean architecture with Room SQLite backend and FTS search. Repository mediates all data access; ViewModels never access DAOs directly.

**Tech Stack:** Kotlin 2.0.21, Room 2.6.1 + KSP, Hilt 2.51.1, Coroutines & Flow, JUnit4.

**Spec:** `docs/DATA_LAYER_STRUCTURE.md`, `docs/TRD.md`, `AGENTS.md`.

## Global Constraints
- Money is `Long` in paisa. No `Double` or `Float` in calculations.
- Dates are epoch milliseconds UTC (`Long`).
- Room `exportSchema = true` is mandatory; schema JSON committed to Git.
- No destructive migrations.
- Strict offline mode: zero network permissions or SDKs.

---

### Task 1: Domain Models and Money Value Utilities

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/domain/model/EntryType.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/model/Category.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/model/LedgerEntry.kt`
- Create: `app/src/main/java/com/sijan/kharcha/domain/model/Money.kt`
- Test: `app/src/test/java/com/sijan/kharcha/domain/model/MoneyTest.kt`

**Interfaces:**
- Produces: `EntryType`, `Category`, `LedgerEntry`, and `Money.formatPaisa(paisa: Long): String`.

- [ ] **Step 1: Write failing unit test for Money formatting**
- [ ] **Step 2: Run test to confirm failure**
- [ ] **Step 3: Implement domain models and paisa formatter**
- [ ] **Step 4: Run test to confirm green**
- [ ] **Step 5: Run `bash scripts/guard-money.sh`**
- [ ] **Step 6: Commit Task 1**

---

### Task 2: Room Entities, DAOs, and Database Setup

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/data/db/entity/CategoryEntity.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/db/entity/LedgerEntryEntity.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/db/entity/LedgerEntryFtsEntity.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/db/dao/CategoryDao.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/db/dao/LedgerDao.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/db/AppDatabase.kt`
- Test: `app/src/test/java/com/sijan/kharcha/data/db/DatabaseTest.kt`

**Interfaces:**
- Consumes: `EntryType` from Task 1.
- Produces: `AppDatabase`, `CategoryDao`, `LedgerDao`.

- [ ] **Step 1: Write failing tests for CategoryDao and LedgerDao queries**
- [ ] **Step 2: Run test to confirm failure**
- [ ] **Step 3: Implement entities, DAOs, and AppDatabase with `exportSchema = true`**
- [ ] **Step 4: Run `./gradlew kspDebugKotlin` to generate schema JSON v1**
- [ ] **Step 5: Run tests to verify all queries and foreign key constraints pass**
- [ ] **Step 6: Run `bash scripts/guard-migrations.sh`**
- [ ] **Step 7: Commit Task 2 and committed schema JSON**

---

### Task 3: LedgerRepository Implementation and Seed Data

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/data/repository/LedgerRepository.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/repository/LedgerRepositoryImpl.kt`
- Create: `app/src/main/java/com/sijan/kharcha/di/DatabaseModule.kt`
- Test: `app/src/test/java/com/sijan/kharcha/data/repository/LedgerRepositoryTest.kt`

**Interfaces:**
- Consumes: `AppDatabase`, `CategoryDao`, `LedgerDao`.
- Produces: `LedgerRepository` (exposed to ViewModels).

- [ ] **Step 1: Write failing tests for repository CRUD, duplicate check, and seed categories**
- [ ] **Step 2: Run test to confirm failure**
- [ ] **Step 3: Implement `LedgerRepositoryImpl` and Hilt `DatabaseModule`**
- [ ] **Step 4: Run repository tests to confirm green**
- [ ] **Step 5: Run `bash scripts/check.sh`**
- [ ] **Step 6: Commit Task 3**
