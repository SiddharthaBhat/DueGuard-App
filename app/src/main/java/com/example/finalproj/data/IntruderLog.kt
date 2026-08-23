package com.example.finalproj.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intruder_logs")
data class IntruderLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val attemptNumber: Int,
    val timestamp: Long,
    val frontImagePath: String,
    val backImagePath: String,
    val deviceModel: String,
    var isPinned: Boolean = false
)