package com.wapo.flagship.features.articles2.models.deserialized

import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.ElementGroupItem
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.typeconverters.AlignmentTypeConverter
import com.wapo.flagship.features.articles2.typeconverters.ElementListTypeConverter

/**
 * ContextBox model - extended Item Model used in Article2
 */
@JsonClass(generateAdapter = true)
data class ContextBox(
    @Json(name = "content_pages")
    val contentPages: List<ContentPagesItem>? = null,
    @Json(name = "type")
    override val type: String? = null,
    @Json(name = "headline")
    val headline: String? = null,
) : Item(type = type)

/**
 * Context Box Pages model
 */
@JsonClass(generateAdapter = true)
data class ContentPagesItem(
    /**
     * Context items on page. Supported types in [ElementListTypeConverter]
     */
    @Json(name = "content_elements")
    @TypeConverters(ElementListTypeConverter::class)
    val contentElements: List<ElementGroupItem?>? = null,
    @Json(name = "subtype")
    val subtype: String? = null,
    @Json(name = "type")
    val type: String? = null,
    @Json(name = "alignment")
    @TypeConverters(AlignmentTypeConverter::class)
    val alignment: ContextBoxAlignment? = null,
)

enum class ContextBoxAlignment(
    val value: String?,
) {
    @Json(name = "left")
    LEFT("left"),

    @Json(name = "right")
    RIGHT("right"),

    @Json(name = "unknown")
    UNKNOWN("unknown"),
}
