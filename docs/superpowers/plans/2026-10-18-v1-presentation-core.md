# Kharcha v1: Milestone 3 — Presentation Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build shared Material 3 UI components, Compose navigation shell, Home screen with monthly summary card, and Entries screen with search and filters.

**Architecture:** MVI/MVVM with unidirectional data flow. Each screen defines a single immutable `UiState`, ViewModel exposes `StateFlow<UiState>`, composables are stateless and collected with `collectAsStateWithLifecycle()`.

**Tech Stack:** Jetpack Compose, Material 3, Navigation Compose, Hilt ViewModel, StateFlow, Coroutines.

**Spec:** `docs/FRONTEND_GUIDELINES.md`, `docs/APP_FLOW.md`, `AGENTS.md`.

## Global Constraints
- All user-facing text resides in `res/values/strings.xml`. Zero hardcoded strings in composables.
- Money is `Long` in paisa. No `Double` or `Float` in UI state or formatting.
- Display money strictly via `AmountText` with `Rs.` prefix and semantic coloring paired with sign/label.
- Touch targets >= 48dp; content descriptions on all icons.
- Every shared composable has `@Preview` in light and dark mode.

---

### Task 1: String Resources & Shared Compose Components

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/com/sijan/kharcha/ui/components/AmountText.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/components/EntryRow.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/components/EmptyState.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/components/DateFormatter.kt`

**Interfaces:**
- Produces: `AmountText(amountPaisa: Long, type: EntryType?, modifier: Modifier)`
- Produces: `EntryRow(entry: LedgerEntry, categoryName: String, onClick: () -> Unit)`
- Produces: `EmptyState(message: String, actionLabel: String?, onAction: (() -> Unit)?)`

- [ ] **Step 1: Populate `strings.xml` with all navigation and UI labels**
- [ ] **Step 2: Implement `DateFormatter` utility**
- [ ] **Step 3: Implement `AmountText`, `EntryRow`, and `EmptyState` with light/dark previews**
- [ ] **Step 4: Run `bash scripts/guard-strings.sh` to ensure zero hardcoded strings**
- [ ] **Step 5: Commit Task 1**

---

### Task 2: Navigation Shell and Screen Routes

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/navigation/KharchaNavHost.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/KharchaApp.kt`
- Modify: `app/src/main/java/com/sijan/kharcha/MainActivity.kt`

**Interfaces:**
- Produces: `Screen` sealed destinations (Home, Entries, Summary, Settings).
- Produces: `KharchaApp` main scaffold with bottom navigation bar and FAB.

- [ ] **Step 1: Define `Screen` route classes and bottom navigation items**
- [ ] **Step 2: Build `KharchaNavHost` and root `KharchaApp` scaffold**
- [ ] **Step 3: Wire `MainActivity` to render `KharchaApp`**
- [ ] **Step 4: Verify build with `./gradlew compileDebugKotlin`**
- [ ] **Step 5: Commit Task 2**

---

### Task 3: Home Feature Screen & ViewModel

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/ui/home/HomeUiState.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/home/HomeScreen.kt`
- Test: `app/src/test/java/com/sijan/kharcha/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Produces: `HomeViewModel`, `HomeScreen` composable with monthly summary card and recent entries list.

- [ ] **Step 1: Write failing unit test for `HomeViewModel` (seeds categories, calculates totals)**
- [ ] **Step 2: Run test to confirm failure (RED)**
- [ ] **Step 3: Implement `HomeUiState`, `HomeViewModel`, and `HomeScreen`**
- [ ] **Step 4: Run test to confirm green (GREEN)**
- [ ] **Step 5: Commit Task 3**

---

### Task 4: Entries List Feature Screen & ViewModel

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/ui/entries/EntriesUiState.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/entries/EntriesViewModel.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/entries/EntriesScreen.kt`
- Test: `app/src/test/java/com/sijan/kharcha/ui/entries/EntriesViewModelTest.kt`

**Interfaces:**
- Produces: `EntriesViewModel`, `EntriesScreen` with search bar and filter chips.

- [ ] **Step 1: Write failing unit test for `EntriesViewModel` (filter by type/category, search)**
- [ ] **Step 2: Run test to confirm failure (RED)**
- [ ] **Step 3: Implement `EntriesUiState`, `EntriesViewModel`, and `EntriesScreen`**
- [ ] **Step 4: Run test to confirm green (GREEN)**
- [ ] **Step 5: Run `bash scripts/check.sh` full verification gate**
- [ ] **Step 6: Commit Task 4 & Push to `origin main`**
