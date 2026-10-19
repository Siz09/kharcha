package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.domain.model.UdharStatus
import com.sijan.kharcha.domain.model.UdharType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UdharRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: UdharRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UdharRepositoryImpl(db, db.udharDao(), db.auditLogDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun createUdharValidatesAndStoresCorrectly() = runTest {
        val result = repository.createUdhar(
            personName = "Ramesh Sharma",
            phoneNumber = "9851000000",
            type = UdharType.LENT,
            amountPaisa = 500000L, // Rs. 5,000
            dueDate = 1800000000000L,
            notes = "Emergency hospital loan"
        )

        assertTrue(result.isSuccess)
        val id = result.getOrThrow()

        val udhar = repository.getUdharById(id).first()
        assertNotNull(udhar)
        assertEquals("Ramesh Sharma", udhar?.personName)
        assertEquals("9851000000", udhar?.phoneNumber)
        assertEquals(UdharType.LENT, udhar?.type)
        assertEquals(500000L, udhar?.amountPaisa)
        assertEquals(0L, udhar?.settledPaisa)
        assertEquals(500000L, udhar?.remainingPaisa)
        assertEquals(UdharStatus.PENDING, udhar?.status)
    }

    @Test
    fun createUdharRejectsInvalidInput() = runTest {
        val blankName = repository.createUdhar(
            personName = "   ",
            phoneNumber = null,
            type = UdharType.BORROWED,
            amountPaisa = 100000L,
            dueDate = null,
            notes = ""
        )
        assertTrue(blankName.isFailure)

        val zeroAmount = repository.createUdhar(
            personName = "Sita",
            phoneNumber = null,
            type = UdharType.BORROWED,
            amountPaisa = 0L,
            dueDate = null,
            notes = ""
        )
        assertTrue(zeroAmount.isFailure)
    }

    @Test
    fun recordSettlementUpdatesStatusAndSettledAmount() = runTest {
        val createResult = repository.createUdhar(
            personName = "Hari",
            phoneNumber = null,
            type = UdharType.LENT,
            amountPaisa = 100000L, // Rs. 1,000
            dueDate = null,
            notes = ""
        )
        val udharId = createResult.getOrThrow()

        // Partial settlement of Rs. 400 (40000 paisa)
        val settleResult1 = repository.recordSettlement(
            udharId = udharId,
            amountPaisa = 40000L,
            dateEpochMillis = 2000L,
            notes = "Cash installment 1"
        )
        assertTrue(settleResult1.isSuccess)

        val partial = repository.getUdharById(udharId).first()
        assertEquals(40000L, partial?.settledPaisa)
        assertEquals(60000L, partial?.remainingPaisa)
        assertEquals(UdharStatus.PARTIAL, partial?.status)

        // Full remaining settlement of Rs. 600 (60000 paisa)
        val settleResult2 = repository.recordSettlement(
            udharId = udharId,
            amountPaisa = 60000L,
            dateEpochMillis = 3000L,
            notes = "Final cash settlement"
        )
        assertTrue(settleResult2.isSuccess)

        val settled = repository.getUdharById(udharId).first()
        assertEquals(100000L, settled?.settledPaisa)
        assertEquals(0L, settled?.remainingPaisa)
        assertEquals(UdharStatus.SETTLED, settled?.status)
        assertTrue(settled?.isFullySettled == true)

        val settlements = repository.getSettlementsForUdhar(udharId).first()
        assertEquals(2, settlements.size)
    }

    @Test
    fun deleteSettlementRecalculatesStatus() = runTest {
        val udharId = repository.createUdhar(
            personName = "Kiran",
            phoneNumber = null,
            type = UdharType.BORROWED,
            amountPaisa = 200000L, // Rs. 2,000
            dueDate = null,
            notes = ""
        ).getOrThrow()

        val sId1 = repository.recordSettlement(udharId, 100000L, 1000L, null).getOrThrow()
        val sId2 = repository.recordSettlement(udharId, 100000L, 2000L, null).getOrThrow()

        val full = repository.getUdharById(udharId).first()
        assertEquals(UdharStatus.SETTLED, full?.status)

        // Delete second settlement
        repository.deleteSettlement(sId2)

        val reverted = repository.getUdharById(udharId).first()
        assertEquals(100000L, reverted?.settledPaisa)
        assertEquals(UdharStatus.PARTIAL, reverted?.status)

        // Delete first settlement
        repository.deleteSettlement(sId1)

        val initial = repository.getUdharById(udharId).first()
        assertEquals(0L, initial?.settledPaisa)
        assertEquals(UdharStatus.PENDING, initial?.status)
    }

    @Test
    fun udharSummaryCalculatesLentBorrowedAndNetAccurately() = runTest {
        // Lent: 5,000 total, 2,000 settled -> 3,000 remaining receivable
        val id1 = repository.createUdhar("Prakash", null, UdharType.LENT, 500000L, null, "").getOrThrow()
        repository.recordSettlement(id1, 200000L, 1000L, null)

        // Borrowed: 1,500 total, 500 settled -> 1,000 remaining payable
        val id2 = repository.createUdhar("Gita", null, UdharType.BORROWED, 150000L, null, "").getOrThrow()
        repository.recordSettlement(id2, 50000L, 1000L, null)

        val summary = repository.getUdharSummary().first()
        assertEquals(300000L, summary.totalLentPaisa)        // Rs. 3,000
        assertEquals(100000L, summary.totalBorrowedPaisa)    // Rs. 1,000
        assertEquals(250000L, summary.totalSettledPaisa)     // Rs. 2,500
        assertEquals(200000L, summary.netReceivablePaisa)    // Rs. 3,000 - Rs. 1,000 = Rs. 2,000
    }

    @Test
    fun deleteUdharCascadesAndRemovesSettlements() = runTest {
        val id = repository.createUdhar("Deepak", null, UdharType.LENT, 50000L, null, "").getOrThrow()
        repository.recordSettlement(id, 20000L, 1000L, null)

        repository.deleteUdhar(id)

        assertNull(repository.getUdharById(id).first())
        val settlements = repository.getSettlementsForUdhar(id).first()
        assertTrue(settlements.isEmpty())
    }
}
