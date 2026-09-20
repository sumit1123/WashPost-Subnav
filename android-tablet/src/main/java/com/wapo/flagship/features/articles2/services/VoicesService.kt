package com.wapo.flagship.features.articles2.services

import com.wapo.flagship.features.articles2.models.Voices
import com.wapo.flagship.features.articles2.models.deserialized.Audio
import com.wapo.flagship.network.retrofit.network.APIResult
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * This service is used to download list of available voices for audio articles.
 * The URL to download these voices is provided in [Audio] feeds item.
 */
interface VoicesService {
    @GET("{path}")
    suspend fun getAvailableVoices(
        @Path("path", encoded = true) path: String,
    ): APIResult<Voices>
}
