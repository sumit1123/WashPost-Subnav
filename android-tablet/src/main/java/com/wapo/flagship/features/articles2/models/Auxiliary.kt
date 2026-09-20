package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Auxiliary(
    @Json(name = "_id")
    val id: String? = null,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "uid")
    val uid: String? = null,
)
