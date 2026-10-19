package com.sijan.kharcha.ui.savings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.repository.SavingsGoalRepository
import com.sijan.kharcha.data.repository.SavingsGoalRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SavingsGoalsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repository: SavingsGoalRepository
    private lateinit var viewModel: SavingsGoalsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SavingsGoalRepositoryImpl(db, db.savingsGoalDao(), db.auditLogDao())
        viewModel = SavingsGoalsViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun creatingGoalReflectsInUiState() = runTest(testDispatcher) {
        viewModel.openAddGoalDialog()
        val stateWithDialog = viewModel.uiState.first { it.showAddEditDialog }
        assertTrue(stateWithDialog.showAddEditDialog)

        viewModel.saveGoal(
            name = "Dashain Vacation",
            targetAmountPaisa = 10000000L, // Rs. 100,000
            targetDateMillis = 1800000000000L,
            notes = "Family tour"
        )

        val updatedState = viewModel.uiState.first { it.goals.isNotEmpty() }
        assertFalse(updatedState.showAddEditDialog)
        val goals = updatedState.goals
        assertEquals(1, goals.size)
        assertEquals("Dashain Vacation", goals[0].name)
        assertEquals(10000000L, goals[0].targetAmountPaisa)
        assertEquals(0L, goals[0].savedAmountPaisa)
        assertEquals(10000000L, goals[0].remainingPaisa)
        assertEquals(0, goals[0].progressPercent)
        assertFalse(goals[0].isCompleted)
    }

    @Test
    fun depositingFundsUpdatesProgressAndAutoCompletesGoal() = runTest(testDispatcher) {
        viewModel.saveGoal(
            name = "Emergency Fund",
            targetAmountPaisa = 5000000L, // Rs. 50,000
            targetDateMillis = null,
            notes = null
        )

        val stateWithGoal = viewModel.uiState.first { it.goals.isNotEmpty() }
        val goal = stateWithGoal.goals[0]

        // Partial deposit
        viewModel.openDepositDialog(goal)
        val depositDialogState = viewModel.uiState.first { it.selectedGoalForDeposit?.id == goal.id }
        assertEquals(goal.id, depositDialogState.selectedGoalForDeposit?.id)

        viewModel.addDeposit(
            goalId = goal.id,
            amountPaisa = 2000000L,
            dateMillis = 1000L,
            notes = "First deposit"
        )

        val stateAfterPartial = viewModel.uiState.first { it.goals.firstOrNull()?.savedAmountPaisa == 2000000L }
        assertNull(stateAfterPartial.selectedGoalForDeposit)
        assertEquals(2000000L, stateAfterPartial.goals[0].savedAmountPaisa)
        assertEquals(3000000L, stateAfterPartial.goals[0].remainingPaisa)
        assertEquals(40, stateAfterPartial.goals[0].progressPercent)
        assertFalse(stateAfterPartial.goals[0].isCompleted)

        // Complete the goal
        viewModel.addDeposit(
            goalId = goal.id,
            amountPaisa = 3000000L,
            dateMillis = 2000L,
            notes = "Final deposit"
        )

        val completedState = viewModel.uiState.first {
            it.goals.firstOrNull()?.isCompleted == true && it.summary.completedGoalsCount == 1
        }
        assertEquals(5000000L, completedState.goals[0].savedAmountPaisa)
        assertEquals(0L, completedState.goals[0].remainingPaisa)
        assertEquals(100, completedState.goals[0].progressPercent)
        assertTrue(completedState.goals[0].isCompleted)
        assertEquals(1, completedState.summary.completedGoalsCount)
        assertEquals(0, completedState.summary.activeGoalsCount)
    }

    @Test
    fun tabsFilterGoalsAccurately() = runTest(testDispatcher) {
        // Goal 1: Active
        viewModel.saveGoal("Active Goal", 5000000L, null, null)
        // Goal 2: Completed
        viewModel.saveGoal("Completed Goal", 3000000L, null, null)

        val state = viewModel.uiState.first { it.goals.size == 2 }
        val completedGoal = state.goals.first { it.name == "Completed Goal" }
        viewModel.addDeposit(completedGoal.id, 3000000L, 1000L, null)

        viewModel.uiState.first { it.summary.completedGoalsCount == 1 }

        // Test ACTIVE tab
        viewModel.setTabFilter(SavingsTabFilter.ACTIVE)
        val activeState = viewModel.uiState.first { it.tabFilter == SavingsTabFilter.ACTIVE }
        assertEquals(1, activeState.goals.size)
        assertEquals("Active Goal", activeState.goals[0].name)

        // Test COMPLETED tab
        viewModel.setTabFilter(SavingsTabFilter.COMPLETED)
        val completedTabState = viewModel.uiState.first { it.tabFilter == SavingsTabFilter.COMPLETED }
        assertEquals(1, completedTabState.goals.size)
        assertEquals("Completed Goal", completedTabState.goals[0].name)

        // Test ALL tab
        viewModel.setTabFilter(SavingsTabFilter.ALL)
        val allTabState = viewModel.uiState.first { it.tabFilter == SavingsTabFilter.ALL }
        assertEquals(2, allTabState.goals.size)
    }

    @Test
    fun editingGoalUpdatesFields() = runTest(testDispatcher) {
        viewModel.saveGoal("Initial Name", 5000000L, null, "Old notes")
        val state = viewModel.uiState.first { it.goals.isNotEmpty() }
        val goal = state.goals[0]

        viewModel.openEditGoalDialog(goal)
        val editState = viewModel.uiState.first { it.goalToEdit?.id == goal.id }
        assertEquals(goal.id, editState.goalToEdit?.id)
        assertTrue(editState.showAddEditDialog)

        viewModel.saveGoal("Updated Name", 8000000L, 1900000000000L, "New notes")

        val updatedState = viewModel.uiState.first { it.goals.firstOrNull()?.name == "Updated Name" }
        assertEquals("Updated Name", updatedState.goals[0].name)
        assertEquals(8000000L, updatedState.goals[0].targetAmountPaisa)
        assertEquals(1900000000000L, updatedState.goals[0].targetDateMillis)
        assertEquals("New notes", updatedState.goals[0].notes)
    }

    @Test
    fun deletingGoalRemovesFromUiState() = runTest(testDispatcher) {
        viewModel.saveGoal("To Be Deleted", 5000000L, null, null)
        val state = viewModel.uiState.first { it.goals.isNotEmpty() }
        val goal = state.goals[0]

        viewModel.promptDeleteGoal(goal)
        val deleteState = viewModel.uiState.first { it.goalToDelete?.id == goal.id }
        assertEquals(goal.id, deleteState.goalToDelete?.id)

        viewModel.confirmDeleteGoal()

        val deletedState = viewModel.uiState.first { it.goals.isEmpty() }
        assertNull(deletedState.goalToDelete)
        assertTrue(deletedState.goals.isEmpty())
    }

    @Test
    fun historyContributionsAndDeletionWorks() = runTest(testDispatcher) {
        viewModel.saveGoal("Goal with History", 5000000L, null, null)
        val state = viewModel.uiState.first { it.goals.isNotEmpty() }
        val goal = state.goals[0]

        viewModel.addDeposit(goal.id, 1000000L, 1000L, "First deposit")

        viewModel.openHistoryDialog(goal)
        val historyState = viewModel.uiState.first { it.contributionsForHistory.isNotEmpty() }
        assertEquals(1, historyState.contributionsForHistory.size)
        val contrib = historyState.contributionsForHistory[0]
        assertEquals(1000000L, contrib.amountPaisa)

        // Delete contribution
        viewModel.deleteContribution(contrib.id)

        val deletedState = viewModel.uiState.first {
            it.contributionsForHistory.isEmpty() && it.goals.firstOrNull()?.savedAmountPaisa == 0L
        }
        assertTrue(deletedState.contributionsForHistory.isEmpty())
        assertEquals(0L, deletedState.goals[0].savedAmountPaisa)
    }
}
