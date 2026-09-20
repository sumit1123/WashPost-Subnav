package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class InlinePlayer(
    @Json(name = "listen")
    val listen: String? = null,
    @Json(name = "label")
    val label: String?,
    @Json(name = "prefixImage")
    val prefixImage: PrefixImage?,
) : Item()
