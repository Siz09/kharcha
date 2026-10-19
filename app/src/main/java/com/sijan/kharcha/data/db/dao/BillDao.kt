package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sijan.kharcha.data.db.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    fun getBillById(id: Long): Flow<BillEntity?>

    @Query("SELECT * FROM bills ORDER BY dueDayOfMonth ASC, id ASC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE isAutoReminder = 1")
    suspend fun getBillsWithReminder(): List<BillEntity>

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()
}
