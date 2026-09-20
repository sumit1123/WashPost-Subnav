package com.wapo.flagship.features.articles2.models.deserialized.gallery

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.models.deserialized.Omniture

@JsonClass(generateAdapter = true)
data class Gallery(
    @Json(name = "arcId")
    override val arcId: String?,
    @Json(name = "blurb")
    val blurb: String?,
    @Json(name = "commercialnode")
    val commercialnode: String?,
    @Json(name = "contenturl")
    val contenturl: String?,
    @Json(name = "dataServiceAdaptor")
    val dataServiceAdaptor: String?,
    @Json(name = "first_published")
    val firstPublished: Long?,
    @Json(name = "id")
    val id: String?,
    @Json(name = "images")
    val images: List<Image>?,
    @Json(name = "lmt")
    val lmt: Long?,
    @Json(name = "omniture")
    val omniture: Omniture?,
    @Json(name = "published")
    val published: Long?,
    @Json(name = "shareurl")
    val shareurl: String?,
    @Json(name = "source")
    val source: String?,
    @Json(name = "title")
    val title: String?,
    @Json(name = "type")
    override val type: String?,
    var isExpanded: Boolean = false,
) : Item(type = type)
