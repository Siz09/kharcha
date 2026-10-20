package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sijan.kharcha.data.db.entity.AnnualArchiveSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnualArchiveSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: AnnualArchiveSummaryEntity)

    @Query("SELECT * FROM annual_archive_summaries WHERE year = :year AND calendarSystem = :calendarSystem")
    fun getSummary(year: Int, calendarSystem: String): Flow<AnnualArchiveSummaryEntity?>

    @Query("SELECT * FROM annual_archive_summaries WHERE calendarSystem = :calendarSystem ORDER BY year DESC")
    fun getAllSummaries(calendarSystem: String): Flow<List<AnnualArchiveSummaryEntity>>

    @Query("DELETE FROM annual_archive_summaries WHERE year = :year AND calendarSystem = :calendarSystem")
    suspend fun deleteSummary(year: Int, calendarSystem: String)

    @Query("DELETE FROM annual_archive_summaries")
    suspend fun deleteAllSummaries()
}
