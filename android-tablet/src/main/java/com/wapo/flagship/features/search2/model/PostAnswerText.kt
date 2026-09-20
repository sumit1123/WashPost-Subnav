package com.wapo.flagship.features.search2.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.search2.ui.MimeType

@JsonClass(generateAdapter = true)
data class PostAnswerText(
    @Json(name = "type")
    override val type: String?,
    @Json(name = "subtype")
    override val subtype: String?,
    @Json(name = "content")
    val content: String?,
    @Json(name = "mime")
    val mime: String? = MimeType.HTML.value,
    @Json(name = "streaming_url")
    val streamingUrl: String? = null,
    @Json(name = "icon")
    val icon: String? = null,
) : PostAnswerItem(
        type = type,
        subtype = subtype,
    )
