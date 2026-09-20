package com.wapo.flagship.features.articles2.models.deserialized

import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.ElementGroupItem
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.typeconverters.ItemListTypeConverter

sealed class ElementGroup(
    override val type: String?,
    open val subtype: String?,
    open val contentElements: List<ElementGroupItem>?,
) : Item(type = type) {
    companion object {
        const val TYPE = "element_group"
        const val LINK_BOX_SUBTYPE = "link_box"
        const val BLOCK_QUOTE_SUBTYPE = "block_quote"
    }
}

@JsonClass(generateAdapter = true)
data class ElementGroupLinkBox(
    @Json(name = "type")
    override val type: String? = TYPE,
    @Json(name = "subtype")
    override val subtype: String? = LINK_BOX_SUBTYPE,
    @Json(name = "content_elements")
    @TypeConverters(ItemListTypeConverter::class)
    override val contentElements: List<ElementGroupItem>?,
    @Json(name = "additional_properties")
    val additionalProperties: AdditionalProperties?,
) : ElementGroup(
    type = type,
    subtype = subtype,
    contentElements = contentElements,
)

@JsonClass(generateAdapter = true)
data class ElementGroupBlockQuote(
    @Json(name = "type")
    override val type: String? = TYPE,
    @Json(name = "subtype")
    override val subtype: String? = BLOCK_QUOTE_SUBTYPE,
    @Json(name = "content_elements")
    @TypeConverters(ItemListTypeConverter::class)
    override val contentElements: List<ElementGroupItem>?,
    @Json(name = "showVersion")
    override val showVersion: Int?,
    @Json(name = "hideVersion")
    override val hideVersion: Int?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "attribution")
    val attribution: String?,
) : ElementGroup(
    type = type,
    subtype = subtype,
    contentElements = contentElements,
)