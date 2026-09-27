package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val fileType: String, // "PDF", "JPG", "PNG", "ZIP"
    val operationType: String, // "JPG to PDF", "PDF to JPG", "Compressed", "Page Editor"
    val sizeBytes: Long,
    val pageCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
