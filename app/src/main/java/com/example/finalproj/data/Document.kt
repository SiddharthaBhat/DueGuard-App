package com.example.finalproj.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memberId"])]
)
data class Document(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val memberId: Int,

    val title: String,
    
    // ✅ Store document type (Licence, PUC, etc.)
    val type: String = "Other",

    // Local file path
    val filePath: String,

    // Firebase Storage URL
    val fileUrl: String? = null,

    val expiryDate: Long,

    val notificationEnabled: Boolean,

    val ocrText: String? = null,

    // Firestore document id
    val cloudId: String? = null
)