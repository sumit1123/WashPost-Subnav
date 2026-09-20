package com.wapo.flagship.features.gifting.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.gifting.services.GiftArticleService

/**
 * This is a model class for the response body for an api call to know the remaining count of gift articles.
 * POST /subscriptionapi/v2/subscriptions/current/gift-articles/remaining-count
 * Check [GiftArticleService.getGiftArticleRemainingCount] for more details
 * [remainingCount] - Max. no. of articles that user can gift this month
 * [status] - could be SUCCESS or FAILURE
 * [state] - In case of FAILURE, the reason for failure e.g. 2256 for no remaining articles.
 * [hasSharedArticle] - If the same article has been gifted previously.
 */
@JsonClass(generateAdapter = true)
data class GiftArticleRemainingCountResponseBody(
    @Json(name = "remainingCount")
    val remainingCount: Int?,
    @Json(name = "status")
    val status: String?,
    @Json(name = "state")
    val state: Int?,
    @Json(name = "hasSharedArticle")
    val hasSharedArticle: Boolean = false,
)
