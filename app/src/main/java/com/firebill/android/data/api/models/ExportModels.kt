package com.firebill.android.data.api.models

import com.google.gson.annotations.SerializedName

data class ExportOptions(
    val ok: Boolean,
    val categories: List<FireflyCategory>,
    val tags: List<FireflyTag>,
    val accounts: List<FireflyAccount>
)

data class FireflyCategory(
    val id: String,
    val name: String
)

data class FireflyTag(
    val id: String,
    val name: String,
    val tag: String
)

data class FireflyAccount(
    val id: String,
    val name: String,
    val type: String?
)

data class ExportRequest(
    @SerializedName("source_account_id") val sourceAccountId: String,
    @SerializedName("bill_date") val billDate: String?,
    val items: List<ExportItemRequest>
)

data class ExportItemRequest(
    val id: Int,
    val selected: Boolean,
    val category: String?,
    val tags: List<String>,
    val notes: String?
)

data class ExportResponse(
    val ok: Boolean,
    val message: String?,
    @SerializedName("transaction_id") val transactionId: String?,
    @SerializedName("transaction_url") val transactionUrl: String?
)
