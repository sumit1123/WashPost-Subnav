package com.wapo.flagship.features.personalizedpodcasts.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastConfigResponse(
    @Json(name = "description")
    val description: String?,
    @Json(name = "podcastConfigItems")
    val podcastConfigItems: List<PodcastConfigItem>,
    @Json(name = "message")
    var message: String?,
    @Json(name = "canGenerate")
    var canGenerate: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PodcastConfigItem(
    @Json(name = "title")
    val title: Option,
    @Json(name = "items")
    val items: List<Option>,
    @Json(name = "selectionLimit")
    val selectionLimit: Int?,
    @Json(name = "allowsCustomInput")
    var allowsCustomInput: Boolean?,
    @Json(name = "style")
    var style: String?
)

@JsonClass(generateAdapter = true)
data class Option(
    @Json(name = "id")
    val id: String,
    @Json(name = "displayName")
    var displayName: String,
    @Json(name = "isSelected")
    var isSelected: Boolean? = false,
    @Json(name = "url")
    var url: String?
)