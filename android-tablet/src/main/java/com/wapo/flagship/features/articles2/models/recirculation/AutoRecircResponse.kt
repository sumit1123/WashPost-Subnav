package com.wapo.flagship.features.articles2.models.recirculation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class AutoRecircResponse(
    @Json(name = "request_id") val requestId: String?,
    @Json(name = "collections") val collections: List<AutoRecircCollection>?,
)

@JsonClass(generateAdapter = true)
data class AutoRecircCollection(
    @Json(name = "id") val collectionId: Int?,
    @Json(name = "title") val title: String?,
    @Json(name = "collection_category") val category: String?,
    @Json(name = "articles") val articles: List<AutoRecircArticle>?,
)

@JsonClass(generateAdapter = true)
data class AutoRecircArticle(
    @Json(name = "_id") val arcId: String?,
    @Json(name = "canonical_url") val canonicalUrl: String?,
    @Json(name = "headlines") val headlines: Headlines?,
    @Json(name = "credits") val credits: Credits?,
    @Json(name = "display_date") val displayDate: Date?,
    @Json(name = "promo_items") val promoItems: PromoItems?,
    @Json(name = "label") val label: Label?,
    @Json(name = "type") val type: String?,
)

@JsonClass(generateAdapter = true)
data class Headlines(
    @Json(name = "basic") val basic: String?
)

@JsonClass(generateAdapter = true)
data class Credits(
    @Json(name = "by") val by: List<ByItem>?
)

@JsonClass(generateAdapter = true)
data class ByItem(
    @Json(name = "_id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "additional_properties") val additionalProperties: AdditionalProperties?
)

@JsonClass(generateAdapter = true)
data class PromoItems(
    @Json(name = "basic") val basic: PromoItemsBasic?
)

@JsonClass(generateAdapter = true)
data class PromoItemsBasic(
    @Json(name = "url") val url: String?,
    @Json(name = "additional_properties") val additionalProperties: AdditionalProperties?,
)

@JsonClass(generateAdapter = true)
data class AdditionalProperties(
    @Json(name = "originalUrl") val originalUrl: String?,
    @Json(name = "size_normalized_url") val sizeNormalizedUrl: String?,
)

@JsonClass(generateAdapter = true)
data class Label(
    @Json(name = "transparency") val transparency: LabelProps?,
    @Json(name = "basic") val basic: LabelProps?,
    @Json(name = "story_length") val storyLength: LabelProps?,
    @Json(name = "user_need") val userNeed: LabelProps?,
)

@JsonClass(generateAdapter = true)
data class LabelProps(
    @Json(name = "text") val text: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "display") val display: Boolean?,
    @Json(name = "additional_properties") val additionalProperties: AdditionalProperties?,
)

@JsonClass(generateAdapter = true)
data class Description(
    @Json(name = "basic") val basic: String?,
)