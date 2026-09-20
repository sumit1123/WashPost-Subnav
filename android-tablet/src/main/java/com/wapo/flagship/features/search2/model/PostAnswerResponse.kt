// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.search2.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostAnswerResponse(
    @Json(name = "items")
    val items: List<PostAnswerItem>?,
    @Json(name = "citations")
    val citations: List<Citation>? = emptyList()
)

@JsonClass(generateAdapter = true)
open class PostAnswerItem(
    @Json(name = "type")
    open val type: String?,
    @Json(name = "subtype")
    open val subtype: String? = null,
)

@JsonClass(generateAdapter = true)
data class Citation(
    @Json(name = "id")
    val id: String?,
    @Json(name = "source")
    val source: Source?,
)

@JsonClass(generateAdapter = true)
data class Source(
    @Json(name = "url")
    val url: String? = null,
    @Json(name = "publish_date")
    val publishDate: Long? = null,
    @Json(name = "content")
    val content: String? = null,
    @Json(name = "passages")
    val passages: String? = null,
    @Json(name = "source_id")
    val citationId: String? = null,
)

