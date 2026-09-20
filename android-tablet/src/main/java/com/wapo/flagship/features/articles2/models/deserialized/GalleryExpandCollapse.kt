package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
open class GalleryExpandCollapse(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "truncatedLabel")
    val truncatedLabel: String? = null,
    @Json(name = "expandedLabel")
    val expandedLabel: String? = null,
    var galleryId: String?,
) : Item(type = type)
