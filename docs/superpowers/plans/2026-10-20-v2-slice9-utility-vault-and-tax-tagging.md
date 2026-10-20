# Kharcha v2 Slice 9: Utility Vault & Tax Deductible Tagging Implementation Plan

> **Note**: This is the final slice for Kharcha v2.0 milestone. All database changes are consolidated into Room Database Schema v7 (`MIGRATION_6_7`).

## Proposed Changes

### Data & Room Layer (Schema v7)
- `UtilityType.kt`: NEA, KHANEPANI, ISP, OTHER
- `UtilityAccount.kt`: Domain model
- `TaxCategory.kt`: LIFE_INSURANCE, HEALTH_INSURANCE, MEDICAL, RETIREMENT, DONATION, OTHER
- `TaxDeductionCategorySummary.kt` and `FiscalYearTaxSummary.kt`: Domain models for tax calculations
- `UtilityAccountEntity.kt`: Room entity with table name `utility_accounts`
- `UtilityAccountDao.kt`: Room DAO for CRUD & Flow queries
- `LedgerEntryEntity.kt`: Add `isTaxDeductible: Boolean = false` and `taxCategory: TaxCategory? = null`
- `LedgerEntry.kt`: Add `isTaxDeductible: Boolean = false` and `taxCategory: TaxCategory? = null`
- `MIGRATION_6_7`: Table creation and columns addition in `Migrations.kt`
- `AppDatabase.kt`: Version 7, register `UtilityAccountEntity`, `utilityAccountDao()`, `MIGRATION_6_7`
- `DatabaseModule.kt`: Provide `UtilityAccountDao` and bind `UtilityRepository`
- `Migration6To7Test.kt`: Migration test verifying data preservation and table schema

### Business & Repository Layer
- `UtilityRepository.kt` & `UtilityRepositoryImpl.kt`: CRUD operations
- `TaxDeductionCalculator.kt`:
  - Calculate Nepali Fiscal Year (Shrawan 1 to Ashad end, mid-July to mid-July)
  - Ceiling calculations: Life Insurance (Rs. 40,000 / 4,000,000 paisa), Health Insurance (Rs. 20,000 / 2,000,000 paisa)
- `TaxDeductionCalculatorTest.kt`: Unit tests for FY boundaries and ceiling capping
- `UtilityRepositoryTest.kt`: Unit tests for utility accounts CRUD

### Backup & Restore Layer
- `BackupUtilityAccount.kt`: Serialization model
- `BackupEntry.kt`: Add `isTaxDeductible` and `taxCategory`
- `BackupPayload.kt`: Add `utilityAccounts: List<BackupUtilityAccount> = emptyList()`
- `BackupServiceImpl.kt`: Export and import utility accounts & tax fields atomically

### Presentation Layer
- Strings in `res/values/strings.xml` for Utility Vault and Tax Deductions
- `UtilityVaultViewModel.kt` & `UtilityVaultScreen.kt`:
  - Filter chips (All, Electricity, Water, Internet, Other)
  - Cards with quick clipboard copy with tactile haptic feedback & checkmark transition
  - Add / Edit Dialog
- `AddEditEntryScreen.kt` & `AddEditEntryViewModel.kt`:
  - Tax deductible toggle & category selector chips
- `TaxSummaryCard.kt` on `SummaryScreen.kt`:
  - Fiscal year tax deduction progress bars against ceilings
- Navigation: Route `Screen.UtilityVault` wired in `Screen.kt`, `KharchaNavHost.kt`, `SettingsScreen.kt`

---

## Verification Plan

### Automated Tests
1. `rtk gradlew testDebugUnitTest --tests "*Migration*"`
2. `rtk gradlew testDebugUnitTest --tests "*Utility*"`
3. `rtk gradlew testDebugUnitTest --tests "*Tax*"`
4. `rtk gradlew testDebugUnitTest --tests "*Backup*"`
5. `rtk gradlew testDebugUnitTest`
6. `rtk gradlew lintDebug`
7. `node scripts/guard.mjs worktree`
8. `rtk gradlew assembleDebug`
