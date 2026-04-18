package com.firebill.android.data.api.models

import com.google.gson.annotations.SerializedName

data class Settings(
    @SerializedName("firefly_url") val fireflyUrl: String?,
    @SerializedName("firefly_token") val fireflyToken: String?
)

data class SettingsResponse(
    val ok: Boolean,
    val settings: Settings?
)

data class UpdateSettingsRequest(
    @SerializedName("firefly_url") val fireflyUrl: String?,
    @SerializedName("firefly_token") val fireflyToken: String?
)

data class ConnectionTestResponse(
    val ok: Boolean,
    val message: String?
)

data class HealthResponse(
    val ok: Boolean,
    val message: String?
)
