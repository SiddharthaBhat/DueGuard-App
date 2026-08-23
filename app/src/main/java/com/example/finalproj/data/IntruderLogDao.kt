package com.example.finalproj.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IntruderLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: IntruderLog)

    // 🔥 Pinned first, then latest
    @Query("SELECT * FROM intruder_logs ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAllLogs(): List<IntruderLog>

    @Query("SELECT * FROM intruder_logs WHERE id = :id")
    suspend fun getLogById(id: Int): IntruderLog

    @Query("DELETE FROM intruder_logs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE intruder_logs SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePinStatus(id: Int, pinned: Boolean)

    @Query("DELETE FROM intruder_logs")
    suspend fun deleteAll()
}