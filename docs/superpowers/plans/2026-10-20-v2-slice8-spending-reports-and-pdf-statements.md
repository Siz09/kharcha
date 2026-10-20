# Kharcha v2 Slice 8: Spending Reports & Offline PDF Statements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement month-over-month trend analytics, day-of-week spend patterns with custom Canvas bar charts, and on-device printable A4 monthly PDF statement generation via `android.graphics.pdf.PdfDocument`.

**Architecture:** `AnalyticsRepository` computes deterministic spending analytics from existing Room ledger records without schema migration. `PdfStatementGenerator` uses native Android Canvas drawing to produce high-contrast, printable A4 PDF statements served via `FileProvider`. `SpendingReportsScreen` renders the charts and insights with Impeccable styling.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose Material 3 Canvas, Android PdfDocument, FileProvider, Hilt.

**Spec:** [docs/superpowers/specs/2026-10-20-v2-slice8-spending-reports-and-pdf-statements-design.md](file:///D:/vscode/kharcha/docs/superpowers/specs/2026-10-20-v2-slice8-spending-reports-and-pdf-statements-design.md)

## Global Constraints

- 100% offline: zero network permissions (`android.permission.INTERNET` forbidden).
- Zero floating point for money: all amounts strictly `Long` in paisa.
- All user-facing strings must reside in `res/values/strings.xml`.
- Follow Impeccable design: custom Canvas rendering (no external chart libraries), high-contrast accessible typography, tinted slate/cream surfaces, minimum 48dp touch targets.
- Always prefix commands with `rtk`.

---

### Task 1: Domain Models & Analytics Repository

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/domain/model/SpendingAnalytics.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/repository/AnalyticsRepository.kt`
- Create: `app/src/main/java/com/sijan/kharcha/data/repository/AnalyticsRepositoryImpl.kt`
- Modify: `app/src/main/java/com/sijan/kharcha/di/RepositoryModule.kt`
- Test: `app/src/test/java/com/sijan/kharcha/data/repository/AnalyticsRepositoryTest.kt`

**Interfaces:**
- Produces: `MonthOverMonthComparison`, `CategorySpendDelta`, `DayOfWeekSpend`, `SpendingAnalyticsSummary`, `AnalyticsRepository`

- [ ] **Step 1: Create domain models**

Create `app/src/main/java/com/sijan/kharcha/domain/model/SpendingAnalytics.kt`:
```kotlin
package com.sijan.kharcha.domain.model

import java.time.DayOfWeek

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
    val dayOfWeek: DayOfWeek,
    val totalExpensePaisa: Long,
    val transactionCount: Int
)

data class SpendingAnalyticsSummary(
    val year: Int,
    val month: Int,
    val momComparison: MonthOverMonthComparison,
    val dayOfWeekSpends: List<DayOfWeekSpend>,
    val topMerchants: List<Pair<String, Long>>,
    val highestSpendDayDate: Long?,
    val highestSpendDayAmountPaisa: Long
)
```

- [ ] **Step 2: Write failing unit test for AnalyticsRepository**

Create `app/src/test/java/com/sijan/kharcha/data/repository/AnalyticsRepositoryTest.kt`:
```kotlin
package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class AnalyticsRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AnalyticsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AnalyticsRepositoryImpl(db.ledgerDao(), db.categoryDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getMonthlyAnalytics_computesMonthOverMonthCorrectly() = runTest {
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Food", type = EntryType.EXPENSE))

        val currentMonthDate = LocalDate.of(2026, 10, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val prevMonthDate = LocalDate.of(2026, 9, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 300000L, categoryId = catId, type = EntryType.EXPENSE, date = currentMonthDate))
        db.ledgerDao().insertEntry(LedgerEntryEntity(amountPaisa = 200000L, categoryId = catId, type = EntryType.EXPENSE, date = prevMonthDate))

        val summary = repository.getMonthlyAnalytics(2026, 10).first()
        assertEquals(300000L, summary.momComparison.currentMonthTotalExpensePaisa)
        assertEquals(200000L, summary.momComparison.previousMonthTotalExpensePaisa)
        assertEquals(100000L, summary.momComparison.netDifferencePaisa)
        assertEquals(50.0f, summary.momComparison.percentageChange, 0.1f)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.data.repository.AnalyticsRepositoryTest"`  
Expected: FAIL (unresolved classes)

- [ ] **Step 4: Implement AnalyticsRepository and AnalyticsRepositoryImpl**

Create `app/src/main/java/com/sijan/kharcha/data/repository/AnalyticsRepository.kt`:
```kotlin
package com.sijan.kharcha.data.repository

import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    fun getMonthlyAnalytics(year: Int, month: Int): Flow<SpendingAnalyticsSummary>
}
```

Create `app/src/main/java/com/sijan/kharcha/data/repository/AnalyticsRepositoryImpl.kt`:
```kotlin
package com.sijan.kharcha.data.repository

import com.sijan.kharcha.data.db.dao.CategoryDao
import com.sijan.kharcha.data.db.dao.LedgerDao
import com.sijan.kharcha.domain.model.CategorySpendDelta
import com.sijan.kharcha.domain.model.DayOfWeekSpend
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.MonthOverMonthComparison
import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val ledgerDao: LedgerDao,
    private val categoryDao: CategoryDao
) : AnalyticsRepository {

    override fun getMonthlyAnalytics(year: Int, month: Int): Flow<SpendingAnalyticsSummary> {
        val currentStart = LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val currentEnd = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val prevMonthDate = LocalDate.of(year, month, 1).minusMonths(1)
        val prevStart = prevMonthDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val prevEnd = currentStart

        return combine(
            ledgerDao.getEntriesByDateRange(currentStart, currentEnd),
            ledgerDao.getEntriesByDateRange(prevStart, prevEnd),
            categoryDao.getAllCategories()
        ) { currentEntities, prevEntities, categories ->
            val currentExpenses = currentEntities.filter { it.type == EntryType.EXPENSE }
            val prevExpenses = prevEntities.filter { it.type == EntryType.EXPENSE }

            val currentTotal = currentExpenses.sumOf { it.amountPaisa }
            val prevTotal = prevExpenses.sumOf { it.amountPaisa }
            val netDifference = currentTotal - prevTotal

            val percentageChange = if (prevTotal > 0L) {
                ((netDifference.toDouble() / prevTotal.toDouble()) * 100.0).toFloat()
            } else if (currentTotal > 0L) {
                100.0f
            } else 0.0f

            val categoryMap = categories.associateBy { it.id }

            // Category deltas
            val currentByCat = currentExpenses.groupBy { it.categoryId }
            val prevByCat = prevExpenses.groupBy { it.categoryId }
            val allCatIds = (currentByCat.keys + prevByCat.keys).distinct()

            val categoryDeltas = allCatIds.map { catId ->
                val currentCatTotal = currentByCat[catId]?.sumOf { it.amountPaisa } ?: 0L
                val prevCatTotal = prevByCat[catId]?.sumOf { it.amountPaisa } ?: 0L
                val catDiff = currentCatTotal - prevCatTotal
                val catPct = if (prevCatTotal > 0L) {
                    ((catDiff.toDouble() / prevCatTotal.toDouble()) * 100.0).toFloat()
                } else if (currentCatTotal > 0L) {
                    100.0f
                } else 0.0f

                CategorySpendDelta(
                    categoryId = catId,
                    categoryName = categoryMap[catId]?.name ?: "Uncategorized",
                    currentMonthPaisa = currentCatTotal,
                    previousMonthPaisa = prevCatTotal,
                    differencePaisa = catDiff,
                    percentageChange = catPct
                )
            }.sortedByDescending { it.currentMonthPaisa }

            // Day of week spends
            val dayOfWeekMap = DayOfWeek.values().associateWith { mutableListOf<Long>() }
            currentExpenses.forEach { entry ->
                val day = Instant.ofEpochMilli(entry.date).atZone(ZoneId.of("UTC")).dayOfWeek
                dayOfWeekMap[day]?.add(entry.amountPaisa)
            }

            val dayOfWeekSpends = DayOfWeek.values().map { day ->
                val amounts = dayOfWeekMap[day] ?: emptyList()
                DayOfWeekSpend(
                    dayOfWeek = day,
                    totalExpensePaisa = amounts.sum(),
                    transactionCount = amounts.size
                )
            }

            // Top merchants
            val topMerchants = currentExpenses
                .filter { !it.merchant.isNullOrBlank() }
                .groupBy { it.merchant!!.trim() }
                .mapValues { (_, entries) -> entries.sumOf { it.amountPaisa } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)

            // Highest spending day
            val byDay = currentExpenses.groupBy { entry ->
                Instant.ofEpochMilli(entry.date).atZone(ZoneId.of("UTC")).toLocalDate()
            }.mapValues { (_, entries) -> entries.sumOf { it.amountPaisa } }

            val peakDay = byDay.maxByOrNull { it.value }
            val highestDateEpoch = peakDay?.key?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            val highestAmount = peakDay?.value ?: 0L

            SpendingAnalyticsSummary(
                year = year,
                month = month,
                momComparison = MonthOverMonthComparison(
                    currentMonthTotalExpensePaisa = currentTotal,
                    previousMonthTotalExpensePaisa = prevTotal,
                    netDifferencePaisa = netDifference,
                    percentageChange = percentageChange,
                    categoryDeltas = categoryDeltas
                ),
                dayOfWeekSpends = dayOfWeekSpends,
                topMerchants = topMerchants,
                highestSpendDayDate = highestDateEpoch,
                highestSpendDayAmountPaisa = highestAmount
            )
        }
    }
}
```

Modify `app/src/main/java/com/sijan/kharcha/di/RepositoryModule.kt` to bind `AnalyticsRepository`:
```kotlin
    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        impl: AnalyticsRepositoryImpl
    ): AnalyticsRepository
```

- [ ] **Step 5: Run test to verify it passes**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.data.repository.AnalyticsRepositoryTest"`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/domain/model/SpendingAnalytics.kt app/src/main/java/com/sijan/kharcha/data/repository/Analytics* app/src/main/java/com/sijan/kharcha/di/RepositoryModule.kt app/src/test/java/com/sijan/kharcha/data/repository/AnalyticsRepositoryTest.kt
rtk git commit -m "feat: implement AnalyticsRepository for month-over-month and day-of-week analytics"
```

---

### Task 2: String Resources for Spending Analytics & Statements

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: Strings for analytics screen, day abbreviations, and PDF export.

- [ ] **Step 1: Add strings to strings.xml**

Add to `app/src/main/res/values/strings.xml`:
```xml
    <!-- Spending Reports & PDF Export -->
    <string name="reports_title">Spending Analytics</string>
    <string name="reports_mom_title">Month-over-Month Comparison</string>
    <string name="reports_spending_increase">+%1$s (%2$.1f%%) vs last month</string>
    <string name="reports_spending_decrease">-%1$s (%2$.1f%%) vs last month</string>
    <string name="reports_spending_equal">Same as last month</string>
    <string name="reports_day_of_week_title">Weekly Spending Concentration</string>
    <string name="reports_top_merchants_title">Top Spending Merchants</string>
    <string name="reports_peak_day_title">Peak Spending Day</string>
    <string name="reports_export_pdf_button">Export PDF Statement</string>
    <string name="reports_generating_pdf">Generating offline statement...</string>
    <string name="reports_pdf_ready">Statement generated successfully</string>
    <string name="reports_pdf_share_title">Kharcha Monthly Financial Statement</string>
    <string name="reports_summary_view_analytics">View Deep Analytics</string>
```

- [ ] **Step 2: Commit**

```bash
rtk git add app/src/main/res/values/strings.xml
rtk git commit -m "chore: add string resources for spending reports and PDF statement export"
```

---

### Task 3: Offline PDF Statement Generator Service

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/data/reports/PdfStatementGenerator.kt`
- Test: `app/src/test/java/com/sijan/kharcha/data/reports/PdfStatementGeneratorTest.kt`

**Interfaces:**
- Produces: `PdfStatementGenerator.generateStatement(year: Int, month: Int, ...): File`

- [ ] **Step 1: Write unit test for PdfStatementGenerator**

Create `app/src/test/java/com/sijan/kharcha/data/reports/PdfStatementGeneratorTest.kt`:
```kotlin
package com.sijan.kharcha.data.reports

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.MonthlySummary
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PdfStatementGeneratorTest {

    @Test
    fun generateStatement_createsValidNonEmptyPdfFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = PdfStatementGenerator(context)

        val summary = MonthlySummary(
            totalIncomePaisa = 5000000L,
            totalExpensePaisa = 2500000L,
            balancePaisa = 2500000L
        )

        val entries = listOf(
            LedgerEntry(
                id = 1L,
                amountPaisa = 150000L,
                type = EntryType.EXPENSE,
                categoryId = 1L,
                date = System.currentTimeMillis(),
                note = "Groceries shopping",
                merchant = "Bhatbhateni"
            )
        )

        val categories = listOf(
            Category(id = 1L, name = "Groceries", type = EntryType.EXPENSE)
        )

        val file = generator.generateMonthlyStatement(
            year = 2026,
            month = 10,
            summary = summary,
            entries = entries,
            categories = categories
        )

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0L)
    }
}
```

- [ ] **Step 2: Implement PdfStatementGenerator**

Create `app/src/main/java/com/sijan/kharcha/data/reports/PdfStatementGenerator.kt`:
```kotlin
package com.sijan.kharcha.data.reports

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.sijan.kharcha.domain.calendar.BikramSambatConverter
import com.sijan.kharcha.domain.model.Category
import com.sijan.kharcha.domain.model.EntryType
import com.sijan.kharcha.domain.model.LedgerEntry
import com.sijan.kharcha.domain.model.Money
import com.sijan.kharcha.domain.model.MonthlySummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfStatementGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun generateMonthlyStatement(
        year: Int,
        month: Int,
        summary: MonthlySummary,
        entries: List<LedgerEntry>,
        categories: List<Category>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 portrait in points
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val categoryMap = categories.associateBy { it.id }

        // Top Banner
        paint.color = Color.parseColor("#1B5E20")
        canvas.drawRect(0f, 0f, 595f, 60f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KHARCHA — FINANCIAL STATEMENT", 36f, 38f, paint)

        // Month & Dual Dates
        val monthDate = LocalDate.of(year, month, 1)
        val bsDate = BikramSambatConverter.toBs(monthDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
        val monthLabel = "${monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))} (BS ${bsDate.monthName} ${bsDate.year})"

        paint.color = Color.parseColor("#1F2937")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Statement Period: $monthLabel", 36f, 90f, paint)

        val df = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#6B7280")
        canvas.drawText("Generated on: ${df.format(Date())} (100% Offline)", 36f, 106f, paint)

        // Financial KPI Block
        paint.color = Color.parseColor("#F3F4F6")
        canvas.drawRoundRect(36f, 120f, 559f, 180f, 8f, 8f, paint)

        paint.color = Color.parseColor("#374151")
        paint.textSize = 10f
        canvas.drawText("TOTAL INCOME", 56f, 142f, paint)
        canvas.drawText("TOTAL EXPENSE", 186f, 142f, paint)
        canvas.drawText("NET SAVINGS", 316f, 142f, paint)
        canvas.drawText("SAVINGS RATE", 446f, 142f, paint)

        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#15803D")
        canvas.drawText(Money.formatPaisa(summary.totalIncomePaisa), 56f, 164f, paint)

        paint.color = Color.parseColor("#DC2626")
        canvas.drawText(Money.formatPaisa(summary.totalExpensePaisa), 186f, 164f, paint)

        paint.color = if (summary.balancePaisa >= 0L) Color.parseColor("#15803D") else Color.parseColor("#DC2626")
        canvas.drawText(Money.formatPaisa(summary.balancePaisa), 316f, 164f, paint)

        val rate = if (summary.totalIncomePaisa > 0L) {
            ((summary.balancePaisa.toDouble() / summary.totalIncomePaisa.toDouble()) * 100).toInt()
        } else 0
        paint.color = Color.parseColor("#1F2937")
        canvas.drawText("$rate%", 446f, 164f, paint)

        // Table Header
        var yPos = 215f
        paint.color = Color.parseColor("#E5E7EB")
        canvas.drawRect(36f, yPos, 559f, yPos + 24f, paint)

        paint.color = Color.parseColor("#374151")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATE", 46f, yPos + 16f, paint)
        canvas.drawText("CATEGORY / MERCHANT", 120f, yPos + 16f, paint)
        canvas.drawText("NOTE", 300f, yPos + 16f, paint)
        canvas.drawText("AMOUNT", 480f, yPos + 16f, paint)

        yPos += 36f
        val itemDateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (entry in entries.take(30)) {
            paint.color = Color.parseColor("#4B5563")
            canvas.drawText(itemDateFormat.format(Date(entry.date)), 46f, yPos, paint)

            val catName = categoryMap[entry.categoryId]?.name ?: "General"
            val title = if (!entry.merchant.isNullOrBlank()) "$catName (${entry.merchant})" else catName
            canvas.drawText(title.take(28), 120f, yPos, paint)
            canvas.drawText(entry.note.take(28), 300f, yPos, paint)

            if (entry.type == EntryType.INCOME) {
                paint.color = Color.parseColor("#15803D")
                canvas.drawText("+${Money.formatPaisa(entry.amountPaisa)}", 480f, yPos, paint)
            } else {
                paint.color = Color.parseColor("#DC2626")
                canvas.drawText("-${Money.formatPaisa(entry.amountPaisa)}", 480f, yPos, paint)
            }

            yPos += 18f
            if (yPos > 790f) break
        }

        // Footer
        paint.color = Color.parseColor("#9CA3AF")
        paint.textSize = 9f
        canvas.drawText("Kharcha Personal Finance Tracker • 100% Offline • Zero Cloud • Encrypted Local Storage", 36f, 815f, paint)

        document.finishPage(page)

        val cacheDir = File(context.cacheDir, "statements").apply { mkdirs() }
        val outputFile = File(cacheDir, "Kharcha_Statement_${year}_$month.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }
}
```

- [ ] **Step 3: Run test to verify it passes**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.data.reports.PdfStatementGeneratorTest"`  
Expected: PASS

- [ ] **Step 4: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/data/reports/PdfStatementGenerator.kt app/src/test/java/com/sijan/kharcha/data/reports/PdfStatementGeneratorTest.kt
rtk git commit -m "feat: implement on-device printable A4 PDF statement generator"
```

---

### Task 4: Spending Reports ViewModel & Custom Canvas UI

**Files:**
- Create: `app/src/main/java/com/sijan/kharcha/ui/reports/SpendingReportsViewModel.kt`
- Create: `app/src/main/java/com/sijan/kharcha/ui/reports/SpendingReportsScreen.kt`
- Modify: `app/src/main/java/com/sijan/kharcha/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/sijan/kharcha/ui/navigation/KharchaNavHost.kt`
- Modify: `app/src/main/java/com/sijan/kharcha/ui/summary/SummaryScreen.kt`
- Test: `app/src/test/java/com/sijan/kharcha/ui/reports/SpendingReportsViewModelTest.kt`

**Interfaces:**
- Produces: `SpendingReportsViewModel`, `SpendingReportsScreen`, navigation route `Screen.Reports`

- [ ] **Step 1: Write unit test for SpendingReportsViewModel**

Create `app/src/test/java/com/sijan/kharcha/ui/reports/SpendingReportsViewModelTest.kt`:
```kotlin
package com.sijan.kharcha.ui.reports

import com.sijan.kharcha.data.reports.PdfStatementGenerator
import com.sijan.kharcha.data.repository.AnalyticsRepository
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.MonthOverMonthComparison
import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class SpendingReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val analyticsRepo = mock(AnalyticsRepository::class.java)
    private val ledgerRepo = mock(LedgerRepository::class.java)
    private val pdfGenerator = mock(PdfStatementGenerator::class.java)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun monthNavigation_updatesSelectedYearAndMonth() = runTest(testDispatcher) {
        val dummySummary = SpendingAnalyticsSummary(
            year = 2026,
            month = 10,
            momComparison = MonthOverMonthComparison(0L, 0L, 0L, 0f, emptyList()),
            dayOfWeekSpends = emptyList(),
            topMerchants = emptyList(),
            highestSpendDayDate = null,
            highestSpendDayAmountPaisa = 0L
        )
        `when`(analyticsRepo.getMonthlyAnalytics(2026, 10)).thenReturn(flowOf(dummySummary))
        `when`(analyticsRepo.getMonthlyAnalytics(2026, 11)).thenReturn(flowOf(dummySummary.copy(month = 11)))

        val viewModel = SpendingReportsViewModel(analyticsRepo, ledgerRepo, pdfGenerator)
        viewModel.onNextMonth()
        assertEquals(11, viewModel.selectedMonth.value)
        viewModel.onPreviousMonth()
        assertEquals(10, viewModel.selectedMonth.value)
    }
}
```

- [ ] **Step 2: Implement SpendingReportsViewModel**

Create `app/src/main/java/com/sijan/kharcha/ui/reports/SpendingReportsViewModel.kt`:
```kotlin
package com.sijan.kharcha.ui.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijan.kharcha.data.reports.PdfStatementGenerator
import com.sijan.kharcha.data.repository.AnalyticsRepository
import com.sijan.kharcha.data.repository.LedgerRepository
import com.sijan.kharcha.domain.model.SpendingAnalyticsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SpendingReportsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val ledgerRepository: LedgerRepository,
    private val pdfStatementGenerator: PdfStatementGenerator
) : ViewModel() {

    private val now = LocalDate.now()
    val selectedYear = MutableStateFlow(now.year)
    val selectedMonth = MutableStateFlow(now.monthValue)

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    val analyticsSummary: StateFlow<SpendingAnalyticsSummary?> = kotlinx.coroutines.flow.combine(
        selectedYear,
        selectedMonth
    ) { y, m -> Pair(y, m) }.flatMapLatest { (y, m) ->
        analyticsRepository.getMonthlyAnalytics(y, m)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun onPreviousMonth() {
        if (selectedMonth.value == 1) {
            selectedYear.value -= 1
            selectedMonth.value = 12
        } else {
            selectedMonth.value -= 1
        }
    }

    fun onNextMonth() {
        if (selectedMonth.value == 12) {
            selectedYear.value += 1
            selectedMonth.value = 1
        } else {
            selectedMonth.value += 1
        }
    }

    fun exportPdf(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val year = selectedYear.value
                val month = selectedMonth.value
                val summary = ledgerRepository.getMonthlySummary(year, month).first()
                val categories = ledgerRepository.getAllCategories().first()

                val startEpoch = LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val endEpoch = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val entries = ledgerRepository.getEntriesByDateRange(startEpoch, endEpoch).first()

                val file = withContext(Dispatchers.IO) {
                    pdfStatementGenerator.generateMonthlyStatement(
                        year = year,
                        month = month,
                        summary = summary,
                        entries = entries,
                        categories = categories
                    )
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Monthly Statement"))
            } finally {
                _isGeneratingPdf.value = false
                onComplete()
            }
        }
    }
}
```

- [ ] **Step 3: Implement SpendingReportsScreen with custom Canvas bar chart**

Create `app/src/main/java/com/sijan/kharcha/ui/reports/SpendingReportsScreen.kt`:
```kotlin
package com.sijan.kharcha.ui.reports

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sijan.kharcha.R
import com.sijan.kharcha.domain.calendar.BikramSambatConverter
import com.sijan.kharcha.domain.model.Money
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingReportsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpendingReportsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val summary by viewModel.analyticsSummary.collectAsStateWithLifecycle()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()

    val monthDate = LocalDate.of(year, month, 1)
    val bsDate = BikramSambatConverter.toBs(monthDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val monthLabel = "${monthDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))} (BS ${bsDate.monthName} ${bsDate.year})"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.onPreviousMonth() }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.onNextMonth() }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            // MoM Comparison Card
            item {
                summary?.momComparison?.let { mom ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.reports_mom_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val isIncrease = mom.netDifferencePaisa > 0L
                            val deltaColor = if (isIncrease) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            val deltaText = if (isIncrease) {
                                stringResource(R.string.reports_spending_increase, Money.formatPaisa(mom.netDifferencePaisa), mom.percentageChange)
                            } else if (mom.netDifferencePaisa < 0L) {
                                stringResource(R.string.reports_spending_decrease, Money.formatPaisa(-mom.netDifferencePaisa), -mom.percentageChange)
                            } else {
                                stringResource(R.string.reports_spending_equal)
                            }

                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = deltaColor
                            )
                        }
                    }
                }
            }

            // Day of Week Canvas Bar Chart
            item {
                summary?.let { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.reports_day_of_week_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val maxSpend = (s.dayOfWeekSpends.maxOfOrNull { it.totalExpensePaisa } ?: 1L).coerceAtLeast(1L)
                            val barColor = MaterialTheme.colorScheme.primary

                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val barWidth = size.width / 7f
                                    val maxHeight = size.height - 30f

                                    s.dayOfWeekSpends.forEachIndexed { index, daySpend ->
                                        val heightRatio = (daySpend.totalExpensePaisa.toFloat() / maxSpend.toFloat()).coerceIn(0f, 1f)
                                        val barHeight = (heightRatio * maxHeight).coerceAtLeast(6f)
                                        val left = index * barWidth + (barWidth * 0.2f)
                                        val top = maxHeight - barHeight

                                        drawRoundRect(
                                            color = barColor,
                                            topLeft = Offset(left, top),
                                            size = Size(barWidth * 0.6f, barHeight),
                                            cornerRadius = CornerRadius(6f, 6f)
                                        )
                                    }
                                }
                            }

                            // Day labels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                labels.forEach { label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Merchants
            item {
                summary?.let { s ->
                    if (s.topMerchants.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.reports_top_merchants_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                s.topMerchants.forEach { (merchant, amountPaisa) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = merchant, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = Money.formatPaisa(amountPaisa),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Export PDF Statement Button
            item {
                Button(
                    onClick = { viewModel.exportPdf(context) },
                    enabled = !isGeneratingPdf,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.reports_generating_pdf))
                    } else {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.reports_export_pdf_button), fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
```

- [ ] **Step 4: Wire navigation in Screen.kt and KharchaNavHost.kt**

In `Screen.kt`:
```kotlin
    data object Reports : Screen(
        route = "reports",
        labelRes = R.string.reports_title,
        icon = Icons.Default.PieChart
    )
```

In `KharchaNavHost.kt`:
```kotlin
        composable(Screen.Reports.route) {
            SpendingReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
```

In `SummaryScreen.kt`:
Add "View Deep Analytics" button/card that calls `onNavigateToReports()`.

- [ ] **Step 5: Run unit tests**

Run: `rtk gradlew testDebugUnitTest --tests "com.sijan.kharcha.ui.reports.SpendingReportsViewModelTest"`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
rtk git add app/src/main/java/com/sijan/kharcha/ui/reports/ app/src/main/java/com/sijan/kharcha/ui/navigation/ app/src/main/java/com/sijan/kharcha/ui/summary/ app/src/test/java/com/sijan/kharcha/ui/reports/
rtk git commit -m "feat: implement SpendingReportsScreen with Canvas charts and statement export"
```

---

### Task 5: Full Verification Gates Run

- [ ] **Step 1: Run all unit tests**
Run: `rtk gradlew testDebugUnitTest`

- [ ] **Step 2: Run Android Lint**
Run: `rtk gradlew lintDebug`

- [ ] **Step 3: Run repository verification guards**
Run: `node scripts/guard.mjs`

- [ ] **Step 4: Build debug APK**
Run: `rtk gradlew assembleDebug`
