package com.firebill.android.data.api.models

import com.google.gson.annotations.SerializedName

data class BillItem(
    val id: Int?,
    @SerializedName("bill_id") val billId: Int?,
    val position: Int?,
    val description: String,
    val price: Double
)
