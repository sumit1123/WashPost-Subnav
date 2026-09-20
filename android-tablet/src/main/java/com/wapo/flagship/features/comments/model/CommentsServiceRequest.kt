package com.wapo.flagship.features.comments.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CommentsServiceRequest(
    @Json(name = "article_url") val articleUrl: String?,
    @Json(name = "comments") val comments: Boolean,
    @Json(name = "source_annotations") val sourceAnnotations: Boolean
)
