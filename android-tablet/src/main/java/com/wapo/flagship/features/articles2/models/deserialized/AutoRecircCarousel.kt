package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class AutoRecircCarousel(
    @Json(name = "carouselId") val carouselId: Int? = null,
    @Json(name = "type") override val type: String? = null,
    @Json(name = "subtype") val subtype: String? = SUBTYPE_STORIES,
): Item(type = type) {
    companion object {
        const val ITEM_NAME = "autorecirc_carousel"
        const val SUBTYPE_STORIES = "stories"
        const val SUBTYPE_THE_7_LIVE = "the-7-live"
    }
}