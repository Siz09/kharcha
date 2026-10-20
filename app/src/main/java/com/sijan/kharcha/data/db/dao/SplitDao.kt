package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sijan.kharcha.data.db.entity.LedgerEntrySplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<LedgerEntrySplitEntity>)

    @Query("SELECT * FROM ledger_entry_splits WHERE entryId = :entryId")
    fun getSplitsForEntry(entryId: Long): Flow<List<LedgerEntrySplitEntity>>

    @Query("SELECT * FROM ledger_entry_splits WHERE entryId = :entryId")
    suspend fun getSplitsForEntrySync(entryId: Long): List<LedgerEntrySplitEntity>

    @Query("DELETE FROM ledger_entry_splits WHERE entryId = :entryId")
    suspend fun deleteSplitsForEntry(entryId: Long)

    @Query("DELETE FROM ledger_entry_splits")
    suspend fun deleteAllSplits()
}
