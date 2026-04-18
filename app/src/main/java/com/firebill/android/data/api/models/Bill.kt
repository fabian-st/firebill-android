package com.firebill.android.data.api.models

import com.google.gson.annotations.SerializedName

data class Bill(
    val id: Int,
    val filename: String?,
    val vendor: String?,
    @SerializedName("bill_date") val billDate: String?,
    @SerializedName("uploaded_at") val uploadedAt: String?,
    val exported: Boolean,
    @SerializedName("firefly_transaction_id") val fireflyTransactionId: String?,
    @SerializedName("firefly_transaction_url") val fireflyTransactionUrl: String?,
    @SerializedName("image_url") val imageUrl: String?,
    val items: List<BillItem> = emptyList(),
    val total: Double?
)
