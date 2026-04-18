package com.firebill.android.data.api.models

import com.google.gson.annotations.SerializedName

data class BillsResponse(
    val ok: Boolean,
    val bills: List<Bill>
)

data class BillResponse(
    val ok: Boolean,
    val bill: Bill
)

data class UpdateBillRequest(
    val vendor: String?,
    @SerializedName("bill_date") val billDate: String?,
    val items: List<BillItemRequest>
)

data class BillItemRequest(
    val id: Int?,
    val description: String,
    val price: Double
)

data class UploadResponse(
    val ok: Boolean,
    val bill: Bill?
)

data class DeleteResponse(
    val ok: Boolean,
    val message: String?
)
