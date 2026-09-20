package com.washingtonpost.foryou.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConsumedArticles(
    @Json(name = "u") val articleUrl: String,
    @Json(name = "t") val timestamp: Long
)

