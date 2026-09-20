package com.wapo.flagship.features.articles2.models.deserialized.tweet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Tweet(
    @Json(name = "content")
    val content: Content?,
    @Json(name = "dataServiceAdaptor")
    val dataServiceAdaptor: String?,
    @Json(name = "lmt")
    val lmt: Long?,
    @Json(name = "type")
    override val type: String?,
) : Item(type = type)
