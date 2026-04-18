package com.firebill.android.data.api

import com.firebill.android.data.api.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface FirebillApiService {

    @GET("api/health")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("api/bills")
    suspend fun getBills(): Response<BillsResponse>

    @Multipart
    @POST("api/bills/upload")
    suspend fun uploadBill(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @GET("api/bills/{id}")
    suspend fun getBill(@Path("id") id: Int): Response<BillResponse>

    @PUT("api/bills/{id}")
    suspend fun updateBill(
        @Path("id") id: Int,
        @Body request: UpdateBillRequest
    ): Response<BillResponse>

    @PATCH("api/bills/{id}")
    suspend fun patchBill(
        @Path("id") id: Int,
        @Body request: UpdateBillRequest
    ): Response<BillResponse>

    @DELETE("api/bills/{id}")
    suspend fun deleteBill(@Path("id") id: Int): Response<DeleteResponse>

    @GET("api/bills/{id}/export/options")
    suspend fun getExportOptions(@Path("id") id: Int): Response<ExportOptions>

    @POST("api/bills/{id}/export")
    suspend fun exportBill(
        @Path("id") id: Int,
        @Body request: ExportRequest
    ): Response<ExportResponse>

    @GET("api/settings")
    suspend fun getSettings(): Response<SettingsResponse>

    @PATCH("api/settings")
    suspend fun updateSettings(
        @Body request: UpdateSettingsRequest
    ): Response<SettingsResponse>

    @GET("api/settings/test")
    suspend fun testConnection(): Response<ConnectionTestResponse>
}
