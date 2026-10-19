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
        backupService = BackupServiceImpl(db, db.categoryDao(), db.ledgerDao(), db.accountDao(), db.udharDao(), JsonBackupSerializer())
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
}

