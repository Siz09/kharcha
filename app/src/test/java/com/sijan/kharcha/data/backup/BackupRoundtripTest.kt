package com.sijan.kharcha.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.db.entity.CategoryEntity
import com.sijan.kharcha.data.db.entity.LedgerEntryEntity
import com.sijan.kharcha.domain.model.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupRoundtripTest {

    private lateinit var db: AppDatabase
    private lateinit var backupService: BackupService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            db.accountDao().insertAccount(
                com.sijan.kharcha.data.db.entity.AccountEntity(
                    id = 1L,
                    name = "Cash in Hand",
                    type = com.sijan.kharcha.domain.model.AccountType.CASH,
                    openingBalancePaisa = 0L
                )
            )
        }
        backupService = BackupServiceImpl(context, db, db.categoryDao(), db.ledgerDao(), db.accountDao(), db.udharDao(), db.savingsGoalDao(), db.utilityAccountDao(), JsonBackupSerializer())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun roundtripBackupRestoresExactData() = runBlocking {
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Groceries", type = EntryType.EXPENSE))
        val entryId = db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 125050L, // Rs. 1,250.50
                type = EntryType.EXPENSE,
                categoryId = catId,
                note = "Weekly groceries",
                date = 1718000000000L,
                source = "receipt",
                transactionId = "TXN_778899"
            )
        )

        // 1. Export backup
        val outStream = ByteArrayOutputStream()
        val exportResult = backupService.exportBackup(outStream)
        assertTrue(exportResult.isSuccess)
        assertEquals(1, exportResult.getOrThrow())

        // 2. Wipe database
        db.ledgerDao().deleteEntry(db.ledgerDao().getEntryById(entryId).first()!!)
        db.categoryDao().deleteCategory(db.categoryDao().getAllCategories().first()[0])
        assertEquals(0, db.ledgerDao().getAllEntries().first().size)
        assertEquals(0, db.categoryDao().getAllCategories().first().size)

        // 3. Restore backup
        val inStream = ByteArrayInputStream(outStream.toByteArray())
        val restoreResult = backupService.restoreBackup(inStream)
        assertTrue(restoreResult.isSuccess)

        val summary = restoreResult.getOrThrow()
        assertEquals(1, summary.categoryCount)
        assertEquals(1, summary.entryCount)

        // 4. Assert 1:1 fidelity
        val restoredCategories = db.categoryDao().getAllCategories().first()
        val restoredEntries = db.ledgerDao().getAllEntries().first()
        assertEquals(1, restoredCategories.size)
        assertEquals("Groceries", restoredCategories[0].name)
        assertEquals(1, restoredEntries.size)
        assertEquals(125050L, restoredEntries[0].amountPaisa)
        assertEquals("TXN_778899", restoredEntries[0].transactionId)
        assertEquals("Weekly groceries", restoredEntries[0].note)
    }

    @Test
    fun restoreAbortsOnCorruptDataAndLeavesExistingIntact() = runBlocking {
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Initial", type = EntryType.INCOME))
        db.ledgerDao().insertEntry(
            LedgerEntryEntity(amountPaisa = 50000L, type = EntryType.INCOME, categoryId = catId, date = 1000L)
        )

        val initialEntryCount = db.ledgerDao().getAllEntries().first().size
        val initialCategoryCount = db.categoryDao().getAllCategories().first().size

        // Attempt restore with corrupt JSON stream
        val corruptStream = ByteArrayInputStream("{ broken json }".toByteArray())
        val result = backupService.restoreBackup(corruptStream)

        assertFalse(result.isSuccess)

        // Assert database remains completely untouched
        assertEquals(initialCategoryCount, db.categoryDao().getAllCategories().first().size)
        assertEquals(initialEntryCount, db.ledgerDao().getAllEntries().first().size)
    }

    @Test
    fun roundtripBackupRestoresUdharsAndSettlements() = runBlocking {
        val udharId = db.udharDao().insertUdhar(
            com.sijan.kharcha.data.db.entity.UdharEntity(
                personName = "Bikash Thapa",
                phoneNumber = "9800000000",
                type = "LENT",
                amountPaisa = 100000L,
                settledPaisa = 30000L,
                dueDate = 1800000000000L,
                notes = "Personal loan",
                status = "PARTIAL",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        db.udharDao().insertSettlement(
            com.sijan.kharcha.data.db.entity.UdharSettlementEntity(
                udharId = udharId,
                amountPaisa = 30000L,
                dateEpochMillis = 2000L,
                notes = "First payment",
                createdAt = 2000L
            )
        )

        val outStream = ByteArrayOutputStream()
        val exportResult = backupService.exportBackup(outStream)
        assertTrue(exportResult.isSuccess)

        // Clear DB
        db.udharDao().deleteAllSettlements()
        db.udharDao().deleteAllUdhars()
        assertEquals(0, db.udharDao().getAllUdhars().first().size)
        assertEquals(0, db.udharDao().getAllSettlements().first().size)

        // Restore
        val inStream = ByteArrayInputStream(outStream.toByteArray())
        val restoreResult = backupService.restoreBackup(inStream)
        assertTrue(restoreResult.isSuccess)

        val restoredUdhars = db.udharDao().getAllUdhars().first()
        val restoredSettlements = db.udharDao().getAllSettlements().first()

        assertEquals(1, restoredUdhars.size)
        assertEquals("Bikash Thapa", restoredUdhars[0].personName)
        assertEquals(100000L, restoredUdhars[0].amountPaisa)
        assertEquals(30000L, restoredUdhars[0].settledPaisa)
        assertEquals("PARTIAL", restoredUdhars[0].status)

        assertEquals(1, restoredSettlements.size)
        assertEquals(30000L, restoredSettlements[0].amountPaisa)
        assertEquals("First payment", restoredSettlements[0].notes)
    }

    @Test
    fun roundtripBackupRestoresSavingsGoalsAndContributions() = runBlocking {
        val goalId = db.savingsGoalDao().insertGoal(
            com.sijan.kharcha.data.db.entity.SavingsGoalEntity(
                name = "Dashain Vacation",
                targetAmountPaisa = 10000000L,
                savedAmountPaisa = 4000000L,
                targetDateEpochMillis = 1800000000000L,
                notes = "Trip to Pokhara",
                isCompleted = false,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        db.savingsGoalDao().insertContribution(
            com.sijan.kharcha.data.db.entity.SavingsContributionEntity(
                goalId = goalId,
                amountPaisa = 4000000L,
                dateEpochMillis = 2000L,
                notes = "First deposit",
                createdAt = 2000L
            )
        )

        val outStream = ByteArrayOutputStream()
        val exportResult = backupService.exportBackup(outStream)
        assertTrue(exportResult.isSuccess)

        // Clear DB
        db.savingsGoalDao().deleteAllContributions()
        db.savingsGoalDao().deleteAllGoals()
        assertEquals(0, db.savingsGoalDao().getAllGoals().first().size)
        assertEquals(0, db.savingsGoalDao().getAllContributions().first().size)

        // Restore
        val inStream = ByteArrayInputStream(outStream.toByteArray())
        val restoreResult = backupService.restoreBackup(inStream)
        assertTrue(restoreResult.isSuccess)

        val restoredGoals = db.savingsGoalDao().getAllGoals().first()
        val restoredContributions = db.savingsGoalDao().getAllContributions().first()

        assertEquals(1, restoredGoals.size)
        assertEquals("Dashain Vacation", restoredGoals[0].name)
        assertEquals(10000000L, restoredGoals[0].targetAmountPaisa)
        assertEquals(4000000L, restoredGoals[0].savedAmountPaisa)
        assertEquals("Trip to Pokhara", restoredGoals[0].notes)
        assertFalse(restoredGoals[0].isCompleted)

        assertEquals(1, restoredContributions.size)
        assertEquals(4000000L, restoredContributions[0].amountPaisa)
        assertEquals("First deposit", restoredContributions[0].notes)
    }

    @Test
    fun roundtripUtilityAccountsAndTaxTags() = runBlocking {
        val catId = db.categoryDao().insertCategory(CategoryEntity(name = "Utilities", type = EntryType.EXPENSE))

        // Insert Tax deductible entry
        db.ledgerDao().insertEntry(
            LedgerEntryEntity(
                amountPaisa = 4000000L,
                type = EntryType.EXPENSE,
                categoryId = catId,
                accountId = 1L,
                note = "Life Insurance",
                date = 1761800000000L,
                isTaxDeductible = true,
                taxCategory = com.sijan.kharcha.domain.model.TaxCategory.LIFE_INSURANCE
            )
        )

        // Insert Utility Account
        db.utilityAccountDao().insertUtilityAccount(
            com.sijan.kharcha.data.db.entity.UtilityAccountEntity(
                utilityType = com.sijan.kharcha.domain.model.UtilityType.NEA,
                providerName = "NEA",
                consumerId = "012.34.567",
                counterOrBranch = "Ratnapark",
                accountHolderName = "Sijan"
            )
        )

        val outStream = ByteArrayOutputStream()
        val exportResult = backupService.exportBackup(outStream)
        assertTrue(exportResult.isSuccess)

        // Clear DB
        db.ledgerDao().deleteAllEntries()
        db.utilityAccountDao().deleteAllUtilityAccounts()

        // Restore
        val inStream = ByteArrayInputStream(outStream.toByteArray())
        val restoreResult = backupService.restoreBackup(inStream)
        assertTrue(restoreResult.isSuccess)

        val restoredEntries = db.ledgerDao().getAllEntries().first()
        assertEquals(1, restoredEntries.size)
        assertTrue(restoredEntries[0].isTaxDeductible)
        assertEquals(com.sijan.kharcha.domain.model.TaxCategory.LIFE_INSURANCE, restoredEntries[0].taxCategory)

        val restoredUtilities = db.utilityAccountDao().getAllUtilityAccounts().first()
        assertEquals(1, restoredUtilities.size)
        assertEquals(com.sijan.kharcha.domain.model.UtilityType.NEA, restoredUtilities[0].utilityType)
        assertEquals("012.34.567", restoredUtilities[0].consumerId)
        assertEquals("Ratnapark", restoredUtilities[0].counterOrBranch)
    }
}


