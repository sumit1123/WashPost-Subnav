package com.wapo.flagship.features.articles2.models.deserialized

import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.articles2.typeconverters.OmnitureXTypeConverter

@JsonClass(generateAdapter = true)
data class InlineCarousel(
    @Json(name = "items")
    val carouselItems: List<CarouselItem>?,
    @Json(name = "headline")
    val headline: String? = "",
    @Json(name = "subtype")
    val subtype: String? = "",
    @Json(name = "_id")
    val id: String? = "",
    @Json(name = "group")
    override var group: String?,
    @Json(name = "type")
    override val type: String? = "",
) : Item(type = type)

@JsonClass(generateAdapter = true)
data class CarouselItem(
    @Json(name = "lmt")
    val lmt: Long?,
    @Json(name = "contenturl")
    val contentUrl: String? = "",
    @Json(name = "image")
    val image: Image?,
    @Json(name = "title")
    val title: Title?,
    @Json(name = "byline")
    val byLine: ByLine?,
    @Json(name = "_id")
    val Id: String? = "",
    @Json(name = "label")
    val label: Kicker?,
    @Json(name = "analytics")
    @TypeConverters(OmnitureXTypeConverter::class)
    val analytics: OmnitureX?,
)
