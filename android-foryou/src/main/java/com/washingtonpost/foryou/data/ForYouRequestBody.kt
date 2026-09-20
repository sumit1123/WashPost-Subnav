package com.washingtonpost.foryou.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForYouRequestBody(
    @Json(name = "readlist") val readList: List<ConsumedArticles>,
    @Json(name = "exclusions") val exclusions: List<String>,
    @Json(name = "j_ucid") val jucId: String?,
    @Json(name = "wapo_login_id") val wapoLoginId: String?,
    @Json(name = "interface") val interfase : String = "android",
    @Json(name = "surface") val surface: String,
    @Json(name = "surface_variant") val surfaceVariant: String?,
    @Json(name = "limit") val limit: Int?,
    @Json(name = "current_url") val currentUrl: String?,
    @Json(name = "desired_content_types") val contentType: List<String>? = null
)
