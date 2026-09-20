package com.wapo.flagship.features.purchasedarticles.model

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class PurchasedArticleResponse(
    @Json(name = "status")
    val status: String,
    @Json(name ="state")
    val state: String,
    @Json(name ="purchasedArticles")
    val purchasedArticles: List<PurchasedArticleUrl>?
)

@JsonClass(generateAdapter = true)
data class PurchasedArticleUrl(
    @Json(name ="canonicalUrl")
    val canonicalUrl: String?,
    @Json(name ="purchaseDate")
    val purchaseDate: Long?,
)
