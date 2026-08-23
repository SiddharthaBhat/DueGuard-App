package com.example.finalproj.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert
    suspend fun insert(document: Document): Long

    @Update
    suspend fun update(document: Document)

    @Delete
    suspend fun delete(document: Document)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): Document?

    @Query("SELECT * FROM documents WHERE memberId = :memberId")
    fun getDocumentsForMember(memberId: Int): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE memberId = :memberId")
    suspend fun getDocumentsListForMember(memberId: Int): List<Document>

    @Query("SELECT * FROM documents")
    suspend fun getAllDocumentsForWorker(): List<Document>

    @Query("""
        SELECT COUNT(*) FROM documents
        WHERE expiryDate >= :now
        AND expiryDate <= :sevenDaysLater
    """)
    suspend fun getUpcomingRenewalsCount(
        now: Long,
        sevenDaysLater: Long
    ): Int

    @Query("""
        SELECT COUNT(*) FROM documents
        WHERE memberId = :memberId
        AND expiryDate >= :now
        AND expiryDate <= :sevenDaysLater
    """)
    suspend fun getUpcomingRenewalsCountForMember(
        memberId: Int,
        now: Long,
        sevenDaysLater: Long
    ): Int

    @Query("SELECT COUNT(*) FROM documents WHERE cloudId = :cloudId")
    suspend fun documentExists(cloudId: String): Int

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
}