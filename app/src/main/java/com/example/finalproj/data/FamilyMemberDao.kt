package com.example.finalproj.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: FamilyMember)

    @Delete
    suspend fun delete(member: FamilyMember)

    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members ORDER BY name ASC")
    suspend fun getAllMembersList(): List<FamilyMember>

    @Query("SELECT COUNT(*) FROM family_members")
    suspend fun getMemberCount(): Int

    @Query("SELECT * FROM family_members WHERE id = :memberId LIMIT 1")
    suspend fun getMemberById(memberId: Int): FamilyMember?

    @Query("SELECT * FROM family_members WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getMemberByCloudId(cloudId: String): FamilyMember?

    @Query("SELECT COUNT(*) > 0 FROM family_members WHERE cloudId = :cloudId")
    suspend fun memberExists(cloudId: String): Boolean

    @Query("DELETE FROM family_members")
    suspend fun deleteAll()
}