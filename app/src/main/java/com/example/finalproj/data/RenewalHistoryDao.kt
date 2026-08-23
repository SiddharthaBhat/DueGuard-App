package com.example.finalproj.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RenewalHistoryDao {

    @Insert
    suspend fun insert(history: RenewalHistory)

    @Query("SELECT * FROM renewal_history WHERE documentId = :docId ORDER BY renewalDate DESC")
    suspend fun getHistoryForDocument(docId: Int): List<RenewalHistory>
}