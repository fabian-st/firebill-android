package com.firebill.android.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PendingUploadDao {

    @Query("SELECT * FROM pending_uploads ORDER BY createdAt ASC")
    fun getAllPending(): LiveData<List<PendingUpload>>

    @Query("SELECT * FROM pending_uploads ORDER BY createdAt ASC")
    suspend fun getAllPendingOnce(): List<PendingUpload>

    @Query("SELECT COUNT(*) FROM pending_uploads")
    fun getPendingCount(): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(upload: PendingUpload): Long

    @Update
    suspend fun update(upload: PendingUpload)

    @Delete
    suspend fun delete(upload: PendingUpload)

    @Query("DELETE FROM pending_uploads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_uploads SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun incrementRetry(id: Long, error: String?)
}
