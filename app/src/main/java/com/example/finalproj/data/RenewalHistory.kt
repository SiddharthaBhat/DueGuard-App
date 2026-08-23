package com.example.finalproj.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "renewal_history")
data class RenewalHistory(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val documentId: Int,

    val oldExpiryDate: Long,

    val renewalDate: Long
)