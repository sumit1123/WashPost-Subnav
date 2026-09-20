/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.preferencesapi.services

import com.wapo.flagship.features.preferencesapi.models.ContentPacksGetResponse
import com.wapo.flagship.features.preferencesapi.models.ContentPacksSetRequest
import com.wapo.flagship.features.preferencesapi.models.ContentPacksSetResponse
import com.wapo.flagship.features.preferencesapi.models.ContentPacksUiResponseBody
import com.wapo.flagship.features.preferencesapi.models.NewsprintAttributesResponse
import com.wapo.flagship.features.preferencesapi.models.NewsprintStateResponse
import com.wapo.flagship.features.preferencesapi.models.SetPersoPodConfigResponse
import com.wapo.flagship.features.preferencesapi.models.TopicNotificationsGetResponse
import com.wapo.flagship.features.preferencesapi.models.TopicNotificationsSetRequest
import com.wapo.flagship.features.preferencesapi.models.TopicNotificationsSetResponse
import com.wapo.flagship.features.preferencesapi.models.WallDismissalSetRequest
import com.wapo.flagship.features.preferencesapi.models.WallDismissalGetResponse
import com.wapo.flagship.features.preferencesapi.models.WallDismissalSetResponse
import com.wapo.flagship.features.preferencesapi.services.PreferencesApiQueryValues.CONTENT_PACKS
import com.wapo.flagship.features.preferencesapi.services.PreferencesApiQueryValues.NEWSPRINT_ATTRIBUTES
import com.wapo.flagship.features.preferencesapi.services.PreferencesApiQueryValues.NEWSPRINT_STATE
import com.wapo.flagship.features.preferencesapi.services.PreferencesApiQueryValues.WALL_DISMISSAL
import com.wapo.flagship.network.retrofit.network.APIResult
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Query

object PreferencesApiQueryValues {

    const val CONTENT_PACKS = "content-packs"
    const val NEWSPRINT_ATTRIBUTES = "newsprint-attributes"
    const val NEWSPRINT_STATE = "newsprint-state"
    const val WALL_DISMISSAL = "wall-dismissal"
}

/**
 * Retrofit service to for accessing remote content pack
 * general and specific user data.
 */
interface PreferencesApiService {

    /**
     * Get user subscribed content packs
     * [headers] - List of required headers (ie. clientId, accessToken, etc.)
     * [preferences] - Query Param that identifies the specific user perference requested
     */
    @GET("preferenceapi/v1/current/preferences/get")
    suspend fun getContentPacks(
        @HeaderMap headers: HashMap<String, String>,
        @Query("preferences") preferences: String = CONTENT_PACKS
    ): APIResult<ContentPacksGetResponse>

    /**
     * Set user subscribed packs
     * [headers] - List of required headers (ie. clientId, accessToken, etc.)
     * [contentPacksRequest] - List of subscribed content packs.
     */
    @POST("preferenceapi/v1/current/preferences/content-packs/set")
    suspend fun setContentPacks(
        @HeaderMap headers: HashMap<String, String>,
        @Body contentPacksRequest: ContentPacksSetRequest
    ): APIResult<ContentPacksSetResponse>

    /**
     * Get content pack UI data.
     */
    @GET("config/my-post/content-pack.json")
    suspend fun getContentPackItems(@HeaderMap headers: HashMap<String, String>): APIResult<ContentPacksUiResponseBody>

    /**
     * Get user's enrolled notification topics
     * [headers] - List of required headers (ie. clientId, accessToken, etc.)
     * [preferences] - Query Param that identifies the specific user perference requested
     */
    @GET("preferenceapi/v1/current/preferences/get")
    suspend fun getTopicNotifications(
        @HeaderMap headers: HashMap<String, String>,
        @Query("preferences") preferences: String = "topic-notifications"
    ): TopicNotificationsGetResponse

    /**
     * Set user's enrolled notification topics
     * [headers] - List of required headers (ie. clientId, accessToken, etc.)
     * [topicNotificationsRequest] - Request body that contains list of enrolled notification topics
     */
    @POST("preferenceapi/v1/current/preferences/topic-notifications/set")
    suspend fun setTopicNotifications(
        @HeaderMap headers: HashMap<String, String>,
        @Body topicNotificationsRequest: TopicNotificationsSetRequest
    ): TopicNotificationsSetResponse

    /**
     * Set notification topics for anonymous users
     * [headers] - List of required headers (ie. clientId, accessToken, etc.)
     * [body] - Request body created from [TopicNotificationsAnonRequest] that contains list of
     * enrolled notification topics
     * body is of type [RequestBody] since we must guarantee the body and the payload to generate
     * the x-access-token header have exactly the same serialization
     */
    @POST("preferenceapi/v1/current/preferences/anon/topic-notifications/set")
    suspend fun setTopicNotificationsForAnonymousUser(
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): TopicNotificationsSetResponse

    @POST("preferenceapi/v1/current/preferences/personalized-podcast/set")
    suspend fun setUserPersoPodConfig(
        @HeaderMap headers: HashMap<String, String>,
        @Body body: RequestBody
    ): APIResult<SetPersoPodConfigResponse>


    /**
     * Get user newsprint state
     * [headers] - List of required headers (ie. clientId, accessToken, etc.)
     * [preferences] - Query Param that identifies the specific user perference requested
     */
    @GET("preferenceapi/v1/current/preferences/get")
    suspend fun getNewsprintState(
        @HeaderMap headers: HashMap<String, String>,
        @Query("preferences") preferences: String = NEWSPRINT_STATE
    ): APIResult<NewsprintStateResponse>

    /**
     * Get user newsprint attributes
     * [headers] - List of required headers (ie. clientId, accessToken, etc.)
     * [preferences] - Query Param that identifies the specific user perference requested
     */
    @GET("preferenceapi/v1/current/preferences/get")
    suspend fun getNewsprintAttributes(
        @HeaderMap headers: HashMap<String, String>,
        @Query("preferences") preferences: String = NEWSPRINT_ATTRIBUTES
    ): APIResult<NewsprintAttributesResponse>

    @GET("preferenceapi/v1/current/preferences/get")
    suspend fun getWallDismissal(
        @HeaderMap headers: HashMap<String, String>,
        @Query("preferences") preferences: String = WALL_DISMISSAL
    ): APIResult<WallDismissalGetResponse>

    @POST("preferenceapi/v1/current/preferences/wall-dismissal/set")
    suspend fun setWallDismissal(
        @HeaderMap headers: HashMap<String, String>,
        @Body wallDismissalSetRequest: WallDismissalSetRequest
    ): APIResult<WallDismissalSetResponse>
}
