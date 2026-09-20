package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.comments.model.SourceComment

@JsonClass(generateAdapter = true)
data class FtsCarousel(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "label")
    val label: String? = null,
) : Item(type = type) {
    // "sourceAnnotations" are loaded by a article augmentation API
    // and is updated once that API is loaded.
    var sourceComments: List<SourceComment> = emptyList()

    companion object {
        const val ITEM_NAME = "fts_carousel"
    }
}
