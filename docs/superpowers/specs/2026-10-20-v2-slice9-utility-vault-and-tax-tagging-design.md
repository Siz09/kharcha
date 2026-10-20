# Kharcha v2 Slice 9: Utility Account Vault & Tax Deductible Tagging Design Spec

**Date**: 2026-10-20  
**Status**: APPROVED  
**Author**: Sijan & Antigravity  
**Target Milestone**: v2.0 (Slice 9)

---

## 1. Overview & Goals

This slice completes the remaining v2 requirements from `docs/PRD.md`:
1. **Utility Account Number Vault**: Offline storage for NEA customer IDs, Khanepani meter numbers, ISP customer IDs, and counter details, with one-tap clipboard copying and direct linkage to the recurring bills tracker.
2. **Tax Deductible Tagging**: Tagging expense entries that qualify for personal income tax deductions under the Nepali Income Tax Act (Life Insurance, Health Insurance, Medical expenses, Retirement/CIT/SSF, and Donations), with fiscal year limit tracking.

To avoid multiple consecutive schema migrations, both features are unified in **Room Database Schema v7 (`MIGRATION_6_7`)**.

### Goals
1. **Room Schema v7 Migration**:
   - Create `utility_accounts` table.
   - Alter `ledger_entries` table to add `is_tax_deductible` (INTEGER) and `tax_category` (TEXT).
   - Export `7.json` schema and verify with migration unit tests (`Migration6To7Test`).
2. **Utility Account Vault**:
   - CRUD repository and reactive `Flow` for utility credentials.
   - Fast copy-to-clipboard button with haptic feedback.
   - Integration with `BillsScreen` to prefill or display consumer numbers during bill payment.
3. **Nepali Fiscal Year Tax Deduction Tracker**:
   - Support `TaxCategory` enum: `LIFE_INSURANCE`, `HEALTH_INSURANCE`, `MEDICAL`, `RETIREMENT`, `DONATION`, `OTHER`.
   - Calculate deductions grouped by Nepali Fiscal Year (Shrawan 1 to Ashad end, mid-July to mid-July).
   - Compute accumulated deductions against legal ceiling limits (e.g., Rs. 40,000 for life insurance, Rs. 20,000 for health insurance).
4. **Backup & Restore v7**:
   - Backward compatible: v1-v6 backups restore cleanly (defaulting utility accounts to empty and new fields to `false`/`null`).
   - Exports utility accounts and tax fields into v7 JSON schema.

---

## 2. Architecture & Data Flow

### 2.1 Database Schema v7 (`MIGRATION_6_7`)

```sql
-- Create utility accounts table
CREATE TABLE IF NOT EXISTS `utility_accounts` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `utility_type` TEXT NOT NULL,
    `provider_name` TEXT NOT NULL,
    `consumer_id` TEXT NOT NULL,
    `counter_or_branch` TEXT,
    `account_holder_name` TEXT,
    `notes` TEXT,
    `created_at` INTEGER NOT NULL,
    `updated_at` INTEGER NOT NULL
);

-- Alter ledger entries for tax deductible tagging
ALTER TABLE `ledger_entries` ADD COLUMN `is_tax_deductible` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `ledger_entries` ADD COLUMN `tax_category` TEXT DEFAULT NULL;
```

### 2.2 Domain Models (`domain/model/`)

```kotlin
enum class UtilityType {
    NEA,        // Nepal Electricity Authority
    KHANEPANI,  // Water Supply
    ISP,        // Internet Service Provider (WorldLink, Vianet, Subisu, etc.)
    OTHER
}

data class UtilityAccount(
    val id: Long = 0,
    val utilityType: UtilityType,
    val providerName: String,
    val consumerId: String,
    val counterOrBranch: String? = null,
    val accountHolderName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TaxCategory {
    LIFE_INSURANCE,    // Ceiling: Rs. 40,000 (4,000,000 paisa)
    HEALTH_INSURANCE,  // Ceiling: Rs. 20,000 (2,000,000 paisa)
    MEDICAL,           // Medical expenses credit
    RETIREMENT,        // CIT / SSF / EPF contributions
    DONATION,          // Tax-exempt charitable donations
    OTHER
}

data class TaxDeductionCategorySummary(
    val category: TaxCategory,
    val totalSpentPaisa: Long,
    val eligibleDeductionPaisa: Long,
    val legalCeilingPaisa: Long?,
    val remainingCapPaisa: Long?
)

data class FiscalYearTaxSummary(
    val fiscalYearLabel: String, // e.g. "2083/84 (2026/27)"
    val startEpochUtc: Long,
    val endEpochUtc: Long,
    val totalDeductiblePaisa: Long,
    val categorySummaries: List<TaxDeductionCategorySummary>
)
```

### 2.3 Presentation Layer
- **`UtilityVaultScreen`**:
  - Filter chips: All, Electricity, Water, Internet, Other.
  - Card list with Provider Name, Consumer ID (monospace, large), Counter/Branch, Holder Name.
  - "Copy" button: copies Consumer ID to Android clipboard, performs tactile haptic feedback, shows snackbar.
  - Add / Edit Utility Account Dialog.
- **`BillsScreen` Integration**:
  - If a bill has a linked utility account, display an inline pill: "Consumer ID: 123.456 (Tap to copy)".
- **`AddEditEntryScreen` & `ConfirmScanScreen`**:
  - Tax Deductible Switch.
  - When enabled, shows `TaxCategory` selection chips.
- **`SummaryScreen` / `TaxSummaryScreen`**:
  - Card: "Tax Deductions (FY 2083/84)".
  - Displays total eligible tax rebate savings and progress bars towards legal ceilings.

---

## 3. Impeccable Design & Animotion Guidelines

- **Utility Vault Card Design**:
  - High-readability monospace font for consumer IDs.
  - Quick-copy button with micro-interaction: checkmark icon transition on copy (`Animotion MI07` progress/complete pulse).
  - Clean grouping by utility provider with standard M3 iconography.
- **Tax Ceiling Meters**:
  - M3 LinearProgressIndicator with soft rounded corners.
  - Warning tint if approaching or exceeding legal ceiling limit.

---

## 4. String Resources (`res/values/strings.xml`)

- `utility_vault_title`: "Utility Account Vault"
- `utility_add_account`: "Add Utility Account"
- `utility_provider_name`: "Provider Name"
- `utility_consumer_id`: "Consumer ID / Customer No."
- `utility_counter`: "Counter / Branch"
- `utility_holder_name`: "Account Holder Name"
- `utility_copied_toast`: "Consumer ID copied to clipboard"
- `tax_deductible_label`: "Tax Deductible Expense"
- `tax_deductible_desc`: "Qualifies for personal income tax deduction under Nepal Income Tax Act"
- `tax_category_label`: "Tax Category"
- `tax_category_life_insurance`: "Life Insurance"
- `tax_category_health_insurance`: "Health Insurance"
- `tax_category_medical`: "Medical Expenses"
- `tax_category_retirement`: "Retirement / CIT / SSF"
- `tax_category_donation`: "Donation"
- `tax_ceiling_reached`: "Ceiling reached: Rs. %1$s"
- `tax_summary_title`: "Tax Deductions (FY %1$s)"

---

## 5. Testing & Verification Plan

1. **Migration & DB Tests**:
   - `Migration6To7Test`: verifies table creation, column additions, and data preservation from schema 6 to 7.
   - `UtilityAccountDaoTest`: verifies CRUD operations and reactive queries.
2. **Repository & Logic Tests**:
   - `UtilityRepositoryTest`: verify storage, updates, deletion.
   - `TaxDeductionCalculatorTest`: verify Nepali fiscal year boundary matching (Shrawan 1 to Ashad end) and legal ceiling capping.
   - `BackupRoundtripTest`: verify v7 backup format and backward compatibility with v1-v6 archives.
3. **Build & Quality Gates**:
   - `rtk gradlew testDebugUnitTest`
   - `rtk gradlew lintDebug`
   - `bash scripts/check.sh`
   - `rtk gradlew assembleDebug`
