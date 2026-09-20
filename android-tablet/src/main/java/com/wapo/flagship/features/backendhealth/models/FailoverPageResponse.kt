package com.wapo.flagship.features.backendhealth.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class FailoverPageResponse(
    @Json(name = "articles") val articles: List<Article>? = null,
)

@JsonClass(generateAdapter = true)
data class Article(
    @Json(name = "contenttype") val contenttype: String? = null,
    @Json(name = "contenturl") val contenturl: String? = null,
    @Json(name = "smallthumburl") val smallthumburl: String? = null,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "displaydatetime") val displaydatetime: Double? = null,
    @Json(name = "byline") val byline: String? = null,
    @Json(name = "blurb") val blurb: String? = null,
)