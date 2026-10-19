package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sijan.kharcha.data.db.entity.RecurringEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringEntryDao {

    @Query("SELECT * FROM recurring_entries ORDER BY nextDueDate ASC")
    fun getAllRecurring(): Flow<List<RecurringEntryEntity>>

    @Query("SELECT * FROM recurring_entries WHERE isActive = 1 AND nextDueDate <= :cutoffTime")
    suspend fun getDueRecurring(cutoffTime: Long): List<RecurringEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(entry: RecurringEntryEntity): Long

    @Update
    suspend fun updateRecurring(entry: RecurringEntryEntity): Int

    @Query("DELETE FROM recurring_entries WHERE id = :id")
    suspend fun deleteRecurring(id: Long): Int
}
