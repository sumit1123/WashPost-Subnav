/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.models.deserialized


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Image(
    @Json(name = "blurb")
    val blurb: String?,
    @Json(name = "caption_display")
    val captionDisplay: String?,
    @Json(name = "credits_caption_display")
    val creditsCaptionDisplay: String?,
    @Json(name = "credits_display")
    val creditsDisplay: String?,
    @Json(name = "fullcaption")
    val fullcaption: String?,
    @Json(name = "imageHeight")
    val imageHeight: Int?,
    @Json(name = "image_type")
    val imageType: String?,
    @Json(name = "imageURL")
    val imageURL: String?,
    @Json(name = "imageWidth")
    val imageWidth: Int?,
    @Json(name = "mime")
    val mime: String?,
    @Json(name = "title")
    val title: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "widthFactor")
    val widthFactor: String?
) : Item(type = type)