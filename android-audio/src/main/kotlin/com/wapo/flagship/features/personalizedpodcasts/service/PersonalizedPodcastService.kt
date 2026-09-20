package com.wapo.flagship.features.personalizedpodcasts.service

import com.wapo.flagship.features.personalizedpodcasts.model.PodcastConfigResponse
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcasts
import com.wapo.flagship.features.personalizedpodcasts.model.TranscriptResponse
import com.washingtonpost.userhistory.network.APIResult
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface PersonalizedPodcastService {

    @POST("personalized-podcast/api/v2/podcast/generate-podcast")
    suspend fun generatePodcast(
        @HeaderMap headerMap: HashMap<String, String>,
        @Body requestBody: RequestBody?,
//        @Header(MockResponseInterceptor.MOCK_RESPONSE) mock: String = MockResponseInterceptor.GENERATE_PODCAST
    ): APIResult<PersonalizedPodcast>

    @Headers("Content-Type: application/json")
    @GET("personalized-podcast/api/v2/podcast/selection-config")
    suspend fun getPodcastConfigs(
        @HeaderMap headerMap: HashMap<String, String>
    ): APIResult<PodcastConfigResponse>


    @GET("personalized-podcast/api/v2/podcast/user-podcasts")
    suspend fun getUserGeneratedPodcasts(
        @HeaderMap headerMap: HashMap<String, String>,
//        @Header(MockResponseInterceptor.MOCK_RESPONSE) mock: String = MockResponseInterceptor.GENERATED_PODCASTS
    ): APIResult<PersonalizedPodcasts>

    @GET
    suspend fun getTranscript(
        @Url fullUrl: String
    ): APIResult<TranscriptResponse>

    @GET("personalized-podcast/api/v2/podcast/fetch-podcast")
    suspend fun getPodcastMetadata(
        @HeaderMap headerMap: HashMap<String, String>,
        @Query("episode_id") podcastId: String,
//        @Header(MockResponseInterceptor.MOCK_RESPONSE) mock: String = MockResponseInterceptor.SINGLE_PODCAST
    ): APIResult<PersonalizedPodcast>

}