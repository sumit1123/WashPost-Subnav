package com.washingtonpost.android.paywall.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NonceResponse(
    @Json(name = "status") val status: String?,
    @Json(name = "nonce") val nonce: String?,
)