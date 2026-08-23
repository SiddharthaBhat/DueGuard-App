package com.example.finalproj.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "family_members",
    indices = [
        Index(value = ["cloudId"], unique = true)
    ]
)
data class FamilyMember(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val phone: String? = null,

    val email: String? = null,

    // ✅ Added profile image path
    val profileImagePath: String? = null,

    // Preferred notification channel (WhatsApp / Email / SMS)
    val notifyChannel: String? = null,

    // Firestore document ID
    val cloudId: String? = null
)
