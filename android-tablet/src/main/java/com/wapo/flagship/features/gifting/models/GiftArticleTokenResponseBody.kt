package com.wapo.flagship.features.gifting.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.gifting.services.GiftArticleService

/**
 * This is a model class for the response body for an api call to get the url for gift article (along with the token)
 * POST /subscriptionapi/v2/subscriptions/current/gift-articles
 * Check [GiftArticleService.getGiftArticleTokenWithUrl] for more details
 * [remainingCount] - Max. no. of articles that user can gift this month
 * [status] - could be SUCCESS or FAILURE
 * [state] - In case of FAILURE, the reason for failure e.g. 2256 for no remaining articles.
 * [hasSharedArticle] - If the same article has been gifted previously.
 * [token] - Token in the gift article url
 * [url] - url that can be used to gift an article (mostly bitly shortened url but not always)
 */
@JsonClass(generateAdapter = true)
data class GiftArticleTokenResponseBody(
    @Json(name = "hasSharedArticle")
    val hasSharedArticle: Boolean = false,
    @Json(name = "remainingCount")
    val remainingCount: Int?,
    @Json(name = "state")
    val state: Int?,
    @Json(name = "status")
    val status: String?,
    @Json(name = "token")
    val token: String?,
    @Json(name = "url")
    val url: String?,
)
