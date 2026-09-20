package com.wapo.flagship.features.gifting.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.gifting.services.GiftArticleService

/**
 * This is a request body for sending gift article sender flow api calls
 * Fore more info please check [GiftArticleService]
 * [articleUrl] -  Url of the article that user wishes to gift.
 */
@JsonClass(generateAdapter = true)
data class GiftArticleSenderRequestBody(
    @Json(name = "articleUrl")
    val articleUrl: String?,
)
