package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class PrefixImage(
    @Json(name = "makeItRound")
    val makeItRound: Boolean?,
    @Json(name = "imageUrl")
    val imageUrl: String?,
    @Json(name = "imageWidth")
    val imageWidth: Int?,
    @Json(name = "imageHeight")
    val imageHeight: Int?,
) : Item()
