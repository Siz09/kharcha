package com.sijan.kharcha.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sijan.kharcha.data.db.AppDatabase
import com.sijan.kharcha.domain.model.UtilityAccount
import com.sijan.kharcha.domain.model.UtilityType
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
class UtilityRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: UtilityRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UtilityRepositoryImpl(db.utilityAccountDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun crudOperations_workReliably() = runTest {
        val nea = UtilityAccount(
            utilityType = UtilityType.NEA,
            providerName = "NEA Ratnapark",
            consumerId = "012.34.567",
            counterOrBranch = "Ratnapark",
            accountHolderName = "Ram Sharma"
        )

        val isp = UtilityAccount(
            utilityType = UtilityType.ISP,
            providerName = "WorldLink",
            consumerId = "wlink_user_9921",
            counterOrBranch = "Jawalakhel",
            accountHolderName = "Sijan"
        )

        val id1 = repository.saveUtilityAccount(nea).getOrThrow()
        val id2 = repository.saveUtilityAccount(isp).getOrThrow()

        val all = repository.getAllUtilityAccounts().first()
        assertEquals(2, all.size)

        val neaList = repository.getUtilityAccountsByType(UtilityType.NEA).first()
        assertEquals(1, neaList.size)
        assertEquals("012.34.567", neaList[0].consumerId)

        val fetched = repository.getUtilityAccountById(id1).first()
        assertNotNull(fetched)
        assertEquals("Ratnapark", fetched?.counterOrBranch)

        repository.updateUtilityAccount(fetched!!.copy(counterOrBranch = "Kuleshwor")).getOrThrow()
        val updated = repository.getUtilityAccountById(id1).first()
        assertEquals("Kuleshwor", updated?.counterOrBranch)

        repository.deleteUtilityAccount(id2).getOrThrow()
        val afterDelete = repository.getAllUtilityAccounts().first()
        assertEquals(1, afterDelete.size)
        assertEquals(id1, afterDelete[0].id)
    }
}
