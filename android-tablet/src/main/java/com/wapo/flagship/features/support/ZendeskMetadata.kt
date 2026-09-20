package com.wapo.flagship.features.support

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ZendeskMetadata(
    @Json(name = "app")
    val app: SupportInfoApp? = null,
    @Json(name = "device")
    val device: Device? = null,
    @Json(name = "user")
    val user: User? = null,
)
