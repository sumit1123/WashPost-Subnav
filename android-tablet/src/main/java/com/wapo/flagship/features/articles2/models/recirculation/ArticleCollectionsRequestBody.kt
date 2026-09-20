package com.wapo.flagship.features.articles2.models.recirculation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticleCollectionsRequestBody(
    @Json(name = "current_url") val currentUrl: String,
    @Json(name = "collection_limit") val collectionLimit: Int,
    @Json(name = "article_limit") val articleLimit: Int,
    @Json(name = "article_id") val articleId: String,
    @Json(name = "section") val section: String,
    @Json(name = "authors") val authors: List<String>?,
    @Json(name = "collections") val collections: List<String>?,
    @Json(name = "exclude_category") val excludeCategory: List<String>?,
    @Json(name = "interface") val `interface`: String,
    @Json(name = "surface") val surface: String,
    @Json(name = "wapo_login_id") val wapoLoginId: String?,
    @Json(name = "j_ucid") val jUcid: String,
    @Json(name = "readlist") val readlist: List<ConsumedArticles>,
)

@JsonClass(generateAdapter = true)
data class ConsumedArticles(
    @Json(name = "u") val articleUrl: String,
    @Json(name = "t") val timestamp: Long
)
