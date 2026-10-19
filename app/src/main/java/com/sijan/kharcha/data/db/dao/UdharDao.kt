package com.sijan.kharcha.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sijan.kharcha.data.db.entity.UdharEntity
import com.sijan.kharcha.data.db.entity.UdharSettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UdharDao {

    @Query("SELECT * FROM udhars ORDER BY createdAt DESC")
    fun getAllUdhars(): Flow<List<UdharEntity>>

    @Query("SELECT * FROM udhars WHERE type = :type ORDER BY createdAt DESC")
    fun getUdharsByType(type: String): Flow<List<UdharEntity>>

    @Query("SELECT * FROM udhars WHERE status = :status ORDER BY createdAt DESC")
    fun getUdharsByStatus(status: String): Flow<List<UdharEntity>>

    @Query("SELECT * FROM udhars WHERE id = :id")
    fun getUdharById(id: Long): Flow<UdharEntity?>

    @Query("SELECT * FROM udhars WHERE id = :id")
    suspend fun getUdharByIdDirect(id: Long): UdharEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUdhar(entity: UdharEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUdhars(entities: List<UdharEntity>)

    @Update
    suspend fun updateUdhar(entity: UdharEntity): Int

    @Query("DELETE FROM udhars WHERE id = :id")
    suspend fun deleteUdhar(id: Long): Int

    @Query("DELETE FROM udhars")
    suspend fun deleteAllUdhars(): Int

    // Settlements
    @Query("SELECT * FROM udhar_settlements WHERE udharId = :udharId ORDER BY dateEpochMillis DESC")
    fun getSettlementsForUdhar(udharId: Long): Flow<List<UdharSettlementEntity>>

    @Query("SELECT * FROM udhar_settlements WHERE udharId = :udharId ORDER BY dateEpochMillis DESC")
    suspend fun getSettlementsForUdharDirect(udharId: Long): List<UdharSettlementEntity>

    @Query("SELECT * FROM udhar_settlements ORDER BY dateEpochMillis DESC")
    fun getAllSettlements(): Flow<List<UdharSettlementEntity>>

    @Query("SELECT * FROM udhar_settlements WHERE id = :id")
    suspend fun getSettlementById(id: Long): UdharSettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(entity: UdharSettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(entities: List<UdharSettlementEntity>)

    @Query("DELETE FROM udhar_settlements WHERE id = :id")
    suspend fun deleteSettlement(id: Long): Int

    @Query("DELETE FROM udhar_settlements WHERE udharId = :udharId")
    suspend fun deleteSettlementsForUdhar(udharId: Long): Int

    @Query("DELETE FROM udhar_settlements")
    suspend fun deleteAllSettlements(): Int
}
