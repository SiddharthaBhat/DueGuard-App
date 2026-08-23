package com.example.finalproj.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(

    @PrimaryKey
    val id: Int = 1,   // Single fixed profile

    val fullName: String,
    val email: String,
    val phone: String,
    val profileImagePath: String? = null   // Optional image
)