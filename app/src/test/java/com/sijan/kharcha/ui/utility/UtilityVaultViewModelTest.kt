package com.sijan.kharcha.ui.utility

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.data.repository.UtilityRepository
import com.sijan.kharcha.data.repository.UtilityRepositoryImpl
import com.sijan.kharcha.domain.model.UtilityAccount
import com.sijan.kharcha.domain.model.UtilityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UtilityVaultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var repository: UtilityRepository
    private lateinit var viewModel: UtilityVaultViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UtilityRepositoryImpl(db.utilityAccountDao())
        viewModel = UtilityVaultViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialState_hasNullFilterAndEmptyList() {
        assertNull(viewModel.selectedType.value)
        assertEquals(0, viewModel.accounts.value.size)
    }

    @Test
    fun filterByType_updatesSelectedTypeAndFiltersList() = runTest(testDispatcher) {
        val nea = UtilityAccount(
            utilityType = UtilityType.NEA,
            providerName = "NEA",
            consumerId = "012.34.567"
        )
        val isp = UtilityAccount(
            utilityType = UtilityType.ISP,
            providerName = "WorldLink",
            consumerId = "wlink123"
        )

        repository.saveUtilityAccount(nea).getOrThrow()
        repository.saveUtilityAccount(isp).getOrThrow()

        // Default all
        viewModel.selectType(null)
        testScheduler.advanceUntilIdle()
        assertEquals(2, repository.getAllUtilityAccounts().first().size)

        // Filter NEA
        viewModel.selectType(UtilityType.NEA)
        assertEquals(UtilityType.NEA, viewModel.selectedType.value)

        // Filter ISP
        viewModel.selectType(UtilityType.ISP)
        assertEquals(UtilityType.ISP, viewModel.selectedType.value)
    }

    @Test
    fun saveAndDeleteAccount_updatesRepository() = runTest(testDispatcher) {
        val account = UtilityAccount(
            utilityType = UtilityType.KHANEPANI,
            providerName = "Kathmandu Upatyaka Khanepani",
            consumerId = "meter-9988"
        )

        viewModel.saveAccount(account)
        testScheduler.advanceUntilIdle()

        val all = repository.getAllUtilityAccounts().first()
        assertEquals(1, all.size)
        assertEquals("meter-9988", all[0].consumerId)

        viewModel.deleteAccount(all[0].id)
        testScheduler.advanceUntilIdle()

        val empty = repository.getAllUtilityAccounts().first()
        assertEquals(0, empty.size)
    }
}
