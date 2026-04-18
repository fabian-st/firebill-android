package com.firebill.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_uploads")
data class PendingUpload(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)
