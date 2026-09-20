package com.wapo.flagship.features.gifting.services

import com.wapo.flagship.features.gifting.models.GiftArticleRemainingCountResponseBody
import com.wapo.flagship.features.gifting.models.GiftArticleSenderRequestBody
import com.wapo.flagship.features.gifting.models.GiftArticleTokenResponseBody
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

/**
 * Service with all the required api calls for gift article sender flow
 */
interface GiftArticleService {
    @POST("subscriptionapi/v2/subscriptions/current/gift-articles/remaining-count")
    suspend fun getGiftArticleRemainingCount(
        @HeaderMap headers: HashMap<String, String>,
        @Body giftArticleSenderRequestBody: GiftArticleSenderRequestBody,
    ): APIResult<GiftArticleRemainingCountResponseBody>

    @POST("subscriptionapi/v2/subscriptions/current/gift-articles")
    suspend fun getGiftArticleTokenWithUrl(
        @HeaderMap headers: HashMap<String, String>,
        @Body giftArticleSenderRequestBody: GiftArticleSenderRequestBody,
    ): APIResult<GiftArticleTokenResponseBody>
}
