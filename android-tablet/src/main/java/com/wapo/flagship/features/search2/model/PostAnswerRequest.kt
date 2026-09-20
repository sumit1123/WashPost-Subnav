package com.wapo.flagship.features.search2.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostAnswerRequest(
    @Json(name = "query")
    val query: String,
    @Json(name = "query_type")
    val queryType: String? = null,
    @Json(name = "urls")
    val urls: List<String>?,
    @Json(name = "app_name")
    val appName: String?,
    @Json(name = "support_id")
    val supportId: String?,
)
