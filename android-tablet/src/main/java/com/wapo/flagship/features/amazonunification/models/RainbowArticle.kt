package com.wapo.flagship.features.amazonunification.models

import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

/**
 * Data class to migrate Rainbow Saved Articles to Classic
 */
@JsonClass(generateAdapter = true)
data class RainbowArticle(
    @Json(name = "blurb")
    val blurb: String? = null,
    @Json(name = "contenturl")
    val contentUrl: String? = null,
    @Json(name = "shareurl")
    val shareUrl: String? = null,
    @Json(name = "socialImage")
    val socialImage: String? = null,
    @Json(name = "sourceurl")
    val sourceUrl: String? = null,
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "items")
    @TypeConverters(RainbowItemListTypeConverter::class)
    val items: List<Item>? = null,
    @Json(name = "published")
    val published: Long? = null,
)
