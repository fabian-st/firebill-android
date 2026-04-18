package com.firebill.android.data.repository

import android.content.Context
import com.firebill.android.data.api.ApiClient
import com.firebill.android.data.api.FirebillApiService
import com.firebill.android.data.api.models.*
import com.firebill.android.data.db.AppDatabase
import com.firebill.android.data.db.PendingUpload
import com.firebill.android.data.db.PendingUploadDao
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class BillRepository(private val context: Context) {

    private val api: FirebillApiService get() = ApiClient.getService(context)
    private val pendingUploadDao: PendingUploadDao =
        AppDatabase.getInstance(context).pendingUploadDao()

    // --- Bills ---

    suspend fun getBills(): Result<List<Bill>> = runCatching {
        val response = api.getBills()
        if (response.isSuccessful) {
            response.body()?.bills ?: emptyList()
        } else {
            throw Exception("Failed to load bills: ${response.code()}")
        }
    }

    suspend fun getBill(id: Int): Result<Bill> = runCatching {
        val response = api.getBill(id)
        if (response.isSuccessful) {
            response.body()?.bill ?: throw Exception("Bill not found")
        } else {
            throw Exception("Failed to load bill: ${response.code()}")
        }
    }

    suspend fun uploadBill(file: File): Result<Bill> = runCatching {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val response = api.uploadBill(part)
        if (response.isSuccessful) {
            response.body()?.bill ?: throw Exception("Upload failed: empty response")
        } else {
            throw Exception("Upload failed: ${response.code()}")
        }
    }

    suspend fun updateBill(id: Int, request: UpdateBillRequest): Result<Bill> = runCatching {
        val response = api.updateBill(id, request)
        if (response.isSuccessful) {
            response.body()?.bill ?: throw Exception("Update failed")
        } else {
            throw Exception("Update failed: ${response.code()}")
        }
    }

    suspend fun deleteBill(id: Int): Result<Unit> = runCatching {
        val response = api.deleteBill(id)
        if (!response.isSuccessful) {
            throw Exception("Delete failed: ${response.code()}")
        }
    }

    suspend fun getExportOptions(id: Int): Result<ExportOptions> = runCatching {
        val response = api.getExportOptions(id)
        if (response.isSuccessful) {
            response.body() ?: throw Exception("No export options")
        } else {
            throw Exception("Failed to get export options: ${response.code()}")
        }
    }

    suspend fun exportBill(id: Int, request: ExportRequest): Result<ExportResponse> = runCatching {
        val response = api.exportBill(id, request)
        if (response.isSuccessful) {
            response.body() ?: throw Exception("Export failed")
        } else {
            throw Exception("Export failed: ${response.code()}")
        }
    }

    // --- Settings ---

    suspend fun getSettings(): Result<Settings> = runCatching {
        val response = api.getSettings()
        if (response.isSuccessful) {
            response.body()?.settings ?: throw Exception("No settings")
        } else {
            throw Exception("Failed to load settings: ${response.code()}")
        }
    }

    suspend fun updateSettings(request: UpdateSettingsRequest): Result<Settings> = runCatching {
        val response = api.updateSettings(request)
        if (response.isSuccessful) {
            response.body()?.settings ?: throw Exception("Update failed")
        } else {
            throw Exception("Settings update failed: ${response.code()}")
        }
    }

    suspend fun testConnection(): Result<ConnectionTestResponse> = runCatching {
        val response = api.testConnection()
        if (response.isSuccessful) {
            response.body() ?: throw Exception("Empty response")
        } else {
            throw Exception("Connection test failed: ${response.code()}")
        }
    }

    suspend fun checkHealth(): Result<HealthResponse> = runCatching {
        val response = api.getHealth()
        if (response.isSuccessful) {
            response.body() ?: throw Exception("Empty response")
        } else {
            throw Exception("Health check failed: ${response.code()}")
        }
    }

    // --- Pending Uploads (offline queue) ---

    fun getPendingUploads() = pendingUploadDao.getAllPending()

    fun getPendingCount() = pendingUploadDao.getPendingCount()

    suspend fun queueUpload(filePath: String, fileName: String): Long {
        val upload = PendingUpload(filePath = filePath, fileName = fileName)
        return pendingUploadDao.insert(upload)
    }

    suspend fun getPendingOnce() = pendingUploadDao.getAllPendingOnce()

    suspend fun deletePendingUpload(upload: PendingUpload) {
        pendingUploadDao.delete(upload)
    }

    suspend fun incrementRetry(id: Long, error: String?) {
        pendingUploadDao.incrementRetry(id, error)
    }
}
