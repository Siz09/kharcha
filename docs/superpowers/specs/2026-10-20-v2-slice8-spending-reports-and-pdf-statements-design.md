# Kharcha v2 Slice 8: Spending Reports & Offline PDF Statements Design Spec

**Date**: 2026-10-20  
**Status**: APPROVED  
**Author**: Sijan & Antigravity  
**Target Milestone**: v2.0 (Slice 8)

---

## 1. Overview & Goals

Kharcha users need deeper visibility into where their money goes over time, plus the ability to export an official, printable monthly financial statement for record-keeping, tax filings, or family accounting.

### Goals
1. **Month-over-Month (MoM) Trend Analytics**:
   - Compare current month total spending against previous month total spending.
   - Category-by-category delta (paisa difference and percentage change).
   - Clear visual feedback: green indicates reduced spending, amber/red indicates increased expenditure.
2. **Day-of-Week Spend Patterns**:
   - Aggregate spending by weekday (Sunday through Saturday).
   - Custom Jetpack Compose Canvas bar visualizer displaying spend concentration (e.g. Saturday shopping peaks in Nepal).
3. **Top Merchants & Peak Spend Days**:
   - Ranked list of top merchants and highest single-day expense events for the selected month.
4. **On-Device Printable PDF Statement**:
   - Deterministic, 100% offline PDF statement generator using standard Android `android.graphics.pdf.PdfDocument` (zero third-party PDF libraries).
   - Formatted for standard A4 portrait printing with high-contrast, clean typography.
   - Dual AD / BS (Bikram Sambat) date headers.
   - Complete transaction ledger and category summary table.
   - Share / Print action via Android `FileProvider` and SAF document export (`application/pdf`).
5. **Impeccable & Animotion Visual Standards**:
   - Custom Canvas charts with accessible labels and tooltips.
   - Tinted slate/cream surfaces, no pure black/gray, minimum 48dp touch targets.

### Non-Goals
- Cloud reporting or remote analytics (strictly 100% offline).
- Complex stock/investment portfolio charts (out of scope for Kharcha).

---

## 2. Architecture & Data Flow

### 2.1 Analytics & Domain Models (`domain/model/`)

```kotlin
data class MonthOverMonthComparison(
    val currentMonthTotalExpensePaisa: Long,
    val previousMonthTotalExpensePaisa: Long,
    val netDifferencePaisa: Long,
    val percentageChange: Float,
    val categoryDeltas: List<CategorySpendDelta>
)

data class CategorySpendDelta(
    val categoryId: Long,
    val categoryName: String,
    val currentMonthPaisa: Long,
    val previousMonthPaisa: Long,
    val differencePaisa: Long,
    val percentageChange: Float
)

data class DayOfWeekSpend(
    val dayOfWeek: java.time.DayOfWeek,
    val totalExpensePaisa: Long,
    val transactionCount: Int
)

data class SpendingAnalyticsSummary(
    val monthStartEpochUtc: Long,
    val monthEndEpochUtc: Long,
    val momComparison: MonthOverMonthComparison,
    val dayOfWeekSpends: List<DayOfWeekSpend>,
    val topMerchants: List<Pair<String, Long>>,
    val highestSpendDay: Pair<Long, Long>?
)
```

### 2.2 Analytics Repository Layer (`data/repository/AnalyticsRepository.kt`)
- `AnalyticsRepository` calculates analytics directly using existing `LedgerEntryDao` without requiring schema migrations:
  - Queries entries for `[startOfMonth, endOfMonth)` and `[startOfPreviousMonth, endOfPreviousMonth)`.
  - Calculates deterministic totals in paisa (`Long`).
  - Groups by category and day-of-week.

### 2.3 Offline PDF Generation Service (`data/reports/PdfStatementGenerator.kt`)
- Standard Android `android.graphics.pdf.PdfDocument`.
- Page setup: A4 size (`595 x 842` points at 72 DPI).
- Rendering layout:
  - **Header**: Kharcha logo/badge, title "MONTHLY FINANCIAL STATEMENT", Bikram Sambat & Gregorian month range, generation date timestamp.
  - **KPI Banner**: Inflow (`Rs. #`), Outflow (`Rs. #`), Net Savings (`Rs. #`), Saving Index (`%`).
  - **Category Summary Table**: Grid with Category, Transaction Count, Total Amount (paisa converted to Rs.).
  - **Transaction Ledger**: Paginated table listing Date, Description/Merchant, Category, Account, Amount.
  - **Footer**: Page numbers and "Kharcha — 100% Offline Personal Finance Tracker".
- Output:
  - Writes to app-private cache directory: `cache/statements/Kharcha_Statement_<month>.pdf`.
  - Content URI served via `androidx.core.content.FileProvider`.

### 2.4 Presentation Layer (`ui/reports/`)
- `SpendingReportsScreen`:
  - Month navigation bar (Previous / Next month) with dual AD/BS label.
  - MoM Delta Card: overall spending delta badge (+12% vs last month).
  - Day-of-Week Distribution Chart: Custom Canvas bar chart with Sunday-Saturday columns.
  - Category Comparison List: Expandable rows showing current vs previous month.
  - Top Merchants Card: Top 5 merchants with percentage of monthly budget.
  - Primary Action Floating/Header Button: "Export PDF Statement".
- Navigation:
  - Linked from `SummaryScreen` ("View Deep Analytics") and Navigation drawer/tab.

---

## 3. Impeccable Design & Animotion Guidelines

- **Chart Canvas**:
  - Soft rounded bar tops (4dp radius).
  - Tonal contrast bars: forest green for savings, muted coral/terracotta for spending increases.
  - Animated bar heights using `animateFloatAsState(..., tween(durationMillis = 400, easing = FastOutSlowInEasing))`.
- **Touch & Accessibility**:
  - Tapping a day bar displays an accessible floating pill with exact paisa amount.
  - TalkBack accessibility strings describing bar percentages and values clearly.

---

## 4. String Resources (`res/values/strings.xml`)

- `reports_title`: "Spending Analytics"
- `reports_mom_comparison_title`: "Month-over-Month Trends"
- `reports_spending_increase`: "%1$s higher than last month"
- `reports_spending_decrease`: "%1$s lower than last month"
- `reports_day_of_week_title`: "Day-of-Week Spending Patterns"
- `reports_top_merchants_title`: "Top Merchants"
- `reports_export_pdf_button`: "Export PDF Statement"
- `reports_generating_pdf`: "Generating statement..."
- `reports_pdf_ready`: "Statement generated successfully"
- `reports_pdf_share_chooser`: "Share Monthly Statement"

---

## 5. Testing & Verification Plan

1. **Unit Tests**:
   - `AnalyticsRepositoryTest`: verify MoM delta, percentage math, day-of-week aggregations, and edge cases (zero spending in previous month).
   - `PdfStatementGeneratorTest`: verify document builds valid pages, pagination splits correctly, and writes non-empty file without crash.
2. **Build & Quality Gates**:
   - `rtk gradlew testDebugUnitTest`
   - `rtk gradlew lintDebug`
   - `bash scripts/check.sh`
   - `rtk gradlew assembleDebug`
