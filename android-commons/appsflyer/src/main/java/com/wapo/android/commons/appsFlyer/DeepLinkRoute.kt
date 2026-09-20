package com.wapo.android.commons.appsFlyer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeepLinkRoute(
    @Json(name = "type")
    val type: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "id")
    val id: String?
)