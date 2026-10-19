package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavingsGoalRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SavingsGoalRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SavingsGoalRepositoryImpl(db, db.savingsGoalDao(), db.auditLogDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun createGoalStoresDataAccurately() = runTest {
        val goalId = repository.createGoal(
            name = "Emergency Fund",
            targetAmountPaisa = 10000000L, // Rs. 100,000
            targetDateMillis = 1800000000000L,
            notes = "6 months living expenses"
        )

        val goal = repository.getGoalById(goalId).first()
        assertNotNull(goal)
        assertEquals("Emergency Fund", goal?.name)
        assertEquals(10000000L, goal?.targetAmountPaisa)
        assertEquals(0L, goal?.savedAmountPaisa)
        assertEquals(10000000L, goal?.remainingPaisa)
        assertEquals(0, goal?.progressPercent)
        assertFalse(goal?.isCompleted ?: true)
    }

    @Test
    fun addContributionUpdatesSavedAmountAndCalculatesProgress() = runTest {
        val goalId = repository.createGoal(
            name = "New Laptop",
            targetAmountPaisa = 15000000L // Rs. 150,000
        )

        val contribId = repository.addContribution(
            goalId = goalId,
            amountPaisa = 5000000L, // Rs. 50,000
            notes = "Monthly salary savings"
        )
        assertTrue(contribId > 0)

        val goal = repository.getGoalById(goalId).first()
        assertNotNull(goal)
        assertEquals(5000000L, goal?.savedAmountPaisa)
        assertEquals(10000000L, goal?.remainingPaisa)
        assertEquals(33, goal?.progressPercent)
        assertFalse(goal?.isCompleted ?: true)

        // Add remaining contribution to reach target
        repository.addContribution(
            goalId = goalId,
            amountPaisa = 10000000L,
            notes = "Bonus payment"
        )

        val completedGoal = repository.getGoalById(goalId).first()
        assertEquals(15000000L, completedGoal?.savedAmountPaisa)
        assertEquals(0L, completedGoal?.remainingPaisa)
        assertEquals(100, completedGoal?.progressPercent)
        assertTrue(completedGoal?.isCompleted ?: false)
    }

    @Test
    fun deleteContributionRecalculatesSavedAmount() = runTest {
        val goalId = repository.createGoal(
            name = "Dashain Vacation",
            targetAmountPaisa = 5000000L
        )

        val c1 = repository.addContribution(goalId, 2000000L)
        val c2 = repository.addContribution(goalId, 3000000L)

        var goal = repository.getGoalById(goalId).first()
        assertTrue(goal?.isCompleted ?: false)

        repository.deleteContribution(c2)

        goal = repository.getGoalById(goalId).first()
        assertEquals(2000000L, goal?.savedAmountPaisa)
        assertEquals(3000000L, goal?.remainingPaisa)
        assertFalse(goal?.isCompleted ?: true)
    }

    @Test
    fun deleteGoalRemovesGoalAndCascadeDeletesContributions() = runTest {
        val goalId = repository.createGoal(
            name = "Temporary Goal",
            targetAmountPaisa = 1000000L
        )
        repository.addContribution(goalId, 500000L)

        repository.deleteGoal(goalId)

        val goal = repository.getGoalById(goalId).first()
        assertNull(goal)

        val contributions = repository.getContributionsForGoal(goalId).first()
        assertTrue(contributions.isEmpty())
    }

    @Test
    fun summaryAggregatesActiveAndCompletedGoals() = runTest {
        val g1 = repository.createGoal("Goal 1", 5000000L)
        val g2 = repository.createGoal("Goal 2", 3000000L)

        repository.addContribution(g1, 5000000L) // Completes Goal 1
        repository.addContribution(g2, 1000000L) // Active Goal 2

        val summary = repository.getSummary().first()
        assertEquals(8000000L, summary.totalTargetPaisa)
        assertEquals(6000000L, summary.totalSavedPaisa)
        assertEquals(2000000L, summary.remainingPaisa)
        assertEquals(75, summary.overallProgressPercent)
        assertEquals(1, summary.activeGoalsCount)
        assertEquals(1, summary.completedGoalsCount)
    }
}
